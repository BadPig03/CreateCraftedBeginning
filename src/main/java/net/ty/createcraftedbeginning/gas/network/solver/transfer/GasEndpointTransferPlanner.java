package net.ty.createcraftedbeginning.gas.network.solver.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.PressureState;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.EndpointFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.TransferComponent;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan.PlannedDrain;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan.PlannedFill;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasEndpointTransferPlanner {
    private static final double ENDPOINT_APPROACH_FRACTION = 0.5;

    private GasEndpointTransferPlanner() {
    }

    public static double calculateSubstepFraction(GasPressureGraphSolution solution, double remainingTickFraction, GasTransportFlowBudget transportBudget) {
        double fraction = remainingTickFraction;
        for (EndpointFlow solved : solution.endpointFlows()) {
            GasNetworkPressureEndpoint endpoint = solved.endpoint();
            double rate = solved.drainFlowRate() > GasFlowMath.FLOW_RATE_EPSILON ? solved.drainFlowRate() : solved.fillFlowRate();
            if (rate <= GasFlowMath.FLOW_RATE_EPSILON) {
                continue;
            }

            PressureState pressureState = endpoint.pressureState();
            if (pressureState.fixedPressure()) {
                continue;
            }

            double targetPressure = solved.drainFlowRate() > GasFlowMath.FLOW_RATE_EPSILON ? GasPressureLimits.clampToHardLimit(solved.manifoldPressurePa()) : Mth.clamp(solved.manifoldPressurePa(), GasPressure.VACUUM_PA, pressureState.maxPressurePa());
            double pressureDistance = Math.abs(pressureState.pressurePa() - targetPressure);
            double amountToManifold = GasPressure.amount(pressureState.volume(), pressureDistance);
            if (amountToManifold <= GasFlowMath.FLOW_RATE_EPSILON) {
                continue;
            }

            fraction = Math.min(fraction, ENDPOINT_APPROACH_FRACTION * amountToManifold / rate);
        }

        for (TransferComponent component : solution.transferComponents()) {
            for (Entry<BlockPos, Double> budgetedFlow : component.budgetedFlowRates().entrySet()) {
                double rate = budgetedFlow.getValue();
                if (rate <= GasFlowMath.FLOW_RATE_EPSILON) {
                    continue;
                }

                long remaining = transportBudget.remainingKnown(budgetedFlow.getKey());
                if (remaining == Long.MAX_VALUE) {
                    continue;
                }

                fraction = Math.min(fraction, remaining / rate);
            }
        }

        if (!Double.isFinite(fraction)) {
            return remainingTickFraction;
        }

        return Mth.clamp(fraction, 0, remainingTickFraction);
    }

    public static GasTransferPlan plan(Level level, GasStack gas, TransferComponent component, double substepFraction, int substepIndex) {
        List<EndpointFlow> drains = new ArrayList<>();
        List<EndpointFlow> fills = new ArrayList<>();
        for (EndpointFlow endpointFlow : component.endpointFlows()) {
            if (endpointFlow.drainFlowRate() > GasFlowMath.FLOW_RATE_EPSILON && endpointFlow.endpoint().access().drainHandler() != null) {
                drains.add(endpointFlow);
                continue;
            }

            if (endpointFlow.fillFlowRate() <= GasFlowMath.FLOW_RATE_EPSILON || endpointFlow.endpoint().access().fillHandler() == null) {
                continue;
            }

            fills.add(endpointFlow);
        }
        if (drains.isEmpty() || fills.isEmpty()) {
            return GasTransferPlan.EMPTY;
        }

        long[] drainLimits = new long[drains.size()];
        long[] drainWeights = new long[drains.size()];
        for (int index = 0; index < drains.size(); index++) {
            EndpointFlow solved = drains.get(index);
            long flowBudget = GasFlowMath.amountForTickFraction(solved.drainFlowRate(), substepFraction);
            long safeAmountLimit = solved.endpoint().maxSafeDrainAmount(gas, solved.manifoldPressurePa(), flowBudget);
            drainLimits[index] = safeAmountLimit;
            drainWeights[index] = safeAmountLimit <= 0 ? 0 : Mth.clamp(flowBudget, 1, safeAmountLimit);
        }

        long totalDrainLimit = BoundedMath.sumNonNegative(drainLimits);
        long[] fillLimits = new long[fills.size()];
        long[] fillWeights = new long[fills.size()];
        for (int index = 0; index < fills.size(); index++) {
            EndpointFlow solved = fills.get(index);
            long flowBudget = GasFlowMath.amountForTickFraction(solved.fillFlowRate(), substepFraction);
            long safeAmountLimit = solved.endpoint().maxSafeFillAmount(gas, solved.manifoldPressurePa(), flowBudget);
            fillLimits[index] = safeAmountLimit;
            fillWeights[index] = safeAmountLimit <= 0 ? 0 : Mth.clamp(flowBudget, 1, safeAmountLimit);
        }

        long totalFillLimit = BoundedMath.sumNonNegative(fillLimits);
        long referenceAmount = GasFlowMath.amountForTickFraction(component.referenceFlowRate(), substepFraction);
        long plannedAmount = Math.min(referenceAmount, Math.min(totalDrainLimit, totalFillLimit));
        if (plannedAmount <= 0) {
            return GasTransferPlan.EMPTY;
        }

        int drainCursor = Math.floorMod(level.getGameTime() + substepIndex, drains.size());
        int fillCursor = Math.floorMod(level.getGameTime() + substepIndex, fills.size());
        long[] drainAllocations = GasAmountDistribution.allocateByWeight(plannedAmount, drainLimits, drainWeights, drainCursor);
        long[] fillAllocations = GasAmountDistribution.allocateByWeight(plannedAmount, fillLimits, fillWeights, fillCursor);
        long allocatedDrain = BoundedMath.sumNonNegative(drainAllocations);
        long allocatedFill = BoundedMath.sumNonNegative(fillAllocations);
        long allocatedAmount = Math.min(allocatedDrain, allocatedFill);
        if (allocatedAmount <= 0) {
            return GasTransferPlan.EMPTY;
        }

        if (allocatedDrain != allocatedAmount) {
            drainAllocations = GasAmountDistribution.allocateByWeight(allocatedAmount, drainLimits, drainWeights, drainCursor);
        }
        if (allocatedFill != allocatedAmount) {
            fillAllocations = GasAmountDistribution.allocateByWeight(allocatedAmount, fillLimits, fillWeights, fillCursor);
        }

        List<PlannedDrain> plannedDrains = new ArrayList<>();
        for (int index = 0; index < drainAllocations.length; index++) {
            if (drainAllocations[index] <= 0) {
                continue;
            }

            plannedDrains.add(new PlannedDrain(drains.get(index).endpoint(), drainAllocations[index]));
        }

        List<PlannedFill> plannedFills = new ArrayList<>();
        for (int index = 0; index < fillAllocations.length; index++) {
            if (fillAllocations[index] <= 0) {
                continue;
            }

            EndpointFlow fillFlow = fills.get(index);
            long sourcePressurePa = GasPressure.round(GasPressureLimits.clampToHardLimit(fillFlow.manifoldPressurePa()));
            plannedFills.add(new PlannedFill(fillFlow.endpoint(), fillAllocations[index], sourcePressurePa));
        }

        if (plannedDrains.isEmpty() || plannedFills.isEmpty()) {
            return GasTransferPlan.EMPTY;
        }

        return new GasTransferPlan(plannedDrains, plannedFills, allocatedAmount);
    }
}
