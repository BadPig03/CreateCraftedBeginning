package net.ty.createcraftedbeginning.compat.sable;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkManager;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalFiberAssemblyCompat {
    private OpticalFiberAssemblyCompat() {
    }

    public static void refreshAfterMove(ServerLevel sourceLevel, ServerLevel resultingLevel, Iterable<BlockPos> positions, UnaryOperator<BlockPos> transform) {
        Set<BlockPos> sourceFibers = new LinkedHashSet<>();
        Set<BlockPos> resultingFibers = new LinkedHashSet<>();
        for (BlockPos pos : positions) {
            collectFibers(sourceLevel, pos, sourceFibers);
            collectFibers(resultingLevel, transform.apply(pos), resultingFibers);
        }
        if (sourceLevel == resultingLevel) {
            sourceFibers.addAll(resultingFibers);
            refreshConnections(sourceLevel, sourceFibers);
            return;
        }

        refreshConnections(sourceLevel, sourceFibers);
        refreshConnections(resultingLevel, resultingFibers);
    }

    private static void collectFibers(ServerLevel level, BlockPos pos, Set<BlockPos> fibers) {
        if (level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof OpticalFiberBlock) {
            fibers.add(pos.immutable());
        }
        for (Direction direction : Iterate.directions) {
            BlockPos neighbor = pos.relative(direction);
            if (!level.isLoaded(neighbor) || !(level.getBlockState(neighbor).getBlock() instanceof OpticalFiberBlock)) {
                continue;
            }

            fibers.add(neighbor);
        }
    }

    private static void refreshConnections(ServerLevel level, Set<BlockPos> fibers) {
        for (BlockPos pos : fibers) {
            if (!level.isLoaded(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof OpticalFiberBlock)) {
                continue;
            }

            OpticalPowerNetworkManager.invalidateAt(level, pos);
            BlockState updated = state;
            for (Direction direction : Iterate.directions) {
                BlockPos neighbor = pos.relative(direction);
                boolean connected = level.isLoaded(neighbor) && OpticalFiberBlock.canConnectTo(level, neighbor, level.getBlockState(neighbor), direction.getOpposite());
                updated = updated.setValue(OpticalFiberBlock.PROPERTY_BY_DIRECTION.get(direction), connected);
            }
            if (updated == state) {
                continue;
            }

            level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
