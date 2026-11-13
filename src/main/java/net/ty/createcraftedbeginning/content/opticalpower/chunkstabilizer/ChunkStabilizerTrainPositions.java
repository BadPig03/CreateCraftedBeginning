package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ChunkStabilizerTrainPositions {
    private ChunkStabilizerTrainPositions() {
    }

    public static @Nullable ChunkStabilizerLocation resolve(MinecraftServer server, UUID identity) {
        for (Train train : Create.RAILWAYS.trains.values()) {
            if (train.invalid) {
                continue;
            }

            for (Carriage carriage : train.carriages) {
                ChunkStabilizerTrainCarriage layout = ChunkStabilizerTrainCarriage.capture(server, train.id, carriage);
                if (layout == null) {
                    continue;
                }

                for (Entry<BlockPos, UUID> entry : layout.identities().entrySet()) {
                    if (!entry.getValue().equals(identity)) {
                        continue;
                    }

                    try {
                        Position position = locate(server, carriage, layout.direction(), entry.getKey(), dimensions(carriage));
                        if (position == null) {
                            return null;
                        }

                        return new ChunkStabilizerLocation(position.level().dimension(), position.worldPosition(), train.id, carriage.id);
                    }
                    catch (RuntimeException failure) {
                        ChunkStabilizerTrainCarriage.suspendWork(server, train.id, carriage, "locate the loader", failure);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    static List<ResourceKey<Level>> dimensions(Carriage carriage) {
        Set<ResourceKey<Level>> traversed = new HashSet<>();
        carriage.bogeys.forEach(bogey -> {
            if (bogey == null) {
                return;
            }

            List.of(bogey.leading(), bogey.trailing()).forEach(point -> {
                if (point.node1 == null) {
                    return;
                }

                traversed.add(point.node1.getLocation().dimension);
            });
        });
        List<ResourceKey<Level>> dimensions = new ArrayList<>(carriage.getPresentDimensions());
        dimensions.removeIf(dimension -> !traversed.isEmpty() && !traversed.contains(dimension));
        dimensions.sort(Comparator.comparing(dimension -> dimension.location().toString()));
        return dimensions;
    }

    static @Nullable Position locate(MinecraftServer server, Carriage carriage, Direction direction, BlockPos localPos, List<ResourceKey<Level>> dimensions) {
        for (ResourceKey<Level> dimension : dimensions) {
            ServerLevel level = server.getLevel(dimension);
            DimensionalCarriageEntity dimensional = carriage.getDimensionalIfPresent(dimension);
            if (level == null || dimensional == null || !contains(dimensional, direction, carriage.bogeySpacing, localPos)) {
                continue;
            }

            Vec3 position = project(dimensional, direction, localPos);
            if (position == null || !Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z) || SubLevelBridge.resolve(level, position).inSubLevel()) {
                continue;
            }

            Vec3 anchor = dimensional.positionAnchor;
            if (anchor == null || !Double.isFinite(anchor.x) || !Double.isFinite(anchor.y) || !Double.isFinite(anchor.z)) {
                continue;
            }

            return new Position(level, dimensional, BlockPos.containing(position), BlockPos.containing(anchor), position);
        }
        return null;
    }

    static boolean contains(DimensionalCarriageEntity dimensional, Direction direction, int bogeySpacing, BlockPos localPos) {
        float cutoff = dimensional.cutoff;
        if (cutoff >= 1 || cutoff <= -1) {
            return false;
        }

        if (cutoff == 0) {
            return true;
        }

        int coordinate = direction.getClockWise().getAxis().choose(localPos.getZ(), localPos.getY(), localPos.getX()) * -direction.getAxisDirection().getStep();
        if (cutoff > 0) {
            return coordinate >= -bogeySpacing - 1 + (2 + bogeySpacing) * cutoff;
        }

        return coordinate < -bogeySpacing - 1 + (2 + bogeySpacing) * (cutoff + 1);
    }

    @SuppressWarnings("ConstantValue")
    static @Nullable Vec3 project(DimensionalCarriageEntity dimensional, Direction direction, BlockPos localPos) {
        CarriageContraptionEntity entity = dimensional.entity.get();
        if (entity != null && entity.isAlive() && !entity.leftTickingChunks) {
            return entity.toGlobalVector(localPos.getCenter(), 1);
        }

        Vec3 anchor = dimensional.positionAnchor;
        if (anchor == null) {
            return null;
        }

        Vec3 leading = dimensional.rotationAnchors.getFirst();
        Vec3 trailing = dimensional.rotationAnchors.getSecond();
        if (leading == null || trailing == null) {
            return null;
        }

        Vec3 difference = leading.subtract(trailing);
        float yaw = (float) (Mth.atan2(difference.z, difference.x) * 180 / Math.PI) + 180;
        float pitch = (float) (Math.atan2(difference.y, Math.sqrt(difference.x * difference.x + difference.z * difference.z)) * 180 / Math.PI) * -1;
        Vec3 rotated = VecHelper.rotate(Vec3.atLowerCornerOf(localPos), direction.getClockWise().toYRot(), Axis.Y);
        rotated = VecHelper.rotate(rotated, pitch, Axis.Z);
        rotated = VecHelper.rotate(rotated, -yaw, Axis.Y);
        return rotated.add(anchor).add(0, 0.5, 0);
    }

    record Position(ServerLevel level, DimensionalCarriageEntity dimensional, BlockPos loader, BlockPos anchor, Vec3 worldPosition) {}
}
