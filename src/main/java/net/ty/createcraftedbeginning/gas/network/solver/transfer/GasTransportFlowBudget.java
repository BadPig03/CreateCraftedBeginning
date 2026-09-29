package net.ty.createcraftedbeginning.gas.network.solver.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasTransportFlowBudget {
    private final Map<BlockPos, Long> flowRateLimitByTransport = new HashMap<>();
    private final Map<BlockPos, Long> consumedFlowByTransport = new HashMap<>();

    public long remaining(BlockPos transportPos, long configuredFlowRateLimit) {
        if (configuredFlowRateLimit == Long.MAX_VALUE) {
            flowRateLimitByTransport.put(transportPos.immutable(), Long.MAX_VALUE);
            return Long.MAX_VALUE;
        }

        long configured = Math.max(0, configuredFlowRateLimit);
        flowRateLimitByTransport.merge(transportPos.immutable(), configured, Math::min);
        long consumed = consumedFlowByTransport.getOrDefault(transportPos, 0L);
        return Math.max(0, BoundedMath.saturatedSubtract(configured, consumed));
    }

    public long remainingKnown(BlockPos transportPos) {
        long configured = flowRateLimitByTransport.getOrDefault(transportPos, Long.MAX_VALUE);
        if (configured == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return Math.max(0, BoundedMath.saturatedSubtract(configured, consumedFlowByTransport.getOrDefault(transportPos, 0L)));
    }

    public void consume(BlockPos transportPos, long amount) {
        if (amount <= 0 || flowRateLimitByTransport.getOrDefault(transportPos, Long.MAX_VALUE) == Long.MAX_VALUE) {
            return;
        }

        consumedFlowByTransport.merge(transportPos.immutable(), amount, BoundedMath::saturatedAdd);
    }

    public boolean canConsumeFlowRates(Map<BlockPos, Double> transportFlowRates, double flowScale) {
        if (!Double.isFinite(flowScale) || flowScale <= 0) {
            return false;
        }

        for (Entry<BlockPos, Double> entry : transportFlowRates.entrySet()) {
            long amount = GasFlowMath.amountForScale(entry.getValue(), flowScale);
            if (amount > remainingKnown(entry.getKey())) {
                return false;
            }
        }
        return true;
    }

    public void consumeFlowRates(Map<BlockPos, Double> transportFlowRates, double flowScale) {
        if (flowScale <= 0) {
            return;
        }

        for (Entry<BlockPos, Double> entry : transportFlowRates.entrySet()) {
            long amount = GasFlowMath.amountForScale(entry.getValue(), flowScale);
            if (amount <= 0) {
                continue;
            }

            consume(entry.getKey(), amount);
        }
    }
}
