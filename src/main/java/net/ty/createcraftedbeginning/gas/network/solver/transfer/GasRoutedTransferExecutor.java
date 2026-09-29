package net.ty.createcraftedbeginning.gas.network.solver.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasFlowRouting.Route;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasFlowRouting.RoutedPlan;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PooledTransferExecutionResult;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRoutedTransferExecutor {
    private GasRoutedTransferExecutor() {}

    public static List<ExecutedRoute> execute(Level level, GasStack gas, RoutedPlan routed, GasTransportFlowBudget budget) {
        List<ExecutedRoute> executed = new ArrayList<>();
        for (Route route : routed.routes()) {
            long amount = route.availableAmount(budget);
            if (amount <= 0 || route.source().access().drainHandler() == null || route.target().access().fillHandler() == null) {
                continue;
            }

            GasTransferPlan plan = route.transferPlan(amount);
            PooledTransferExecutionResult execution = GasTransferExecutor.executePooledTransfer(gas, plan.executorDrains(), plan.executorFills());
            plan.restoreRemainder(level, gas, execution);
            long filled = execution.filledAmounts()[0];
            if (filled <= 0) {
                continue;
            }

            for (Entry<BlockPos,Integer> entry : route.budgetVisits().entrySet()) {
                budget.consume(entry.getKey(), filled * entry.getValue());
            }
            executed.add(new ExecutedRoute(route, filled));
        }
        return List.copyOf(executed);
    }

    public record ExecutedRoute(Route route, long amount) {}
}
