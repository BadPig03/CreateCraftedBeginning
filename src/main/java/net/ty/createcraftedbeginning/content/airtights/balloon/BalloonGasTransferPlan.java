package net.ty.createcraftedbeginning.content.airtights.balloon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureFillService;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferEndpoint;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BalloonGasTransferPlan {
    private final GasHandler target;
    private final GasStack gas;
    private final long sourcePressurePa;
    private final TransferPolicy policy;
    private final long plannedAmount;
    private final List<PlannedFill> plannedFills;

    private BalloonGasTransferPlan(GasHandler target, GasStack gas, long sourcePressurePa, TransferPolicy policy, long plannedAmount, List<PlannedFill> plannedFills) {
        this.target = target;
        this.gas = gas.copy();
        this.sourcePressurePa = sourcePressurePa;
        this.policy = policy;
        this.plannedAmount = Mth.clamp(plannedAmount, 0L, gas.getAmount());
        this.plannedFills = List.copyOf(plannedFills);
    }

    public static BalloonGasTransferPlan plan(GasHandler target, GasStack gas, long sourcePressurePa, TransferPolicy policy) {
        if (gas.isEmpty() || sourcePressurePa <= GasPressure.VACUUM_PA) {
            return new BalloonGasTransferPlan(target, gas, sourcePressurePa, policy, 0, List.of());
        }

        if (policy == TransferPolicy.FULL_ONLY) {
            boolean accepted = target.tryFillAtomicallyFromPressure(List.of(gas.copy()), sourcePressurePa, GasAction.SIMULATE).isSuccess();
            if (!accepted) {
                return new BalloonGasTransferPlan(target, gas, sourcePressurePa, policy, 0, List.of());
            }

            return new BalloonGasTransferPlan(target, gas, sourcePressurePa, policy, gas.getAmount(), List.of());
        }

        List<PlannedFill> fills = new ArrayList<>();
        long remainingAmount = gas.getAmount();
        remainingAmount = appendPass(target, gas, sourcePressurePa, true, remainingAmount, fills);
        remainingAmount = appendPass(target, gas, sourcePressurePa, false, remainingAmount, fills);
        long acceptedAmount = gas.getAmount() - remainingAmount;
        return new BalloonGasTransferPlan(target, gas, sourcePressurePa, policy, acceptedAmount, fills);
    }

    public GasStack gas() {
        return gas.copy();
    }

    public long sourcePressurePa() {
        return sourcePressurePa;
    }

    public long acceptedAmount() {
        return plannedAmount;
    }

    public long remainingAmount() {
        return gas.getAmount() - plannedAmount;
    }

    public boolean canTransfer() {
        return plannedAmount > 0;
    }

    public boolean acceptsEntireBalloon() {
        return !gas.isEmpty() && plannedAmount == gas.getAmount();
    }

    public ExecutionResult execute() {
        if (!canTransfer()) {
            return ExecutionResult.NONE;
        }

        if (policy == TransferPolicy.FULL_ONLY) {
            if (!target.tryFillAtomicallyFromPressure(List.of(gas.copy()), sourcePressurePa, GasAction.EXECUTE).isSuccess()) {
                return ExecutionResult.NONE;
            }

            return new ExecutionResult(gas.getAmount(), true);
        }

        long transferredAmount = 0;
        for (PlannedFill plannedFill : plannedFills) {
            long remainingAmount = gas.getAmount() - transferredAmount;
            if (remainingAmount <= 0) {
                break;
            }

            long requestedAmount = Math.min(remainingAmount, plannedFill.amount());
            long filledAmount = plannedFill.endpoint().executeFill(gas, requestedAmount, sourcePressurePa);
            transferredAmount = BoundedMath.saturatedAdd(transferredAmount, Mth.clamp(filledAmount, 0L, requestedAmount));
        }

        transferredAmount = Mth.clamp(transferredAmount, 0L, gas.getAmount());
        if (transferredAmount <= 0) {
            return ExecutionResult.NONE;
        }

        return new ExecutionResult(transferredAmount, transferredAmount == gas.getAmount());
    }

    private static long appendPass(GasHandler target, GasStack gas, long sourcePressurePa, boolean matchingPass, long remainingAmount, List<PlannedFill> fills) {
        for (int tankIndex = 0; tankIndex < target.getTanks() && remainingAmount > 0; tankIndex++) {
            GasStack storedGas = target.getGasInTank(tankIndex);
            boolean matching = !storedGas.isEmpty() && GasStack.isSameGasSameComponents(storedGas, gas);
            if (matchingPass != matching || !matching && !storedGas.isEmpty()) {
                continue;
            }

            Optional<GasPressureTransferEndpoint> endpoint = GasPressureTransferEndpoint.tryHandler(target, tankIndex);
            if (endpoint.isEmpty() || alreadyPlanned(fills, endpoint.get())) {
                continue;
            }

            long transferableAmount = GasPressureFillService.getTransferableAmount(endpoint.get(), gas, remainingAmount, sourcePressurePa);
            if (transferableAmount <= 0) {
                continue;
            }

            fills.add(new PlannedFill(endpoint.get(), transferableAmount));
            remainingAmount -= transferableAmount;
        }
        return remainingAmount;
    }

    private static boolean alreadyPlanned(List<PlannedFill> fills, GasPressureTransferEndpoint endpoint) {
        for (PlannedFill fill : fills) {
            if (!fill.endpoint().identifiesSameCompartment(endpoint)) {
                continue;
            }

            return true;
        }
        return false;
    }

    public enum TransferPolicy {
        FULL_ONLY,
        BEST_EFFORT
    }

    public record ExecutionResult(long transferredAmount, boolean complete) {
        private static final ExecutionResult NONE = new ExecutionResult(0, false);

        public ExecutionResult {
            transferredAmount = Math.max(0, transferredAmount);
        }
    }

    private record PlannedFill(GasPressureTransferEndpoint endpoint, long amount) {}
}
