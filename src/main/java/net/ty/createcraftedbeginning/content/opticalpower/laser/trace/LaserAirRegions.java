package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserStaticDependencies.Sample;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceGeometry.PreparedBlock;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserAirRegions {
    private static final int MAX_REGIONS = 4096;
    private static final int MAX_SAMPLES = 65536;
    private final WeakReference<Level> world;
    private final LaserWorldSections sections;
    private final List<BeamVolumeCells> regions = new ArrayList<>();
    private boolean complete = true;
    private int observations;

    LaserAirRegions(Level level) {
        world = new WeakReference<>(level);
        sections = new LaserWorldSections(level);
    }

    void include(Level level, BlockPos minimum, BlockPos maximum, BeamVolumeCells cells) {
        if (!complete) {
            return;
        }

        if (world.get() != level || observations >= MAX_REGIONS) {
            complete = false;
            return;
        }

        sections.include(level, minimum, maximum, cells);
        observations++;
        if (!regions.isEmpty() && regions.getLast() == cells) {
            return;
        }

        regions.add(cells);
    }

    boolean matches(Level level) {
        return complete && world.get() == level && sections.matches(level);
    }

    boolean expand(List<AABB> paths, Long2ObjectMap<Sample> samples) {
        Level level = world.get();
        if (level == null || !matches(level)) {
            return false;
        }

        LaserCollisionContext collision = new LaserCollisionContext(1);
        ClipContext clip = new ClipContext(Vec3.ZERO, Vec3.ZERO, Block.COLLIDER, Fluid.NONE, collision);
        for (BeamVolumeCells region : regions) {
            AABB bounds = Objects.requireNonNull(region.bounds(), "Indexed region bounds must not be null.");
            for (AABB path : paths) {
                int minX = (int) Math.max(Math.floor(bounds.minX), Math.ceil(path.minX));
                int minY = (int) Math.max(Math.floor(bounds.minY), Math.ceil(path.minY));
                int minZ = (int) Math.max(Math.floor(bounds.minZ), Math.ceil(path.minZ));
                int maxX = (int) Math.min(Math.ceil(bounds.maxX) - 1, Math.floor(path.maxX));
                int maxY = (int) Math.min(Math.ceil(bounds.maxY) - 1, Math.floor(path.maxY));
                int maxZ = (int) Math.min(Math.ceil(bounds.maxZ) - 1, Math.floor(path.maxZ));
                if (minX > maxX || minY > maxY || minZ > maxZ) {
                    continue;
                }

                BeamVolumeCells selected = new BeamVolumeCells(region.volumes(), new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1));
                for (BlockPos pos : selected) {
                    long key = pos.asLong();
                    if (samples.containsKey(key)) {
                        continue;
                    }

                    if (samples.size() >= MAX_SAMPLES) {
                        return false;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock().getClass() == AirBlock.class) {
                        samples.put(key, new Sample(pos, state, List.of(), false, null));
                        continue;
                    }

                    PreparedBlock block = LaserTraceGeometry.prepareWorldBlock(level, pos, clip, 1);
                    if (block == null || !LaserWorldSections.stable(block.state()) || block.optical() != null) {
                        return false;
                    }

                    samples.put(key, new Sample(pos, block.state(), block.boxes(), block.precise(), null));
                }
            }
        }
        return matches(level);
    }
}
