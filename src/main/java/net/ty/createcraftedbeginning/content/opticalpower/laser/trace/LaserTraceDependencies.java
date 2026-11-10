package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserQuerySpace.Pose;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceGeometry.PreparedBlock;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RenderGeometry;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record LaserTraceDependencies(List<BlockSample> blocks, List<LoadSample> loading, List<AABB> bounds, List<AABB> paths, Map<UUID, RenderGeometry> subLevels, Map<UUID, Pose> contraptions) {
    static boolean isStaticWorld(Level level, List<AABB> bounds) {
        return isStaticWorld(level, bounds, 1);
    }

    static boolean isStaticWorld(Level level, List<AABB> bounds, float partialTick) {
        if (!LaserContraptionBounds.isClear(level, bounds, partialTick)) {
            return false;
        }

        for (AABB query : bounds) {
            if (SubLevelBridge.localRegions(level, query).size() != 1) {
                return false;
            }
        }
        return true;
    }

    static @Unmodifiable Map<UUID, Pose> poses(List<LaserQuerySpace> spaces, List<AABB> bounds) {
        Map<UUID, Pose> result = new LinkedHashMap<>();
        for (LaserQuerySpace space : spaces) {
            AABB posed = space.worldBounds();
            for (AABB query : bounds) {
                if (posed.intersects(query)) {
                    result.put(space.id(), space.pose());
                    break;
                }
            }
        }
        return Map.copyOf(result);
    }

    boolean matches(Level level, float partialTick) {
        if (!subLevels.equals(SubLevelBridge.captureRenderGeometry(level, bounds, paths, partialTick))) {
            return false;
        }

        List<LaserQuerySpace> spaces = List.of();
        if (!contraptions.isEmpty() || !LaserContraptionBounds.isClear(level, bounds, partialTick)) {
            spaces = LaserQuerySpace.mounted(level, partialTick);
            if (!contraptions.equals(poses(spaces, bounds))) {
                return false;
            }
        }

        return matchesBlocks(level, partialTick, spaces);
    }

    boolean matchesStaticWorld(Level level) {
        return isStaticWorld(level, bounds) && matchesBlocks(level, 1, List.of());
    }

    private boolean matchesBlocks(Level level, float partialTick, List<LaserQuerySpace> spaces) {
        for (LoadSample sample : loading) {
            if (level.isLoaded(sample.pos()) != sample.loaded()) {
                return false;
            }
        }
        LaserCollisionContext collision = new LaserCollisionContext(partialTick);
        ClipContext clip = new ClipContext(Vec3.ZERO, Vec3.ZERO, Block.COLLIDER, Fluid.NONE, collision);
        Map<UUID, LaserQuerySpace> owners = new LinkedHashMap<>();
        for (LaserQuerySpace space : spaces) {
            owners.put(space.id(), space);
        }
        for (BlockSample sample : blocks) {
            BlockState expected = sample.state();
            PreparedBlock current;
            UUID owner = sample.owner();
            if (owner == null) {
                current = LaserTraceGeometry.prepareWorldBlock(level, sample.pos(), clip, partialTick);
            }
            else {
                LaserQuerySpace space = owners.get(owner);
                if (space == null) {
                    return false;
                }

                Map<BlockPos, StructureBlockInfo> mountedBlocks = space.blocks();
                if (mountedBlocks == null) {
                    return false;
                }

                current = collision.getGeometry().getMountedBlock(space.owner(), mountedBlocks, sample.pos(), collision);
            }
            if (expected == null || current == null) {
                if (expected != null || current != null) {
                    return false;
                }

                continue;
            }

            boolean precise = current.precise();
            if (expected != current.state() || sample.precise() != precise) {
                return false;
            }

            if (!Objects.equals(sample.optical(), current.optical())) {
                return false;
            }

            List<AABB> boxes = current.boxes();
            if (precise) {
                boxes = current.shape().toAabbs();
            }
            if (!sample.boxes().equals(boxes)) {
                return false;
            }
        }
        return true;
    }

    record BlockSample(BlockPos pos, @Nullable UUID owner, @Nullable BlockState state, List<AABB> boxes, boolean precise, @Nullable OpticalGeometry optical) {}

    record LoadSample(BlockPos pos, boolean loaded) {}
}
