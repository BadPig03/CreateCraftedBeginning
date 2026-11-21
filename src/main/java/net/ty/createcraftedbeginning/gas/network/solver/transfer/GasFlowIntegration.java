package net.ty.createcraftedbeginning.gas.network.solver.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.flow.GasIntakeListener;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan.PlannedDrain;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan.PlannedFill;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;

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

    public long executeQuantum(Level level, GasStack gas, GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long sourcePressure, long amount) {
        return executeQuantum(level, gas, source, target, sourcePressure, amount, ignored -> {});
    }

    public long executeQuantum(Level level, GasStack gas, GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long sourcePressure, long amount, LongConsumer onCommitted) {
        if (amount <= 0 || source.transferIdentity() == target.transferIdentity() || source.access().drainHandler() == null || target.access().fillHandler() == null) {
            return 0;
        }

        GasTransferPlan plan = new GasTransferPlan(List.of(new PlannedDrain(source, amount)), List.of(new PlannedFill(target, amount, sourcePressure)), amount);
        IntegratedAmount drain = drainAmounts.computeIfAbsent(source.transferIdentity(), ignored -> new IntegratedAmount());
        IntegratedAmount fill = fillAmounts.computeIfAbsent(target.transferIdentity(), ignored -> new IntegratedAmount());
        long previous = drain.consumed;
        drain.integrated += amount;
        fill.integrated += amount;
        try {
            return GasPairTransferExecutor.executePair(level, gas, plan, accepted -> {
                drain.consumed = BoundedMath.saturatedAdd(drain.consumed, accepted);
                fill.consumed = BoundedMath.saturatedAdd(fill.consumed, accepted);
                onCommitted.accept(accepted);
                if (source.access().drainHandler() instanceof GasIntakeListener listener) {
                    listener.onGasIntake(accepted);
                }
            });
        }
        finally {
            long unused = amount - (drain.consumed - previous);
            drain.integrated -= unused;
            fill.integrated -= unused;
        }
    }

    private static final class IntegratedAmount {
        private double integrated;
        private long consumed;
    }
}
