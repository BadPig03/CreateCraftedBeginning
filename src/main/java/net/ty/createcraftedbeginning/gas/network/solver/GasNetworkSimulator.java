package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.data.WorldAttached;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasFlowRouting.RoutedPlan;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.TransferComponent;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver.PreparedGraph;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasEndpointTransferPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasFlowIntegration;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasQuantizedEquilibriumPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasRoutedTransferExecutor.ExecutedRoute;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasRoutedTransferExecutor;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransportFlowBudget;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasNetworkSimulator {
    private static final WorldAttached<NetworkSimulationBatch> SIMULATION_BATCHES = new WorldAttached<>(ignored -> new NetworkSimulationBatch());
    private static final int MAX_DYNAMIC_SUBSTEPS = 16;
    private static final double MIN_SUBSTEP_FRACTION = 1.0E-6;

    private GasNetworkSimulator() {
    }

    public static void simulateTick(Level level, BlockPos startPos) {
        if (level.isClientSide || GasConnectionResolver.getTransportBehaviour(level, startPos) == null) {
            return;
        }

        NetworkSimulationBatch batch = SIMULATION_BATCHES.get(level);
        batch.begin(level.getGameTime());
        if (batch.processedPipes.contains(startPos)) {
            return;
        }

        boolean profiling = GasSolverProfiler.beginNetwork(startPos);
        try {
            long topologyStart = profiling ? System.nanoTime() : 0;
            Snapshot topology = GasNetworkTopology.get(level, startPos);
            if (profiling) {
                GasSolverProfiler.recordTopology(System.nanoTime() - topologyStart, topology.pipePositions().size(), topology.endpointFaces().size() + topology.atmosphericFaces().size());
            }
            batch.processedPipes.addAll(topology.pipePositions());
            if (topology.pipePositions().isEmpty()) {
                return;
            }

            long endpointPlanningStart = profiling ? System.nanoTime() : 0;
            GasEndpointPlanner endpointPlanner = GasEndpointPlanner.prepare(level, topology);
            if (profiling) {
                GasSolverProfiler.recordEndpointPlanning(System.nanoTime() - endpointPlanningStart, endpointPlanner.gasGroups().size());
            }
            GasTransportFlowBudget transportBudget = new GasTransportFlowBudget();
            GasPipeTelemetryTargets telemetryTargets = GasPipeTelemetryTargets.get(level, topology);
            GasPipeFlowAccumulator flowAccumulator = new GasPipeFlowAccumulator(telemetryTargets);
            GasPipePressureAccumulator pressureAccumulator = new GasPipePressureAccumulator(telemetryTargets);
            boolean pressureTelemetryEnabled = pressureAccumulator.isEnabled();
            if (profiling) {
                GasSolverProfiler.recordTelemetryTargets(telemetryTargets.pressureTargetCount(), telemetryTargets.flowTargetCount());
            }
            @Nullable Throwable failure = null;
            try {
                for (GasStack gas : endpointPlanner.gasGroups()) {
                    simulateGas(level, topology, gas, endpointPlanner, transportBudget, flowAccumulator, pressureAccumulator, profiling);
                    if (pressureTelemetryEnabled) {
                        pressureAccumulator.finishGas();
                    }
                    if (!flowAccumulator.hasCollision()) {
                        continue;
                    }

                    break;
                }
            }
            catch (RuntimeException | Error caught) {
                failure = caught;
                throw caught;
            }
            finally {
                long telemetryStart = profiling ? System.nanoTime() : 0;
                try {
                    flowAccumulator.apply(level, topology.pipePositions());
                    if (failure == null && pressureTelemetryEnabled) {
                        pressureAccumulator.apply(level);
                    }
                }
                catch (RuntimeException | Error telemetryFailure) {
                    if (failure == null) {
                        throw telemetryFailure;
                    }

                    if (telemetryFailure != failure) {
                        failure.addSuppressed(telemetryFailure);
                    }
                }
                finally {
                    if (profiling) {
                        GasSolverProfiler.recordTelemetry(System.nanoTime() - telemetryStart);
                    }
                }
            }
        }
        finally {
            if (profiling) {
                GasSolverProfiler.finishNetwork();
            }
        }
    }

    public static void recordSubstepCapIfExhausted(int executedSubsteps, double remainingTickFraction) {
        if (executedSubsteps < MAX_DYNAMIC_SUBSTEPS || remainingTickFraction <= MIN_SUBSTEP_FRACTION) {
            return;
        }

        GasSolverProfiler.recordSubstepCapHit(remainingTickFraction);
    }

    private static void simulateGas(Level level, Snapshot topology, GasStack gas, GasEndpointPlanner endpointPlanner, GasTransportFlowBudget transportBudget, GasPipeFlowAccumulator flowAccumulator, GasPipePressureAccumulator pressureAccumulator, boolean profiling) {
        if (profiling) {
            GasSolverProfiler.recordGasSolve();
        }
        long graphPreparationStart = profiling ? System.nanoTime() : 0;
        PreparedGraph pressureGraph = GasPressureGraphSolver.prepare(level, topology, gas);
        if (profiling) {
            GasSolverProfiler.recordGraphPreparation(System.nanoTime() - graphPreparationStart);
        }
        Map<Object, SimulationClock> clocksByEndpoint = new IdentityHashMap<>();
        List<SimulationClock> clocks = new ArrayList<>();
        for (Set<Object> identities : pressureGraph.independentEndpointGroups(endpointPlanner.planPressureEndpoints(level, gas, true))) {
            SimulationClock clock = new SimulationClock();
            clocks.add(clock);
            for (Object identity : identities) {
                clocksByEndpoint.put(identity, clock);
            }
        }
        GasFlowIntegration integration = new GasFlowIntegration();
        double remainingTickFraction = 1;
        int executedSubsteps = 0;
        boolean quantizationAttempted = false;
        for (int substepIndex = 0; substepIndex < MAX_DYNAMIC_SUBSTEPS && remainingTickFraction > MIN_SUBSTEP_FRACTION; substepIndex++) {
            if (profiling) {
                executedSubsteps++;
                GasSolverProfiler.recordDynamicSubstep();
            }
            long endpointPlanningStart = profiling ? System.nanoTime() : 0;
            List<GasNetworkPressureEndpoint> endpoints = endpointPlanner.planPressureEndpoints(level, gas, true);
            int activeEndpointCount = 0;
            for (GasNetworkPressureEndpoint endpoint : endpoints) {
                if (endpoint.access().drainFaces().isEmpty() && endpoint.access().fillFaces().isEmpty()) {
                    continue;
                }

                activeEndpointCount++;
            }
            if (profiling) {
                GasSolverProfiler.recordPressureEndpointPlanning(System.nanoTime() - endpointPlanningStart, activeEndpointCount);
            }
            if (activeEndpointCount == 0) {
                return;
            }

            GasPressureGraphSolution solution = pressureGraph.solve(endpoints, transportBudget, pressureAccumulator.isEnabled());
            if (pressureAccumulator.isEnabled()) {
                long pressureCollectionStart = profiling ? System.nanoTime() : 0;
                int collectedPressureSamples = pressureAccumulator.record(solution.facePressures());
                if (profiling) {
                    GasSolverProfiler.recordPressureCollection(System.nanoTime() - pressureCollectionStart);
                    GasSolverProfiler.recordPressureTelemetrySamples(collectedPressureSamples);
                }
            }
            if (!solution.converged() || solution.isEmpty()) {
                return;
            }

            long transferPlanningStart = profiling ? System.nanoTime() : 0;
            for (SimulationClock clock : clocks) {
                clock.components.clear();
                clock.substepFraction = 0;
            }
            for (TransferComponent component : solution.transferComponents()) {
                SimulationClock clock = clocksByEndpoint.get(component.endpointFlows().getFirst().endpoint().transferIdentity());
                if (clock.remainingTickFraction <= MIN_SUBSTEP_FRACTION) {
                    continue;
                }

                clock.components.add(component);
            }
            List<ComponentPlan> normalPlans = new ArrayList<>();
            List<TransferComponent> quantizedComponents = new ArrayList<>();
            for (SimulationClock clock : clocks) {
                if (clock.components.isEmpty()) {
                    clock.remainingTickFraction = 0;
                    continue;
                }

                GasPressureGraphSolution clockSolution = new GasPressureGraphSolution(clock.components, List.of());
                double fraction = GasEndpointTransferPlanner.calculateSubstepFraction(clockSolution, clock.remainingTickFraction, transportBudget);
                if (fraction <= 0) {
                    clock.remainingTickFraction = 0;
                    continue;
                }

                clock.substepFraction = fraction;
                for (TransferComponent component : clock.components) {
                    GasTransferPlan plan = GasEndpointTransferPlanner.plan(level, gas, component, fraction, substepIndex, integration);
                    if (plan.isEmpty()) {
                        quantizedComponents.add(component);
                        continue;
                    }

                    normalPlans.add(new ComponentPlan(component, plan));
                }
            }
            if (profiling) {
                GasSolverProfiler.recordTransferPlanning(System.nanoTime() - transferPlanningStart);
            }

            if (!quantizationAttempted && !quantizedComponents.isEmpty()) {
                quantizationAttempted = true;
                if (executeQuantizedPlan(level, pressureGraph, gas, endpoints, solution, quantizedComponents, transportBudget, flowAccumulator, integration, substepIndex)) {
                    if (normalPlans.isEmpty()) {
                        return;
                    }

                    continue;
                }
            }
            if (flowAccumulator.hasCollision()) {
                return;
            }

            remainingTickFraction = 0;
            for (SimulationClock clock : clocks) {
                double fraction = clock.substepFraction;
                for (TransferComponent component : clock.components) {
                    integration.advance(component, fraction);
                }
                clock.remainingTickFraction = Math.max(0, clock.remainingTickFraction - fraction);
                remainingTickFraction = Math.max(remainingTickFraction, clock.remainingTickFraction);
            }
            for (ComponentPlan componentPlan : normalPlans) {
                executeComponentPlan(level, gas, componentPlan, transportBudget, flowAccumulator, integration);
                if (!flowAccumulator.hasCollision()) {
                    continue;
                }

                return;
            }

        }
        if (!profiling) {
            return;
        }

        recordSubstepCapIfExhausted(executedSubsteps, remainingTickFraction);
    }

    private static boolean executeQuantizedPlan(Level level, PreparedGraph pressureGraph, GasStack gas, List<GasNetworkPressureEndpoint> endpoints, GasPressureGraphSolution solution, List<TransferComponent> components, GasTransportFlowBudget transportBudget, GasPipeFlowAccumulator flowAccumulator, GasFlowIntegration integration, int substepIndex) {
        int componentCursor = Math.floorMod(level.getGameTime() + substepIndex, components.size());
        for (int componentOffset = 0; componentOffset < components.size(); componentOffset++) {
            TransferComponent component = components.get(Mth.positiveModulo(componentCursor + componentOffset, components.size()));
            GasTransferPlan plan = GasQuantizedEquilibriumPlanner.plan(level, pressureGraph, gas, endpoints, solution, component, transportBudget, substepIndex);
            if (plan.isEmpty()) {
                continue;
            }

            ComponentPlan componentPlan = new ComponentPlan(component, plan);
            return executeComponentPlan(level, gas, componentPlan, transportBudget, flowAccumulator, integration);
        }
        return false;
    }

    private static boolean executeComponentPlan(Level level, GasStack gas, ComponentPlan componentPlan, GasTransportFlowBudget transportBudget, GasPipeFlowAccumulator flowAccumulator, GasFlowIntegration integration) {
        boolean profiling = GasSolverProfiler.isProfilingCurrentNetwork();
        long executionStart = profiling ? System.nanoTime() : 0;
        try {
            GasTransferPlan plan = componentPlan.plan();
            RoutedPlan routed = componentPlan.component().routing().route(plan, transportBudget);
            if (!flowAccumulator.ensureCompatible(level, routed, gas)) {
                return false;
            }

            List<ExecutedRoute> executed = GasRoutedTransferExecutor.execute(level, gas, routed, transportBudget, transfer -> {
                flowAccumulator.recordPath(transfer.route().segments(), gas, transfer.amount());
                integration.record(transfer);
            });
            return !executed.isEmpty();
        }
        finally {
            if (profiling) {
                GasSolverProfiler.recordTransferExecution(System.nanoTime() - executionStart);
            }
        }
    }

    private record ComponentPlan(TransferComponent component, GasTransferPlan plan) {}

    private static final class SimulationClock {
        private final List<TransferComponent> components = new ArrayList<>();
        private double remainingTickFraction = 1;
        private double substepFraction;
    }

    private static final class NetworkSimulationBatch {
        private final Set<BlockPos> processedPipes = new HashSet<>();
        private long gameTime = Long.MIN_VALUE;

        private void begin(long currentGameTime) {
            if (gameTime == currentGameTime) {
                return;
            }

            gameTime = currentGameTime;
            processedPipes.clear();
        }
    }
}
