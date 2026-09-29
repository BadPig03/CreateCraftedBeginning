package net.ty.createcraftedbeginning.gas.network.solver;

import com.sun.management.ThreadMXBean;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasSolverProfiler {
    public static final String ENABLE_PROPERTY = "createcraftedbeginning.gas_solver_profiler";
    public static final String WINDOW_PROPERTY = "createcraftedbeginning.gas_solver_profiler_window";

    private static final boolean LOGGING_ENABLED = Boolean.getBoolean(ENABLE_PROPERTY);
    private static final int LOG_WINDOW_SIZE = Math.max(1, Integer.getInteger(WINDOW_PROPERTY, 200));
    private static final ThreadLocal<ProfileBuilder> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Capture> ACTIVE_CAPTURE = new ThreadLocal<>();
    private static final List<NetworkProfile> LOG_WINDOW = new ArrayList<>();
    private static final ThreadMXBean ALLOCATION_BEAN = allocationBean();

    private GasSolverProfiler() {
    }

    public static boolean beginNetwork(BlockPos startPos) {
        if (!LOGGING_ENABLED && ACTIVE_CAPTURE.get() == null || CURRENT.get() != null) {
            return false;
        }

        CURRENT.set(new ProfileBuilder(startPos, System.nanoTime()));
        return true;
    }

    public static void finishNetwork() {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        CURRENT.remove();
        NetworkProfile profile = builder.finish(System.nanoTime());
        Capture capture = ACTIVE_CAPTURE.get();
        if (capture != null) {
            capture.samples.add(profile);
        }
        if (!LOGGING_ENABLED) {
            return;
        }

        publishToLogWindow(profile);
    }

    public static boolean isProfilingCurrentNetwork() {
        return CURRENT.get() != null;
    }

    public static void recordTopology(long elapsedNanos, int pipeCount, int endpointFaceCount) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.stageTimings.topologyNanos += elapsedNanos;
        builder.shape.pipeCount = Math.max(builder.shape.pipeCount, pipeCount);
        builder.shape.endpointFaceCount = Math.max(builder.shape.endpointFaceCount, endpointFaceCount);
    }

    public static void recordTopologyCacheHit() {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.topologyCache.hits++;
    }

    public static void recordTopologyCacheMiss() {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.topologyCache.misses++;
    }

    public static void recordEndpointPlanning(long elapsedNanos, int gasGroupCount) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.stageTimings.endpointPlanningNanos += elapsedNanos;
        builder.shape.gasGroupCount = Math.max(builder.shape.gasGroupCount, gasGroupCount);
    }

    public static void recordPressureEndpointPlanning(long elapsedNanos, int endpointCount) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.stageTimings.endpointPlanningNanos += elapsedNanos;
        builder.shape.pressureEndpointCount = Math.max(builder.shape.pressureEndpointCount, endpointCount);
    }

    public static void recordGraphPreparation(long elapsedNanos) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.stageTimings.graphPreparationNanos += elapsedNanos;
    }

    public static void recordGraphSetup(long elapsedNanos) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.stageTimings.graphSetupNanos += elapsedNanos;
    }

    public static void recordGasSolve() {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.simulation.gasSolveCount++;
    }

    public static void recordDynamicSubstep() {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.simulation.dynamicSubsteps++;
    }

    public static void recordSubstepCapHit(double remainingTickFraction) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.simulation.substepCapHitCount++;
        builder.simulation.maxSubstepCapRemainingTickFraction = Math.max(builder.simulation.maxSubstepCapRemainingTickFraction, remainingTickFraction);
    }

    public static void recordGraphSolve(long elapsedNanos, int nodeCount, int edgeCount, boolean hypothetical, boolean converged, int activeSetIterations, int forestDirectSolveAttempts, int forestDirectSolveSuccesses, int conjugateGradientIterations, int biConjugateGradientStabilizedIterations, int sorSweeps) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.stageTimings.graphSolveNanos += elapsedNanos;
        builder.shape.nodeCount = Math.max(builder.shape.nodeCount, nodeCount);
        builder.shape.edgeCount = Math.max(builder.shape.edgeCount, edgeCount);
        builder.graph.solveCount++;
        if (hypothetical) {
            builder.graph.hypotheticalSolveCount++;
        }
        if (!converged) {
            builder.graph.nonConvergedSolveCount++;
        }
        builder.graph.activeSetIterations += activeSetIterations;
        builder.graph.forestDirectSolveAttempts += forestDirectSolveAttempts;
        builder.graph.forestDirectSolveSuccesses += forestDirectSolveSuccesses;
        builder.graph.conjugateGradientIterations += conjugateGradientIterations;
        builder.graph.biConjugateGradientStabilizedIterations += biConjugateGradientStabilizedIterations;
        builder.graph.sorSweeps += sorSweeps;
    }

    public static void recordGraphPostSolve(long warmStartCopyNanos, long activeComponentsNanos, long endpointFlowsNanos, long facePressuresNanos, long edgeFlowsNanos, long materializationNanos) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.graphPostSolveTimings.warmStartCopyNanos += warmStartCopyNanos;
        builder.graphPostSolveTimings.activeComponentsNanos += activeComponentsNanos;
        builder.graphPostSolveTimings.endpointFlowsNanos += endpointFlowsNanos;
        builder.graphPostSolveTimings.facePressuresNanos += facePressuresNanos;
        builder.graphPostSolveTimings.edgeFlowsNanos += edgeFlowsNanos;
        builder.graphPostSolveTimings.materializationNanos += materializationNanos;
    }

    public static void recordRegularizationWork(int solves, int iterations) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.regularizedSolves += solves;
        builder.regularizedIterations += iterations;
    }

    public static void recordPressureCollection(long elapsedNanos) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.stageTimings.pressureCollectionNanos += elapsedNanos;
    }

    public static void recordTelemetryTargets(int pressureTargetCount, int flowTargetCount) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.telemetry.pressureTargetCount = Math.max(builder.telemetry.pressureTargetCount, Math.max(0, pressureTargetCount));
        builder.telemetry.flowTargetCount = Math.max(builder.telemetry.flowTargetCount, Math.max(0, flowTargetCount));
    }

    public static void recordPressureTelemetrySamples(int sampleCount) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.telemetry.pressureFaceSampleCount += Math.max(0, sampleCount);
    }

    public static void recordFlowTelemetryUpdate() {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.telemetry.flowTargetUpdateCount++;
    }

    public static void recordTransferPlanning(long elapsedNanos) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.stageTimings.transferPlanningNanos += elapsedNanos;
    }

    public static void recordTransferExecution(long elapsedNanos) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.stageTimings.transferExecutionNanos += elapsedNanos;
    }

    public static void recordTelemetry(long elapsedNanos) {
        ProfileBuilder builder = CURRENT.get();
        if (builder == null) {
            return;
        }

        builder.stageTimings.telemetryNanos += elapsedNanos;
    }

    public static Capture capture() {
        if (ACTIVE_CAPTURE.get() != null) {
            throw new IllegalStateException("A gas solver profiler capture is already active on this thread.");
        }

        Capture capture = new Capture(Thread.currentThread());
        ACTIVE_CAPTURE.set(capture);
        return capture;
    }

    public static Summary summarize(List<NetworkProfile> samples) {
        if (samples.isEmpty()) {
            return Summary.empty();
        }

        TimingAccumulator timingAccumulator = new TimingAccumulator(samples.size());
        NetworkShape maxShape = NetworkShape.EMPTY;
        TopologyCacheStats topologyCache = TopologyCacheStats.EMPTY;
        SimulationAccumulator simulationAccumulator = new SimulationAccumulator();
        TelemetryAccumulator telemetryAccumulator = new TelemetryAccumulator();
        GraphAccumulator graphAccumulator = new GraphAccumulator();
        for (int index = 0; index < samples.size(); index++) {
            NetworkProfile sample = samples.get(index);
            timingAccumulator.add(index, sample.timings());
            maxShape = maxShape.max(sample.shape());
            topologyCache = topologyCache.plus(sample.topologyCache());
            simulationAccumulator.add(sample.simulation());
            telemetryAccumulator.add(sample.telemetry());
            graphAccumulator.add(sample.graph());
        }
        return new Summary(samples.size(), timingAccumulator.finish(), maxShape, topologyCache, simulationAccumulator.finish(samples.size()), telemetryAccumulator.finish(samples.size()), graphAccumulator.finish(samples.size()));
    }

    public static void logSummary(String label, List<NetworkProfile> samples) {
        Summary summary = summarize(samples);
        if (summary.sampleCount() == 0) {
            CCBAPI.LOGGER.info("Gas solver profile [{}]: no samples.", label);
            return;
        }

        TimingSummary timings = summary.timings();
        AverageStageTimings stages = timings.averageStages();
        AverageGraphPostSolveTimings postSolve = timings.averageGraphPostSolve();
        NetworkShape shape = summary.maxShape();
        TopologyCacheStats topologyCache = summary.topologyCache();
        SimulationSummary simulation = summary.simulation();
        TelemetrySummary telemetry = summary.telemetry();
        GraphSummary graph = summary.graph();
        CCBAPI.LOGGER.info("Gas solver profile [{}]: samples={}, totalAvg={} ms, totalP95={} ms, totalMax={} ms, maxPipes={}, maxNodes={}, maxEdges={}, maxEndpointFaces={}, maxPressureEndpoints={}, maxGases={}", label, summary.sampleCount(), formatMs(timings.averageTotalNanos()), formatMs(timings.p95TotalNanos()), formatMs(timings.maxTotalNanos()), shape.pipeCount(), shape.nodeCount(), shape.edgeCount(), shape.endpointFaceCount(), shape.pressureEndpointCount(), shape.gasGroupCount());
        double attributedNanos = stages.totalNanos() + postSolve.totalNanos();
        double unattributedNanos = Math.max(0, timings.averageTotalNanos() - attributedNanos);
        CCBAPI.LOGGER.info("Gas solver profile [{}] stages avg: topology={} ms, endpoints={} ms, graphPrepare={} ms, graphSetup={} ms, graphSolve={} ms, pressureCollect={} ms, transferPlan={} ms, transferExecute={} ms, telemetry={} ms, unattributed={} ms; topologyCacheHitRate={}%, topologyCacheHits={}, topologyCacheQueries={}", label, formatMs(stages.topologyNanos()), formatMs(stages.endpointPlanningNanos()), formatMs(stages.graphPreparationNanos()), formatMs(stages.graphSetupNanos()), formatMs(stages.graphSolveNanos()), formatMs(stages.pressureCollectionNanos()), formatMs(stages.transferPlanningNanos()), formatMs(stages.transferExecutionNanos()), formatMs(stages.telemetryNanos()), formatMs(unattributedNanos), String.format(Locale.ROOT, "%.1f", topologyCache.hitRate() * 100.0), topologyCache.hits(), topologyCache.queries());
        CCBAPI.LOGGER.info("Gas solver profile [{}] postSolve avg: warmStartCopy={} ms, activeComponents={} ms, endpointFlows={} ms, facePressures={} ms, edgeFlows={} ms, materialize={} ms", label, formatMs(postSolve.warmStartCopyNanos()), formatMs(postSolve.activeComponentsNanos()), formatMs(postSolve.endpointFlowsNanos()), formatMs(postSolve.facePressuresNanos()), formatMs(postSolve.edgeFlowsNanos()), formatMs(postSolve.materializationNanos()));
        CCBAPI.LOGGER.info("Gas solver profile [{}] work: gasSolvesAvg={}, substepsPerSampleAvg={}, substepsPerGasSolveAvg={}, graphSolvesAvg={}, hypotheticalGraphSolvesAvg={}, activeSetIterationsAvg={}, forestDirectSuccessesAvg={}, forestDirectAttemptsAvg={}, cgIterationsAvg={}, bicgstabIterationsAvg={}, sorSweepsAvg={}, nonConvergedSolvesTotal={}", label, formatCount(summary.averageGasSolveCount()), formatCount(simulation.averageDynamicSubsteps()), formatCount(simulation.averageDynamicSubstepsPerGasSolve()), formatCount(graph.averageSolveCount()), formatCount(graph.averageHypotheticalSolveCount()), formatCount(graph.averageActiveSetIterations()), formatCount(graph.averageForestDirectSolveSuccesses()), formatCount(graph.averageForestDirectSolveAttempts()), formatCount(graph.averageConjugateGradientIterations()), formatCount(graph.averageBiConjugateGradientStabilizedIterations()), formatCount(graph.averageSorSweeps()), graph.nonConvergedSolveCount());
        CCBAPI.LOGGER.info("Gas solver profile [{}] telemetry avg: pressureTargets={}, pressureFaceSamples={}, flowTargets={}, flowTargetUpdates={}", label, formatCount(telemetry.averagePressureTargetCount()), formatCount(telemetry.averagePressureFaceSampleCount()), formatCount(telemetry.averageFlowTargetCount()), formatCount(telemetry.averageFlowTargetUpdateCount()));
        CCBAPI.LOGGER.info("Gas solver profile [{}] substepCap: hitsTotal={}, gasSolvesTotal={}, hitRate={}%, affectedSamples={}, samples={}, affectedSampleRate={}%, maxRemainingTick={}%", label, simulation.substepCapHitCount(), simulation.gasSolveCount(), formatPercent(summary.substepCapHitRate()), simulation.substepCapAffectedSampleCount(), summary.sampleCount(), formatPercent(summary.substepCapAffectedSampleRate()), formatPercent(simulation.maxSubstepCapRemainingTickFraction()));
        int worstSubstepCapProfileIndex = findWorstSubstepCapProfileIndex(samples);
        if (worstSubstepCapProfileIndex < 0) {
            return;
        }

        NetworkProfile worst = samples.get(worstSubstepCapProfileIndex);
        CCBAPI.LOGGER.info("Gas solver profile [{}] substepCap worst: startPos={}, hits={}, gasSolves={}, maxRemainingTick={}%, maxPipes={}, maxEndpointFaces={}, maxPressureEndpoints={}, maxGases={}, maxNodes={}, maxEdges={}", label, worst.startPos(), worst.simulation().substepCapHitCount(), worst.simulation().gasSolveCount(), formatPercent(worst.simulation().maxSubstepCapRemainingTickFraction()), worst.shape().pipeCount(), worst.shape().endpointFaceCount(), worst.shape().pressureEndpointCount(), worst.shape().gasGroupCount(), worst.shape().nodeCount(), worst.shape().edgeCount());
    }

    @Nullable
    private static ThreadMXBean allocationBean() {
        if (!Boolean.getBoolean("createcraftedbeginning.gas_solver_allocations")) {
            return null;
        }

        if (ManagementFactory.getThreadMXBean() instanceof ThreadMXBean bean && bean.isThreadAllocatedMemorySupported()) {
            if (!bean.isThreadAllocatedMemoryEnabled()) {
                bean.setThreadAllocatedMemoryEnabled(true);
            }
            return bean;
        }

        return null;
    }

    private static synchronized void publishToLogWindow(NetworkProfile profile) {
        LOG_WINDOW.add(profile);
        if (LOG_WINDOW.size() < LOG_WINDOW_SIZE) {
            return;
        }

        List<NetworkProfile> samples = List.copyOf(LOG_WINDOW);
        LOG_WINDOW.clear();
        logSummary("rolling-" + LOG_WINDOW_SIZE, samples);
    }

    private static String formatMs(double nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1000000.0);
    }

    private static String formatCount(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatPercent(double fraction) {
        return String.format(Locale.ROOT, "%.2f", fraction * 100.0);
    }

    private static int findWorstSubstepCapProfileIndex(List<NetworkProfile> samples) {
        int worstIndex = -1;
        for (int index = 0; index < samples.size(); index++) {
            NetworkProfile sample = samples.get(index);
            if (sample.simulation().substepCapHitCount() <= 0) {
                continue;
            }

            if (worstIndex < 0) {
                worstIndex = index;
                continue;
            }

            NetworkProfile worst = samples.get(worstIndex);
            if (sample.simulation().maxSubstepCapRemainingTickFraction() <= worst.simulation().maxSubstepCapRemainingTickFraction() && (sample.simulation().maxSubstepCapRemainingTickFraction() != worst.simulation().maxSubstepCapRemainingTickFraction() || sample.simulation().substepCapHitCount() <= worst.simulation().substepCapHitCount())) {
                continue;
            }

            worstIndex = index;
        }
        return worstIndex;
    }

    public static final class Capture implements AutoCloseable {
        private final Thread ownerThread;
        private final List<NetworkProfile> samples = new ArrayList<>();
        private boolean closed;

        private Capture(Thread ownerThread) {
            this.ownerThread = ownerThread;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            if (Thread.currentThread() != ownerThread) {
                throw new IllegalStateException("Gas solver profiler capture must be closed on the thread that opened it.");
            }

            if (ACTIVE_CAPTURE.get() != this) {
                throw new IllegalStateException("Gas solver profiler capture ownership was lost before close.");
            }

            ACTIVE_CAPTURE.remove();
            closed = true;
        }

        @Contract(pure = true)
        public @Unmodifiable List<NetworkProfile> samples() {
            return List.copyOf(samples);
        }
    }

    public record NetworkProfile(BlockPos startPos, TimingProfile timings, NetworkShape shape, TopologyCacheStats topologyCache, SimulationWork simulation, TelemetryWork telemetry, GraphWork graph, Diagnostics diagnostics) {}

    public record Diagnostics(long allocatedBytes, int regularizedSolves, int regularizedIterations) {}

    public record TimingProfile(long totalNanos, StageTimings stages, GraphPostSolveTimings graphPostSolve) {}

    public record StageTimings(long topologyNanos, long endpointPlanningNanos, long graphPreparationNanos, long graphSetupNanos, long graphSolveNanos, long pressureCollectionNanos, long transferPlanningNanos, long transferExecutionNanos, long telemetryNanos) {}

    public record GraphPostSolveTimings(long warmStartCopyNanos, long activeComponentsNanos, long endpointFlowsNanos, long facePressuresNanos, long edgeFlowsNanos, long materializationNanos) {}

    public record NetworkShape(int pipeCount, int endpointFaceCount, int gasGroupCount, int pressureEndpointCount, int nodeCount, int edgeCount) {
        private static final NetworkShape EMPTY = new NetworkShape(0, 0, 0, 0, 0, 0);

        private NetworkShape max(NetworkShape other) {
            return new NetworkShape(Math.max(pipeCount, other.pipeCount), Math.max(endpointFaceCount, other.endpointFaceCount), Math.max(gasGroupCount, other.gasGroupCount), Math.max(pressureEndpointCount, other.pressureEndpointCount), Math.max(nodeCount, other.nodeCount), Math.max(edgeCount, other.edgeCount));
        }
    }

    public record TopologyCacheStats(long hits, long misses) {
        private static final TopologyCacheStats EMPTY = new TopologyCacheStats(0, 0);

        @Contract(pure = true)
        public long queries() {
            return hits + misses;
        }

        @Contract(pure = true)
        public double hitRate() {
            long queries = queries();
            if (queries == 0) {
                return 0;
            }

            return (double) hits / queries;
        }

        private TopologyCacheStats plus(TopologyCacheStats other) {
            return new TopologyCacheStats(hits + other.hits, misses + other.misses);
        }
    }

    public record SimulationWork(int gasSolveCount, int dynamicSubsteps, int substepCapHitCount, double maxSubstepCapRemainingTickFraction) {}

    public record TelemetryWork(int pressureTargetCount, int pressureFaceSampleCount, int flowTargetCount, int flowTargetUpdateCount) {}

    public record GraphWork(int solveCount, int hypotheticalSolveCount, int nonConvergedSolveCount, int activeSetIterations, int forestDirectSolveAttempts, int forestDirectSolveSuccesses, int conjugateGradientIterations, int biConjugateGradientStabilizedIterations, int sorSweeps) {}

    public record Summary(int sampleCount, TimingSummary timings, NetworkShape maxShape, TopologyCacheStats topologyCache, SimulationSummary simulation, TelemetrySummary telemetry, GraphSummary graph) {
        private static Summary empty() {
            return new Summary(0, TimingSummary.EMPTY, NetworkShape.EMPTY, TopologyCacheStats.EMPTY, SimulationSummary.EMPTY, TelemetrySummary.EMPTY, GraphSummary.EMPTY);
        }

        @Contract(pure = true)
        public double averageGasSolveCount() {
            if (sampleCount == 0) {
                return 0;
            }

            return (double) simulation.gasSolveCount() / sampleCount;
        }

        @Contract(pure = true)
        public double substepCapHitRate() {
            if (simulation.gasSolveCount() == 0) {
                return 0;
            }

            return (double) simulation.substepCapHitCount() / simulation.gasSolveCount();
        }

        @Contract(pure = true)
        public double substepCapAffectedSampleRate() {
            if (sampleCount == 0) {
                return 0;
            }

            return (double) simulation.substepCapAffectedSampleCount() / sampleCount;
        }
    }

    public record TimingSummary(double averageTotalNanos, long p95TotalNanos, long maxTotalNanos, AverageStageTimings averageStages, AverageGraphPostSolveTimings averageGraphPostSolve) {
        private static final TimingSummary EMPTY = new TimingSummary(0, 0, 0, AverageStageTimings.EMPTY, AverageGraphPostSolveTimings.EMPTY);
    }

    public record AverageStageTimings(double topologyNanos, double endpointPlanningNanos, double graphPreparationNanos, double graphSetupNanos, double graphSolveNanos, double pressureCollectionNanos, double transferPlanningNanos, double transferExecutionNanos, double telemetryNanos) {
        private static final AverageStageTimings EMPTY = new AverageStageTimings(0, 0, 0, 0, 0, 0, 0, 0, 0);

        private double totalNanos() {
            return topologyNanos + endpointPlanningNanos + graphPreparationNanos + graphSetupNanos + graphSolveNanos + pressureCollectionNanos + transferPlanningNanos + transferExecutionNanos + telemetryNanos;
        }
    }

    public record AverageGraphPostSolveTimings(double warmStartCopyNanos, double activeComponentsNanos, double endpointFlowsNanos, double facePressuresNanos, double edgeFlowsNanos, double materializationNanos) {
        private static final AverageGraphPostSolveTimings EMPTY = new AverageGraphPostSolveTimings(0, 0, 0, 0, 0, 0);

        private double totalNanos() {
            return warmStartCopyNanos + activeComponentsNanos + endpointFlowsNanos + facePressuresNanos + edgeFlowsNanos + materializationNanos;
        }
    }

    public record SimulationSummary(long gasSolveCount, double averageDynamicSubsteps, double averageDynamicSubstepsPerGasSolve, long substepCapHitCount, int substepCapAffectedSampleCount, double maxSubstepCapRemainingTickFraction) {
        private static final SimulationSummary EMPTY = new SimulationSummary(0, 0, 0, 0, 0, 0);
    }

    public record TelemetrySummary(double averagePressureTargetCount, double averagePressureFaceSampleCount, double averageFlowTargetCount, double averageFlowTargetUpdateCount) {
        private static final TelemetrySummary EMPTY = new TelemetrySummary(0, 0, 0, 0);
    }

    public record GraphSummary(double averageSolveCount, double averageHypotheticalSolveCount, long nonConvergedSolveCount, double averageActiveSetIterations, double averageForestDirectSolveAttempts, double averageForestDirectSolveSuccesses, double averageConjugateGradientIterations, double averageBiConjugateGradientStabilizedIterations, double averageSorSweeps) {
        private static final GraphSummary EMPTY = new GraphSummary(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static final class ProfileBuilder {
        private final BlockPos startPos;
        private final long startNanos;
        private final long startAllocatedBytes = allocatedBytes();
        private final MutableStageTimings stageTimings = new MutableStageTimings();
        private final MutableGraphPostSolveTimings graphPostSolveTimings = new MutableGraphPostSolveTimings();
        private final MutableNetworkShape shape = new MutableNetworkShape();
        private final MutableTopologyCacheStats topologyCache = new MutableTopologyCacheStats();
        private final MutableSimulationWork simulation = new MutableSimulationWork();
        private final MutableTelemetryWork telemetry = new MutableTelemetryWork();
        private final MutableGraphWork graph = new MutableGraphWork();
        private int regularizedSolves;
        private int regularizedIterations;

        private ProfileBuilder(BlockPos startPos, long startNanos) {
            this.startPos = startPos.immutable();
            this.startNanos = startNanos;
        }

        private static long allocatedBytes() {
            if (ALLOCATION_BEAN == null) {
                return -1;
            }

            return ALLOCATION_BEAN.getThreadAllocatedBytes(Thread.currentThread().threadId());
        }

        private NetworkProfile finish(long endNanos) {
            long endAllocatedBytes = allocatedBytes();
            long allocation = startAllocatedBytes < 0 || endAllocatedBytes < 0 ? -1 : Math.max(0, endAllocatedBytes - startAllocatedBytes);
            TimingProfile timings = new TimingProfile(Math.max(0, endNanos - startNanos), stageTimings.freeze(), graphPostSolveTimings.freeze());
            return new NetworkProfile(startPos, timings, shape.freeze(), topologyCache.freeze(), simulation.freeze(), telemetry.freeze(), graph.freeze(), new Diagnostics(allocation, regularizedSolves, regularizedIterations));
        }
    }

    private static final class MutableStageTimings {
        private long topologyNanos;
        private long endpointPlanningNanos;
        private long graphPreparationNanos;
        private long graphSetupNanos;
        private long graphSolveNanos;
        private long pressureCollectionNanos;
        private long transferPlanningNanos;
        private long transferExecutionNanos;
        private long telemetryNanos;

        private StageTimings freeze() {
            return new StageTimings(topologyNanos, endpointPlanningNanos, graphPreparationNanos, graphSetupNanos, graphSolveNanos, pressureCollectionNanos, transferPlanningNanos, transferExecutionNanos, telemetryNanos);
        }
    }

    private static final class MutableGraphPostSolveTimings {
        private long warmStartCopyNanos;
        private long activeComponentsNanos;
        private long endpointFlowsNanos;
        private long facePressuresNanos;
        private long edgeFlowsNanos;
        private long materializationNanos;

        private GraphPostSolveTimings freeze() {
            return new GraphPostSolveTimings(warmStartCopyNanos, activeComponentsNanos, endpointFlowsNanos, facePressuresNanos, edgeFlowsNanos, materializationNanos);
        }
    }

    private static final class MutableNetworkShape {
        private int pipeCount;
        private int endpointFaceCount;
        private int gasGroupCount;
        private int pressureEndpointCount;
        private int nodeCount;
        private int edgeCount;

        private NetworkShape freeze() {
            return new NetworkShape(pipeCount, endpointFaceCount, gasGroupCount, pressureEndpointCount, nodeCount, edgeCount);
        }
    }

    private static final class MutableTopologyCacheStats {
        private int hits;
        private int misses;

        private TopologyCacheStats freeze() {
            return new TopologyCacheStats(hits, misses);
        }
    }

    private static final class MutableSimulationWork {
        private int gasSolveCount;
        private int dynamicSubsteps;
        private int substepCapHitCount;
        private double maxSubstepCapRemainingTickFraction;

        private SimulationWork freeze() {
            return new SimulationWork(gasSolveCount, dynamicSubsteps, substepCapHitCount, maxSubstepCapRemainingTickFraction);
        }
    }

    private static final class MutableTelemetryWork {
        private int pressureTargetCount;
        private int pressureFaceSampleCount;
        private int flowTargetCount;
        private int flowTargetUpdateCount;

        private TelemetryWork freeze() {
            return new TelemetryWork(pressureTargetCount, pressureFaceSampleCount, flowTargetCount, flowTargetUpdateCount);
        }
    }

    private static final class MutableGraphWork {
        private int solveCount;
        private int hypotheticalSolveCount;
        private int nonConvergedSolveCount;
        private int activeSetIterations;
        private int forestDirectSolveAttempts;
        private int forestDirectSolveSuccesses;
        private int conjugateGradientIterations;
        private int biConjugateGradientStabilizedIterations;
        private int sorSweeps;

        private GraphWork freeze() {
            return new GraphWork(solveCount, hypotheticalSolveCount, nonConvergedSolveCount, activeSetIterations, forestDirectSolveAttempts, forestDirectSolveSuccesses, conjugateGradientIterations, biConjugateGradientStabilizedIterations, sorSweeps);
        }
    }

    private static final class TimingAccumulator {
        private final long[] totalTimes;
        private final StageTimingAccumulator stages = new StageTimingAccumulator();
        private final GraphPostSolveTimingAccumulator graphPostSolve = new GraphPostSolveTimingAccumulator();
        private long totalNanos;

        private TimingAccumulator(int sampleCount) {
            totalTimes = new long[sampleCount];
        }

        private void add(int index, TimingProfile timings) {
            totalTimes[index] = timings.totalNanos();
            totalNanos += timings.totalNanos();
            stages.add(timings.stages());
            graphPostSolve.add(timings.graphPostSolve());
        }

        private TimingSummary finish() {
            Arrays.sort(totalTimes);
            int sampleCount = totalTimes.length;
            int p95Index = Math.min(sampleCount - 1, Mth.ceil(sampleCount * 0.95) - 1);
            return new TimingSummary((double) totalNanos / sampleCount, totalTimes[p95Index], totalTimes[sampleCount - 1], stages.average(sampleCount), graphPostSolve.average(sampleCount));
        }
    }

    private static final class StageTimingAccumulator {
        private long topologyNanos;
        private long endpointPlanningNanos;
        private long graphPreparationNanos;
        private long graphSetupNanos;
        private long graphSolveNanos;
        private long pressureCollectionNanos;
        private long transferPlanningNanos;
        private long transferExecutionNanos;
        private long telemetryNanos;

        private void add(StageTimings timings) {
            topologyNanos += timings.topologyNanos();
            endpointPlanningNanos += timings.endpointPlanningNanos();
            graphPreparationNanos += timings.graphPreparationNanos();
            graphSetupNanos += timings.graphSetupNanos();
            graphSolveNanos += timings.graphSolveNanos();
            pressureCollectionNanos += timings.pressureCollectionNanos();
            transferPlanningNanos += timings.transferPlanningNanos();
            transferExecutionNanos += timings.transferExecutionNanos();
            telemetryNanos += timings.telemetryNanos();
        }

        private AverageStageTimings average(int sampleCount) {
            return new AverageStageTimings((double) topologyNanos / sampleCount, (double) endpointPlanningNanos / sampleCount, (double) graphPreparationNanos / sampleCount, (double) graphSetupNanos / sampleCount, (double) graphSolveNanos / sampleCount, (double) pressureCollectionNanos / sampleCount, (double) transferPlanningNanos / sampleCount, (double) transferExecutionNanos / sampleCount, (double) telemetryNanos / sampleCount);
        }
    }

    private static final class GraphPostSolveTimingAccumulator {
        private long warmStartCopyNanos;
        private long activeComponentsNanos;
        private long endpointFlowsNanos;
        private long facePressuresNanos;
        private long edgeFlowsNanos;
        private long materializationNanos;

        private void add(GraphPostSolveTimings timings) {
            warmStartCopyNanos += timings.warmStartCopyNanos();
            activeComponentsNanos += timings.activeComponentsNanos();
            endpointFlowsNanos += timings.endpointFlowsNanos();
            facePressuresNanos += timings.facePressuresNanos();
            edgeFlowsNanos += timings.edgeFlowsNanos();
            materializationNanos += timings.materializationNanos();
        }

        private AverageGraphPostSolveTimings average(int sampleCount) {
            return new AverageGraphPostSolveTimings((double) warmStartCopyNanos / sampleCount, (double) activeComponentsNanos / sampleCount, (double) endpointFlowsNanos / sampleCount, (double) facePressuresNanos / sampleCount, (double) edgeFlowsNanos / sampleCount, (double) materializationNanos / sampleCount);
        }
    }

    private static final class SimulationAccumulator {
        private long gasSolveCount;
        private long dynamicSubsteps;
        private long substepCapHitCount;
        private int substepCapAffectedSampleCount;
        private double maxSubstepCapRemainingTickFraction;

        private void add(SimulationWork work) {
            gasSolveCount += work.gasSolveCount();
            dynamicSubsteps += work.dynamicSubsteps();
            substepCapHitCount += work.substepCapHitCount();
            if (work.substepCapHitCount() > 0) {
                substepCapAffectedSampleCount++;
            }
            maxSubstepCapRemainingTickFraction = Math.max(maxSubstepCapRemainingTickFraction, work.maxSubstepCapRemainingTickFraction());
        }

        private SimulationSummary finish(int sampleCount) {
            double averageDynamicSubstepsPerGasSolve = gasSolveCount == 0 ? 0 : (double) dynamicSubsteps / gasSolveCount;
            return new SimulationSummary(gasSolveCount, (double) dynamicSubsteps / sampleCount, averageDynamicSubstepsPerGasSolve, substepCapHitCount, substepCapAffectedSampleCount, maxSubstepCapRemainingTickFraction);
        }
    }

    private static final class TelemetryAccumulator {
        private long pressureTargetCount;
        private long pressureFaceSampleCount;
        private long flowTargetCount;
        private long flowTargetUpdateCount;

        private void add(TelemetryWork work) {
            pressureTargetCount += work.pressureTargetCount();
            pressureFaceSampleCount += work.pressureFaceSampleCount();
            flowTargetCount += work.flowTargetCount();
            flowTargetUpdateCount += work.flowTargetUpdateCount();
        }

        private TelemetrySummary finish(int sampleCount) {
            return new TelemetrySummary((double) pressureTargetCount / sampleCount, (double) pressureFaceSampleCount / sampleCount, (double) flowTargetCount / sampleCount, (double) flowTargetUpdateCount / sampleCount);
        }
    }

    private static final class GraphAccumulator {
        private long solveCount;
        private long hypotheticalSolveCount;
        private long nonConvergedSolveCount;
        private long activeSetIterations;
        private long forestDirectSolveAttempts;
        private long forestDirectSolveSuccesses;
        private long conjugateGradientIterations;
        private long biConjugateGradientStabilizedIterations;
        private long sorSweeps;

        private void add(GraphWork work) {
            solveCount += work.solveCount();
            hypotheticalSolveCount += work.hypotheticalSolveCount();
            nonConvergedSolveCount += work.nonConvergedSolveCount();
            activeSetIterations += work.activeSetIterations();
            forestDirectSolveAttempts += work.forestDirectSolveAttempts();
            forestDirectSolveSuccesses += work.forestDirectSolveSuccesses();
            conjugateGradientIterations += work.conjugateGradientIterations();
            biConjugateGradientStabilizedIterations += work.biConjugateGradientStabilizedIterations();
            sorSweeps += work.sorSweeps();
        }

        private GraphSummary finish(int sampleCount) {
            return new GraphSummary((double) solveCount / sampleCount, (double) hypotheticalSolveCount / sampleCount, nonConvergedSolveCount, (double) activeSetIterations / sampleCount, (double) forestDirectSolveAttempts / sampleCount, (double) forestDirectSolveSuccesses / sampleCount, (double) conjugateGradientIterations / sampleCount, (double) biConjugateGradientStabilizedIterations / sampleCount, (double) sorSweeps / sampleCount);
        }
    }
}
