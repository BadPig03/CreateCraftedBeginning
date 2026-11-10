package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserContraptionBounds.MountedGeometry;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.LocalRegion;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record LaserQuerySpace(BlockGetter owner, @Nullable Map<BlockPos, StructureBlockInfo> blocks, @Nullable UUID id, AABB bounds, Vec3 pivot, Vec3 worldPivot, Vec3 x, Vec3 y, Vec3 z, Vec3 inverseX, Vec3 inverseY, Vec3 inverseZ, @Nullable AABB preparedBounds) implements CoordinateTransform {
    static LaserQuerySpace world(Level level, LocalRegion region) {
        Vec3 x = region.axisX();
        Vec3 y = region.axisY();
        Vec3 z = region.axisZ();
        return new LaserQuerySpace(level, null, null, region.bounds(), region.pivot(), region.worldPivot(), x, y, z, x.scale(1 / x.lengthSqr()), y.scale(1 / y.lengthSqr()), z.scale(1 / z.lengthSqr()), null);
    }

    static @Unmodifiable List<LaserQuerySpace> mounted(Level level, float partialTick) {
        List<LaserQuerySpace> result = new ArrayList<>();
        for (WeakReference<AbstractContraptionEntity> reference : ContraptionHandler.loadedContraptions.get(level).values()) {
            AbstractContraptionEntity entity = reference.get();
            if (entity == null || !entity.isAlive()) {
                continue;
            }

            Contraption contraption = entity.getContraption();
            if (contraption == null || contraption.bounds == null) {
                continue;
            }

            MountedGeometry geometry = LaserContraptionBounds.mounted(level, reference, entity, contraption.bounds, partialTick);
            Pose pose = geometry.pose();
            result.add(new LaserQuerySpace(contraption.getContraptionWorld(), contraption.getBlocks(), entity.getUUID(), pose.bounds(), pose.pivot(), pose.worldPivot(), pose.x(), pose.y(), pose.z(), pose.inverseX(), pose.inverseY(), pose.inverseZ(), geometry.world()));
        }
        return List.copyOf(result);
    }

    @Override
    public Vec3 transformPosition(Position position) {
        Vec3 delta = new Vec3(position.x(), position.y(), position.z()).subtract(pivot);
        return worldPivot.add(x.scale(delta.x)).add(y.scale(delta.y)).add(z.scale(delta.z));
    }

    @Override
    public Vec3 transformNormal(Position normal) {
        return x.scale(normal.x()).add(y.scale(normal.y())).add(z.scale(normal.z())).normalize();
    }

    @Override
    public boolean inSubLevel() {
        return id == null && (!pivot.equals(Vec3.ZERO) || !worldPivot.equals(Vec3.ZERO) || !x.equals(new Vec3(1, 0, 0)) || !y.equals(new Vec3(0, 1, 0)) || !z.equals(new Vec3(0, 0, 1)));
    }

    Vec3 localPosition(Vec3 position) {
        return pivot.add(localVector(position.subtract(worldPivot)));
    }

    Vec3 localPosition(Vec3 position, Vec3 origin) {
        return pivot.subtract(origin).add(localVector(position.subtract(worldPivot)));
    }

    Vec3 localVector(Vec3 vector) {
        return new Vec3(vector.dot(inverseX), vector.dot(inverseY), vector.dot(inverseZ));
    }

    Vec3 localError(Vec3 start, Vec3 end) {
        double ex = Math.max(Math.ulp(worldPivot.x), Math.max(Math.ulp(start.x), Math.ulp(end.x)));
        double ey = Math.max(Math.ulp(worldPivot.y), Math.max(Math.ulp(start.y), Math.ulp(end.y)));
        double ez = Math.max(Math.ulp(worldPivot.z), Math.max(Math.ulp(start.z), Math.ulp(end.z)));
        return new Vec3(Math.abs(inverseX.x) * ex + Math.abs(inverseX.y) * ey + Math.abs(inverseX.z) * ez, Math.abs(inverseY.x) * ex + Math.abs(inverseY.y) * ey + Math.abs(inverseY.z) * ez, Math.abs(inverseZ.x) * ex + Math.abs(inverseZ.y) * ey + Math.abs(inverseZ.z) * ez).scale(LaserOcclusionShape.TRANSFORM_BOUNDARY_ULPS);
    }

    AABB worldBounds() {
        if (preparedBounds != null) {
            return preparedBounds;
        }

        AABB local = bounds.inflate(LaserBlockClip.NEIGHBOR_REACH);
        Vec3 center = transformPosition(local.getCenter());
        Vec3 extentX = x.scale(local.getXsize() / 2);
        Vec3 extentY = y.scale(local.getYsize() / 2);
        Vec3 extentZ = z.scale(local.getZsize() / 2);
        return AABB.ofSize(center, 2 * (Math.abs(extentX.x) + Math.abs(extentY.x) + Math.abs(extentZ.x)), 2 * (Math.abs(extentX.y) + Math.abs(extentY.y) + Math.abs(extentZ.y)), 2 * (Math.abs(extentX.z) + Math.abs(extentY.z) + Math.abs(extentZ.z)));
    }

    Pose pose() {
        return new Pose(bounds, pivot, worldPivot, x, y, z, inverseX, inverseY, inverseZ);
    }

    record Pose(AABB bounds, Vec3 pivot, Vec3 worldPivot, Vec3 x, Vec3 y, Vec3 z, Vec3 inverseX, Vec3 inverseY, Vec3 inverseZ) {}
}
