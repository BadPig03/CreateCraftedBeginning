package net.ty.createcraftedbeginning.content.opticalpower.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface OpticalPowerSource {
    Source getOpticalPowerSource(Level level, BlockPos pos, BlockState state);

    default int getCurrentOpticalPowerPoints(Level level, BlockPos pos, BlockState state, Source discoveredSource) {
        return getOpticalPowerSource(level, pos, state).powerPoints();
    }

    record Source(BlockPos key, int powerPoints, List<BlockPos> dependencies, List<BlockPos> powerDependencies, boolean dynamic, boolean topologyValid) {
        public Source(BlockPos key, int powerPoints) {
            this(key, powerPoints, List.of(key), List.of(key), false, true);
        }

        public Source {
            key = key.immutable();
            powerPoints = Math.max(0, powerPoints);
            dependencies = immutablePositions(dependencies);
            powerDependencies = immutablePositions(powerDependencies);
        }

        private static @Unmodifiable List<BlockPos> immutablePositions(List<BlockPos> positions) {
            List<BlockPos> immutable = new ArrayList<>(positions.size());
            for (BlockPos position : positions) {
                immutable.add(position.immutable());
            }
            return List.copyOf(immutable);
        }
    }
}
