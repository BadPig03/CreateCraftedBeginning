package net.ty.createcraftedbeginning.content.photostresses.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface PhotoStressSource {
    Source getPhotoStressSource(Level level, BlockPos pos, BlockState state);

    default int getCurrentPhotoStressPoints(Level level, BlockPos pos, BlockState state, Source discoveredSource) {
        return getPhotoStressSource(level, pos, state).stressPoints();
    }

    record Source(BlockPos key, int stressPoints, List<BlockPos> dependencies, List<BlockPos> powerDependencies, boolean dynamic, boolean topologyValid) {
        public Source(BlockPos key, int stressPoints) {
            this(key, stressPoints, List.of(key), List.of(key), false, true);
        }

        public Source(BlockPos key, int stressPoints, List<BlockPos> dependencies, boolean dynamic) {
            this(key, stressPoints, dependencies, dependencies, dynamic, true);
        }

        public Source(BlockPos key, int stressPoints, List<BlockPos> dependencies, boolean dynamic, boolean topologyValid) {
            this(key, stressPoints, dependencies, dependencies, dynamic, topologyValid);
        }

        public Source {
            key = key.immutable();
            stressPoints = Math.max(0, stressPoints);
            dependencies = immutablePositions(dependencies);
            powerDependencies = immutablePositions(powerDependencies);
        }

        private static List<BlockPos> immutablePositions(List<BlockPos> positions) {
            List<BlockPos> immutable = new ArrayList<>(positions.size());
            for (BlockPos position : positions) {
                immutable.add(position.immutable());
            }
            return List.copyOf(immutable);
        }
    }
}
