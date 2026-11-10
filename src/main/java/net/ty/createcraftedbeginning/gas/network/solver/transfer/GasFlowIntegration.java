package net.ty.createcraftedbeginning.gas.network.solver.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.EndpointFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.TransferComponent;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasRoutedTransferExecutor.ExecutedRoute;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.IdentityHashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFlowIntegration {
    private final Map<Object, IntegratedAmount> drainAmounts = new IdentityHashMap<>();
    private final Map<Object, IntegratedAmount> fillAmounts = new IdentityHashMap<>();

    public long available(GasNetworkPressureEndpoint endpoint, double rate, double fraction, boolean draining) {
        Map<Object, IntegratedAmount> amounts = draining ? drainAmounts : fillAmounts;
        IntegratedAmount amount = amounts.get(endpoint.transferIdentity());
        if (amount == null) {
            return GasFlowMath.amountForTickFraction(rate, fraction);
        }

        return Math.max(0, GasFlowMath.toWholeAmount(amount.integrated + rate * fraction) - amount.consumed);
    }

    public long available(TransferComponent component, double fraction) {
        double integratedDrains = 0;
        double integratedFills = 0;
        long consumedDrains = 0;
        long consumedFills = 0;
        for (EndpointFlow flow : component.endpointFlows()) {
            boolean draining = flow.drainFlowRate() > GasFlowMath.FLOW_RATE_EPSILON;
            Map<Object, IntegratedAmount> amounts = draining ? drainAmounts : fillAmounts;
            IntegratedAmount previous = amounts.get(flow.endpoint().transferIdentity());
            double integrated = previous == null ? 0 : previous.integrated;
            long consumed = previous == null ? 0 : previous.consumed;
            if (draining) {
                integratedDrains += integrated + flow.drainFlowRate() * fraction;
                consumedDrains = BoundedMath.saturatedAdd(consumedDrains, consumed);
                continue;
            }

            integratedFills += integrated + flow.fillFlowRate() * fraction;
            consumedFills = BoundedMath.saturatedAdd(consumedFills, consumed);
        }
        long drains = Math.max(0, GasFlowMath.toWholeAmount(integratedDrains) - consumedDrains);
        long fills = Math.max(0, GasFlowMath.toWholeAmount(integratedFills) - consumedFills);
        return Math.min(drains, fills);
    }

    public void advance(TransferComponent component, double fraction) {
        for (EndpointFlow flow : component.endpointFlows()) {
            GasNetworkPressureEndpoint endpoint = flow.endpoint();
            Object identity = endpoint.transferIdentity();
            boolean draining = flow.drainFlowRate() > GasFlowMath.FLOW_RATE_EPSILON;
            double rate = draining ? flow.drainFlowRate() : flow.fillFlowRate();
            Map<Object, IntegratedAmount> amounts = draining ? drainAmounts : fillAmounts;
            IntegratedAmount amount = amounts.computeIfAbsent(identity, ignored -> new IntegratedAmount());
            amount.integrated += rate * fraction;
        }
    }

    public void record(ExecutedRoute transfer) {
        IntegratedAmount source = drainAmounts.get(transfer.route().source().transferIdentity());
        IntegratedAmount target = fillAmounts.get(transfer.route().target().transferIdentity());
        if (source != null) {
            source.consumed = BoundedMath.saturatedAdd(source.consumed, transfer.amount());
        }
        if (target == null) {
            return;
        }

        target.consumed = BoundedMath.saturatedAdd(target.consumed, transfer.amount());
    }

    private static final class IntegratedAmount {
        private double integrated;
        private long consumed;
    }
}
