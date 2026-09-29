package net.ty.createcraftedbeginning.gas.network.solver.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.EndpointFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.TransferComponent;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver.PreparedGraph;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan.PlannedDrain;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan.PlannedFill;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasQuantizedEquilibriumPlanner {
    private static final int MAX_PLAN_ATTEMPTS = 32;
    private static final int MAX_COORDINATED_SCALE_ATTEMPTS = 12;
    private static final double IMPROVEMENT_EPSILON = 1.0E-10;

    private GasQuantizedEquilibriumPlanner() {
    }

    public static GasTransferPlan plan(Level level, PreparedGraph pressureGraph, GasStack gas, List<GasNetworkPressureEndpoint> endpoints, GasPressureGraphSolution solution, TransferComponent component, GasTransportFlowBudget transportBudget, int substepIndex) {
        if (!solution.converged()) {
            return GasTransferPlan.EMPTY;
        }

        List<EndpointFlow> drains = new ArrayList<>();
        List<EndpointFlow> fills = new ArrayList<>();
        for (EndpointFlow solved : component.endpointFlows()) {
            if (solved.drainFlowRate() > GasFlowMath.FLOW_RATE_EPSILON && solved.endpoint().canQuantizedDrain(gas)) {
                drains.add(solved);
            }
            if (solved.fillFlowRate() <= GasFlowMath.FLOW_RATE_EPSILON || !solved.endpoint().canQuantizedFill(gas, solved.manifoldPressurePa())) {
                continue;
            }

            fills.add(solved);
        }
        if (drains.isEmpty() || fills.isEmpty()) {
            return GasTransferPlan.EMPTY;
        }

        int drainCursor = Math.floorMod(level.getGameTime() + substepIndex, drains.size());
        int fillCursor = Math.floorMod(level.getGameTime() + substepIndex, fills.size());
        int attempts = 0;
        pairSearch:
        for (int drainOffset = 0; drainOffset < drains.size(); drainOffset++) {
            EndpointFlow drain = drains.get(Mth.positiveModulo(drainCursor + drainOffset, drains.size()));
            for (int fillOffset = 0; fillOffset < fills.size(); fillOffset++) {
                EndpointFlow fill = fills.get(Mth.positiveModulo(fillCursor + fillOffset, fills.size()));
                if (drain.endpoint() == fill.endpoint()) {
                    continue;
                }

                long preferredAmount = Math.max(drain.endpoint().quantizedPressureStepAmount(), fill.endpoint().quantizedPressureStepAmount());
                long candidateAmount = Math.min(drain.endpoint().maxQuantizedDrainAmount(gas, preferredAmount), fill.endpoint().maxQuantizedFillAmount(gas, preferredAmount, fill.manifoldPressurePa()));
                while (candidateAmount > 0) {
                    if (attempts++ >= MAX_PLAN_ATTEMPTS) {
                        break pairSearch;
                    }

                    long sourcePressurePa = GasPressure.round(GasPressureLimits.clampToHardLimit(fill.manifoldPressurePa()));
                    GasTransferPlan candidate = new GasTransferPlan(List.of(new PlannedDrain(drain.endpoint(), candidateAmount)), List.of(new PlannedFill(fill.endpoint(), candidateAmount, sourcePressurePa)), candidateAmount);
                    if (fitsTransportBudget(component, candidate, transportBudget) && improvesEquilibrium(pressureGraph, gas, endpoints, solution, candidate, transportBudget, false)) {
                        return candidate;
                    }

                    candidateAmount = GasFlowMath.nextSmallerQuantizedAmount(candidateAmount);
                }
            }
        }

        return planCoordinatedQuantizedStep(pressureGraph, gas, endpoints, solution, component, drains, fills, transportBudget, drainCursor, fillCursor);
    }

    public static boolean improvesFlowNorm(GasPressureGraphSolution currentSolution, GasPressureGraphSolution hypotheticalSolution) {
        if (!currentSolution.converged() || !hypotheticalSolution.converged()) {
            return false;
        }

        double currentNorm = dynamicEndpointFlowNorm(currentSolution);
        double hypotheticalNorm = dynamicEndpointFlowNorm(hypotheticalSolution);
        return Double.isFinite(currentNorm) && Double.isFinite(hypotheticalNorm) && currentNorm - hypotheticalNorm > Math.max(IMPROVEMENT_EPSILON, currentNorm * IMPROVEMENT_EPSILON * 10);
    }

    private static boolean fitsTransportBudget(TransferComponent component, GasTransferPlan candidate, GasTransportFlowBudget transportBudget) {
        return component.routing().route(candidate, transportBudget).totalAmount() == candidate.totalAmount();
    }

    private static GasTransferPlan planCoordinatedQuantizedStep(PreparedGraph pressureGraph, GasStack gas, List<GasNetworkPressureEndpoint> endpoints, GasPressureGraphSolution solution, TransferComponent component, List<EndpointFlow> drains, List<EndpointFlow> fills, GasTransportFlowBudget transportBudget, int drainCursor, int fillCursor) {
        GasTransferPlan best = GasTransferPlan.EMPTY;
        long previousAmount = 0;
        long scale = 1;
        for (int attempt = 0; attempt < MAX_COORDINATED_SCALE_ATTEMPTS; attempt++) {
            GasTransferPlan candidate = buildCoordinatedCandidate(gas, drains, fills, drainCursor, fillCursor, scale);
            if (candidate.isEmpty() || candidate.totalAmount() <= previousAmount || !fitsTransportBudget(component, candidate, transportBudget) || !improvesEquilibrium(pressureGraph, gas, endpoints, solution, candidate, transportBudget, true)) {
                break;
            }

            best = candidate;
            previousAmount = candidate.totalAmount();
            if (scale > Long.MAX_VALUE / 2) {
                break;
            }

            scale *= 2;
        }
        return best;
    }

    private static GasTransferPlan buildCoordinatedCandidate(GasStack gas, List<EndpointFlow> drains, List<EndpointFlow> fills, int drainCursor, int fillCursor, long scale) {
        long[] drainAllocations = seedDynamicPressureSteps(gas, drains, scale, true);
        long[] fillAllocations = seedDynamicPressureSteps(gas, fills, scale, false);
        long targetAmount = Math.max(BoundedMath.sumNonNegative(drainAllocations), BoundedMath.sumNonNegative(fillAllocations));
        if (targetAmount <= 0) {
            return GasTransferPlan.EMPTY;
        }

        if (!topUpAllocations(gas, drains, drainAllocations, targetAmount, drainCursor, true) || !topUpAllocations(gas, fills, fillAllocations, targetAmount, fillCursor, false)) {
            return GasTransferPlan.EMPTY;
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

            EndpointFlow fill = fills.get(index);
            long sourcePressurePa = GasPressure.round(GasPressureLimits.clampToHardLimit(fill.manifoldPressurePa()));
            plannedFills.add(new PlannedFill(fill.endpoint(), fillAllocations[index], sourcePressurePa));
        }
        if (plannedDrains.isEmpty() || plannedFills.isEmpty()) {
            return GasTransferPlan.EMPTY;
        }

        return new GasTransferPlan(plannedDrains, plannedFills, targetAmount);
    }

    private static long[] seedDynamicPressureSteps(GasStack gas, List<EndpointFlow> flows, long scale, boolean draining) {
        long[] allocations = new long[flows.size()];
        for (int index = 0; index < flows.size(); index++) {
            GasNetworkPressureEndpoint endpoint = flows.get(index).endpoint();
            if (endpoint.pressureState().fixedPressure()) {
                continue;
            }

            long requested = BoundedMath.saturatedMultiply(endpoint.quantizedPressureStepAmount(), scale);
            allocations[index] = draining ? endpoint.maxQuantizedDrainAmount(gas, requested) : endpoint.maxQuantizedFillAmount(gas, requested, flows.get(index).manifoldPressurePa());
        }
        return allocations;
    }

    private static boolean topUpAllocations(GasStack gas, List<EndpointFlow> flows, long[] allocations, long targetAmount, int cursor, boolean draining) {
        long remaining = targetAmount - BoundedMath.sumNonNegative(allocations);
        if (remaining <= 0) {
            return true;
        }

        for (boolean fixedOnly : new boolean[]{true, false}) {
            for (int offset = 0; offset < flows.size() && remaining > 0; offset++) {
                int index = Mth.positiveModulo(cursor + offset, flows.size());
                GasNetworkPressureEndpoint endpoint = flows.get(index).endpoint();
                if (fixedOnly != endpoint.pressureState().fixedPressure()) {
                    continue;
                }

                long requestedTotal = BoundedMath.saturatedAdd(allocations[index], remaining);
                long availableTotal = draining ? endpoint.maxQuantizedDrainAmount(gas, requestedTotal) : endpoint.maxQuantizedFillAmount(gas, requestedTotal, flows.get(index).manifoldPressurePa());
                long headroom = Math.max(0, availableTotal - allocations[index]);
                long added = Math.min(remaining, headroom);
                allocations[index] = BoundedMath.saturatedAdd(allocations[index], added);
                remaining -= added;
            }
        }
        return remaining <= 0;
    }

    private static boolean improvesEquilibrium(PreparedGraph pressureGraph, GasStack gas, List<GasNetworkPressureEndpoint> endpoints, GasPressureGraphSolution currentSolution, GasTransferPlan candidate, GasTransportFlowBudget transportBudget, boolean rejectDirectionReversal) {
        IdentityHashMap<GasNetworkPressureEndpoint, Long> deltas = new IdentityHashMap<>();
        for (PlannedDrain drain : candidate.drains()) {
            deltas.merge(drain.endpoint(), -drain.amount(), BoundedMath::saturatedAdd);
        }
        for (PlannedFill fill : candidate.fills()) {
            deltas.merge(fill.endpoint(), fill.amount(), BoundedMath::saturatedAdd);
        }

        List<GasNetworkPressureEndpoint> hypotheticalEndpoints = new ArrayList<>(endpoints.size());
        IdentityHashMap<GasNetworkPressureEndpoint, GasNetworkPressureEndpoint> originalByHypothetical = new IdentityHashMap<>();
        boolean changesFiniteEndpoint = false;
        for (GasNetworkPressureEndpoint endpoint : endpoints) {
            long delta = deltas.getOrDefault(endpoint, 0L);
            if (delta != 0 && !endpoint.pressureState().fixedPressure()) {
                changesFiniteEndpoint = true;
            }
            GasNetworkPressureEndpoint hypothetical = endpoint.withAmountDelta(gas, delta);
            if (hypothetical == null) {
                return false;
            }

            hypotheticalEndpoints.add(hypothetical);
            originalByHypothetical.put(hypothetical, endpoint);
        }
        if (!changesFiniteEndpoint) {
            return false;
        }

        GasPressureGraphSolution hypotheticalSolution = pressureGraph.solveHypothetical(hypotheticalEndpoints, transportBudget);
        return hypotheticalSolution.converged() && (!rejectDirectionReversal || !reversesDynamicEndpointFlow(currentSolution, hypotheticalSolution, originalByHypothetical)) && improvesFlowNorm(currentSolution, hypotheticalSolution);
    }

    private static boolean reversesDynamicEndpointFlow(GasPressureGraphSolution currentSolution, GasPressureGraphSolution hypotheticalSolution, IdentityHashMap<GasNetworkPressureEndpoint, GasNetworkPressureEndpoint> originalByHypothetical) {
        IdentityHashMap<GasNetworkPressureEndpoint, Double> hypotheticalNetFlows = new IdentityHashMap<>();
        for (EndpointFlow flow : hypotheticalSolution.endpointFlows()) {
            GasNetworkPressureEndpoint original = originalByHypothetical.get(flow.endpoint());
            if (original == null || original.pressureState().fixedPressure()) {
                continue;
            }

            hypotheticalNetFlows.put(original, flow.drainFlowRate() - flow.fillFlowRate());
        }

        for (EndpointFlow flow : currentSolution.endpointFlows()) {
            GasNetworkPressureEndpoint endpoint = flow.endpoint();
            if (endpoint.pressureState().fixedPressure()) {
                continue;
            }

            double currentNetFlow = flow.drainFlowRate() - flow.fillFlowRate();
            double hypotheticalNetFlow = hypotheticalNetFlows.getOrDefault(endpoint, 0.0);
            if ((currentNetFlow <= GasFlowMath.FLOW_RATE_EPSILON || hypotheticalNetFlow >= -GasFlowMath.FLOW_RATE_EPSILON) && (currentNetFlow >= -GasFlowMath.FLOW_RATE_EPSILON || hypotheticalNetFlow <= GasFlowMath.FLOW_RATE_EPSILON)) {
                continue;
            }

            return true;
        }
        return false;
    }

    private static double dynamicEndpointFlowNorm(GasPressureGraphSolution solution) {
        double norm = 0;
        for (EndpointFlow solved : solution.endpointFlows()) {
            if (solved.endpoint().pressureState().fixedPressure()) {
                continue;
            }

            norm += Math.abs(solved.fillFlowRate() - solved.drainFlowRate());
        }
        return norm;
    }
}
