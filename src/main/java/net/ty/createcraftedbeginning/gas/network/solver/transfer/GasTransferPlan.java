package net.ty.createcraftedbeginning.gas.network.solver.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PooledTransferExecutionResult;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasTransferPlan(List<PlannedDrain> drains, List<PlannedFill> fills, long totalAmount) {
    public static final GasTransferPlan EMPTY = new GasTransferPlan(List.of(), List.of(), 0);

    public GasTransferPlan {
        drains = List.copyOf(drains);
        fills = List.copyOf(fills);
    }

    public boolean isEmpty() {
        return totalAmount <= 0 || drains.isEmpty() || fills.isEmpty();
    }

    public @Unmodifiable List<GasTransferExecutor.PlannedDrain> executorDrains() {
        List<GasTransferExecutor.PlannedDrain> plan = new ArrayList<>(drains.size());
        for (PlannedDrain drain : drains) {
            if (drain.endpoint.access().drainHandler() == null) {
                continue;
            }

            plan.add(new GasTransferExecutor.PlannedDrain(drain.endpoint.access().drainHandler(), drain.amount));
        }
        return List.copyOf(plan);
    }

    public @Unmodifiable List<GasTransferExecutor.PlannedFill> executorFills() {
        List<GasTransferExecutor.PlannedFill> plan = new ArrayList<>(fills.size());
        for (PlannedFill fill : fills) {
            if (fill.endpoint.access().fillHandler() == null) {
                continue;
            }

            plan.add(new GasTransferExecutor.PlannedFill(fill.endpoint.access().fillHandler(), fill.amount, fill.sourcePressurePa));
        }
        return List.copyOf(plan);
    }

    public void restoreRemainder(Level level, GasStack gas, PooledTransferExecutionResult execution) {
        GasStack remainder = execution.remainingGas();
        if (remainder.isEmpty()) {
            return;
        }

        long remaining = remainder.getAmount();
        long[] drainedAmounts = execution.drainedAmounts();
        for (int index = 0; index < drains.size() && index < drainedAmounts.length && remaining > 0; index++) {
            GasNetworkPressureEndpoint endpoint = drains.get(index).endpoint;
            long amount = Math.min(remaining, drainedAmounts[index]);
            if (amount <= 0) {
                continue;
            }

            long secured = endpoint.restoreRemainder(level, gas.copyWithAmount(amount));
            remaining = BoundedMath.saturatedSubtract(remaining, secured);
        }
        if (remaining <= 0) {
            return;
        }

        for (PlannedDrain drain : drains) {
            if (remaining <= 0) {
                break;
            }

            long secured = drain.endpoint.restoreRemainder(level, gas.copyWithAmount(remaining));
            remaining = BoundedMath.saturatedSubtract(remaining, secured);
        }
    }

    public record PlannedDrain(GasNetworkPressureEndpoint endpoint, long amount) {}

    public record PlannedFill(GasNetworkPressureEndpoint endpoint, long amount, long sourcePressurePa) {}
}
