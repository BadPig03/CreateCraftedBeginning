package net.ty.createcraftedbeginning.gas.network.solver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.scheduling.GasTransferScheduler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasNetworkSimulator {
    private GasNetworkSimulator() {
    }

    public static void simulateTick(Level level, BlockPos startPos) {
        if (level.isClientSide) {
            return;
        }

        long tick = level.getGameTime();
        if (GasNetworkTopology.alreadySimulated(level, startPos, tick) || GasConnectionResolver.getTransportBehaviour(level, startPos) == null) {
            return;
        }

        Snapshot topology = GasNetworkTopology.get(level, startPos);
        GasNetworkState state = topology.state();
        if (state.simulatedAt(tick) || topology.pipePositions().isEmpty()) {
            return;
        }

        state.markSimulated(tick);
        GasTransferScheduler scheduler = topology.scheduler();
        if (scheduler == null) {
            scheduler = topology.activateBatch(level);
        }

        scheduler.tick(level);
    }
}
