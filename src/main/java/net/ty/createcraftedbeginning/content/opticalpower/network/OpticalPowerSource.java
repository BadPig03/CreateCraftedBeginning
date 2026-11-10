package net.ty.createcraftedbeginning.content.opticalpower.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface OpticalPowerSource {
    Source resolveOpticalPowerSource(Level level, BlockPos pos, BlockState state);

    default boolean canConnectOpticalPower(BlockState state, Direction side) {
        return true;
    }

    default double getCurrentOpticalFluxLm(Level level, BlockPos pos, BlockState state, Source discoveredSource) {
        return resolveOpticalPowerSource(level, pos, state).fluxLm();
    }

    record Source(BlockPos key, double fluxLm, List<BlockPos> dependencies, List<BlockPos> powerDependencies, boolean dynamic, boolean topologyValid) {
        public Source(BlockPos key, double fluxLm) {
            this(key, fluxLm, List.of(key), List.of(key), false, true);
        }

        public Source {
            key = key.immutable();
            if (!Double.isFinite(fluxLm) || fluxLm < 0) {
                throw new IllegalArgumentException("Source luminous flux must be finite and non-negative; got " + fluxLm + " lm.");
            }

            dependencies = immutablePositions(dependencies);
            powerDependencies = immutablePositions(powerDependencies);
        }

        private static @Unmodifiable List<BlockPos> immutablePositions(List<BlockPos> positions) {
            return positions.stream().map(BlockPos::immutable).toList();
        }
    }
}
