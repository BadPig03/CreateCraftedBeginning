package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceDependencies.BlockSample;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceDependencies.LoadSample;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceGeometry.PreparedBlock;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LaserStaticDependencies(List<Sample> blocks, List<BlockPos> loaded, List<AABB> bounds) {
    public LaserStaticDependencies {
        blocks = List.copyOf(blocks);
        loaded = loaded.stream().map(BlockPos::immutable).toList();
        bounds = List.copyOf(bounds);
    }

    static @Nullable LaserStaticDependencies from(LaserTraceDependencies dependencies) {
        List<AABB> paths = dependencies.paths().stream().map(path -> new AABB(Math.floor(path.minX) - LaserBlockClip.NEIGHBOR_REACH, Math.floor(path.minY) - LaserBlockClip.NEIGHBOR_REACH, Math.floor(path.minZ) - LaserBlockClip.NEIGHBOR_REACH, Math.ceil(path.maxX) + LaserBlockClip.NEIGHBOR_REACH, Math.ceil(path.maxY) + LaserBlockClip.NEIGHBOR_REACH, Math.ceil(path.maxZ) + LaserBlockClip.NEIGHBOR_REACH)).toList();
        List<Sample> samples = new ArrayList<>();
        for (BlockSample sample : dependencies.blocks()) {
            BlockState state = sample.state();
            if (sample.owner() != null || state == null) {
                return null;
            }

            if (!contains(paths, sample.pos())) {
                continue;
            }

            OpticalGeometry optical = sample.optical();
            List<Double> key = null;
            if (optical != null) {
                key = optical.displayKey();
                if (key == null) {
                    return null;
                }
            }
            samples.add(new Sample(sample.pos(), state, sample.boxes(), sample.precise(), key));
        }
        List<LoadSample> loading = dependencies.loading().stream().filter(sample -> contains(paths, sample.pos())).toList();
        if (loading.stream().anyMatch(sample -> !sample.loaded())) {
            return null;
        }

        return new LaserStaticDependencies(samples, loading.stream().map(LoadSample::pos).toList(), paths);
    }

    private static boolean contains(List<AABB> bounds, BlockPos pos) {
        for (AABB bound : bounds) {
            if (pos.getX() >= bound.minX && pos.getX() <= bound.maxX && pos.getY() >= bound.minY && pos.getY() <= bound.maxY && pos.getZ() >= bound.minZ && pos.getZ() <= bound.maxZ) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(Level level, float partialTick) {
        if (!LaserTraceDependencies.isStaticWorld(level, bounds, partialTick)) {
            return false;
        }

        Map<?, ?> subLevels = level.isClientSide ? SubLevelBridge.captureRenderGeometry(level, bounds, bounds, partialTick) : Map.of();
        return subLevels != null && subLevels.isEmpty() && matchesBlocks(level, partialTick);
    }

    boolean matchesBlocks(Level level, float partialTick) {
        for (BlockPos pos : loaded) {
            if (!level.isLoaded(pos)) {
                return false;
            }
        }
        LaserCollisionContext collision = new LaserCollisionContext(partialTick);
        ClipContext clip = new ClipContext(Vec3.ZERO, Vec3.ZERO, Block.COLLIDER, Fluid.NONE, collision);
        for (Sample sample : blocks) {
            if (sample.state().is(Blocks.AIR) && sample.boxes().isEmpty() && !sample.precise() && sample.optical() == null) {
                if (level.getBlockState(sample.pos()) != sample.state()) {
                    return false;
                }

                continue;
            }

            PreparedBlock current = LaserTraceGeometry.prepareWorldBlock(level, sample.pos(), clip, partialTick);
            if (current == null || current.state() != sample.state() || current.precise() != sample.precise()) {
                return false;
            }

            OpticalGeometry optical = current.optical();
            List<Double> key = null;
            if (optical != null) {
                key = optical.displayKey();
                if (key == null) {
                    return false;
                }
            }
            if (!Objects.equals(sample.optical(), key)) {
                return false;
            }

            List<AABB> boxes = current.boxes();
            if (current.precise()) {
                boxes = current.shape().toAabbs();
            }
            if (!sample.boxes().equals(boxes)) {
                return false;
            }
        }
        return true;
    }

    public record Sample(BlockPos pos, BlockState state, List<AABB> boxes, boolean precise, @Nullable List<Double> optical) {
        public Sample {
            pos = pos.immutable();
            boxes = List.copyOf(boxes);
            optical = optical == null ? null : List.copyOf(optical);
        }
    }
}
