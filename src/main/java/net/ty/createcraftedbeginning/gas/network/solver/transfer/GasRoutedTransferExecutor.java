package net.ty.createcraftedbeginning.gas.network.solver.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasFlowRouting.Route;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasFlowRouting.RoutedPlan;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PooledTransferExecutionResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRoutedTransferExecutor {
    private GasRoutedTransferExecutor() {}

    public static List<ExecutedRoute> execute(Level level, GasStack gas, RoutedPlan routed, GasTransportFlowBudget budget) {
        return execute(level, gas, routed, budget, transfer -> {});
    }

    public static List<ExecutedRoute> execute(Level level, GasStack gas, RoutedPlan routed, GasTransportFlowBudget budget, Consumer<ExecutedRoute> onExecuted) {
        List<ExecutedRoute> executed = new ArrayList<>();
        for (Route route : routed.routes()) {
            long amount = route.availableAmount(budget);
            if (amount <= 0 || route.source().access().drainHandler() == null || route.target().access().fillHandler() == null) {
                continue;
            }

            GasTransferPlan plan = route.transferPlan(amount);
            PooledTransferExecutionResult execution = GasTransferExecutor.executePooledTransfer(gas, plan.executorDrains(), plan.executorFills(), partial -> settleRoute(level, gas, route, plan, partial, budget, executed, onExecuted));
            settleRoute(level, gas, route, plan, execution, budget, executed, onExecuted);
        }
        return List.copyOf(executed);
    }

    private static void settleRoute(Level level, GasStack gas, Route route, GasTransferPlan plan, PooledTransferExecutionResult execution, GasTransportFlowBudget budget, List<ExecutedRoute> executed, Consumer<ExecutedRoute> onExecuted) {
        @Nullable Throwable failure = null;
        try {
            long filled = execution.filledAmounts()[0];
            if (filled <= 0) {
                return;
            }

            for (Entry<BlockPos,Integer> entry : route.budgetVisits().entrySet()) {
                budget.consume(entry.getKey(), filled * entry.getValue());
            }
            ExecutedRoute transfer = new ExecutedRoute(route, filled);
            executed.add(transfer);
            onExecuted.accept(transfer);
        }
        catch (RuntimeException | Error caught) {
            failure = caught;
            throw caught;
        }
        finally {
            try {
                plan.restoreRemainder(level, gas, execution);
            }
            catch (RuntimeException | Error recoveryFailure) {
                if (failure == null) {
                    throw recoveryFailure;
                }

                if (recoveryFailure != failure) {
                    failure.addSuppressed(recoveryFailure);
                }
            }
        }
    }

    public record ExecutedRoute(Route route, long amount) {}
}
