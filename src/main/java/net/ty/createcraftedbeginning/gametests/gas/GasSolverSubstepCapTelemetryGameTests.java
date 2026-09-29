package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkSimulator;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler.Capture;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler.Summary;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasSolverSubstepCapTelemetryGameTests {
    private static final int SUBSTEP_CAP = 16;
    private static final double EPSILON = 1.0E-12;

    private GasSolverSubstepCapTelemetryGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void belowSubstepCapDoesNotCountAsCapHit(GameTestHelper helper) {
        Summary summary = captureSummary(new int[]{SUBSTEP_CAP - 1}, new double[]{0.5});

        helper.assertValueEqual(summary.simulation().gasSolveCount(), 1L, "below-cap gas solve count");
        helper.assertTrue(closeTo(summary.simulation().averageDynamicSubstepsPerGasSolve(), SUBSTEP_CAP - 1), "Below-cap telemetry did not retain the executed substep count");
        assertNoCapHit(helper, summary, "below-cap solve");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void sixteenthSubstepCompletingTickDoesNotCountAsCapHit(GameTestHelper helper) {
        Summary summary = captureSummary(new int[]{SUBSTEP_CAP}, new double[]{1.0E-6});

        helper.assertValueEqual(summary.simulation().gasSolveCount(), 1L, "exact-cap gas solve count");
        helper.assertTrue(closeTo(summary.simulation().averageDynamicSubstepsPerGasSolve(), SUBSTEP_CAP), "Exact-cap completion did not retain all sixteen executed substeps");
        assertNoCapHit(helper, summary, "sixteenth substep completing the tick");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void sixteenthSubstepWithRemainingTickCountsAsCapHit(GameTestHelper helper) {
        double remainingTickFraction = 0.25;
        Summary summary = captureSummary(new int[]{SUBSTEP_CAP}, new double[]{remainingTickFraction});

        helper.assertValueEqual(summary.simulation().gasSolveCount(), 1L, "exhausted-cap gas solve count");
        helper.assertValueEqual(summary.simulation().substepCapHitCount(), 1L, "exhausted-cap hit count");
        helper.assertValueEqual(summary.simulation().substepCapAffectedSampleCount(), 1, "exhausted-cap affected sample count");
        helper.assertTrue(closeTo(summary.substepCapHitRate(), 1), "Exhausted-cap hit rate was not 100%");
        helper.assertTrue(closeTo(summary.substepCapAffectedSampleRate(), 1), "Exhausted-cap affected-sample rate was not 100%");
        helper.assertTrue(closeTo(summary.simulation().maxSubstepCapRemainingTickFraction(), remainingTickFraction), "Exhausted-cap remaining tick fraction was not preserved");
        helper.assertValueEqual(summary.graph().nonConvergedSolveCount(), 0L, "substep-cap telemetry leaked into graph non-convergence telemetry");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void substepCapTelemetryStressAggregationTracksRatesAndWorstRemaining(GameTestHelper helper) {
        Capture capture = GasSolverProfiler.capture();
        try (capture) {
            for (int sampleIndex = 0; sampleIndex < 64; sampleIndex++) {
                double cappedRemaining = Mth.isMultipleOf(sampleIndex, 2) ? 0.10 + sampleIndex % 8 * 0.01 : 0;
                recordNetworkProfile(new BlockPos(sampleIndex, 0, 0), new int[]{4, 4, 4, SUBSTEP_CAP}, new double[]{0.8, 0.6, 0.4, cappedRemaining});
            }
        }

        Summary summary = GasSolverProfiler.summarize(capture.samples());
        helper.assertValueEqual(summary.sampleCount(), 64, "stress telemetry sample count");
        helper.assertValueEqual(summary.simulation().gasSolveCount(), 256L, "stress telemetry gas solve count");
        helper.assertValueEqual(summary.simulation().substepCapHitCount(), 32L, "stress telemetry cap-hit count");
        helper.assertValueEqual(summary.simulation().substepCapAffectedSampleCount(), 32, "stress telemetry affected-sample count");
        helper.assertTrue(closeTo(summary.simulation().averageDynamicSubstepsPerGasSolve(), 7), "Stress telemetry average substeps per gas solve changed");
        helper.assertTrue(closeTo(summary.substepCapHitRate(), 0.125), "Stress telemetry cap-hit rate changed");
        helper.assertTrue(closeTo(summary.substepCapAffectedSampleRate(), 0.5), "Stress telemetry affected-sample rate changed");
        helper.assertTrue(closeTo(summary.simulation().maxSubstepCapRemainingTickFraction(), 0.16), "Stress telemetry did not retain the worst remaining tick fraction");
        helper.assertValueEqual(summary.graph().nonConvergedSolveCount(), 0L, "Stress cap telemetry was conflated with graph non-convergence");
        helper.succeed();
    }

    private static Summary captureSummary(int[] substepsPerGasSolve, double[] remainingTickFractions) {
        Capture capture = GasSolverProfiler.capture();
        try (capture) {
            recordNetworkProfile(BlockPos.ZERO, substepsPerGasSolve, remainingTickFractions);
        }
        return GasSolverProfiler.summarize(capture.samples());
    }

    private static void recordNetworkProfile(BlockPos startPos, int[] substepsPerGasSolve, double[] remainingTickFractions) {
        if (substepsPerGasSolve.length != remainingTickFractions.length) {
            throw new IllegalArgumentException("Substep and remaining-fraction arrays must have the same length; got " + substepsPerGasSolve.length + " substeps and " + remainingTickFractions.length + " fractions.");
        }

        if (!GasSolverProfiler.beginNetwork(startPos)) {
            throw new IllegalStateException("Failed to begin synthetic gas solver profiler sample.");
        }

        try {
            for (int gasIndex = 0; gasIndex < substepsPerGasSolve.length; gasIndex++) {
                int executedSubsteps = substepsPerGasSolve[gasIndex];
                GasSolverProfiler.recordGasSolve();
                for (int substep = 0; substep < executedSubsteps; substep++) {
                    GasSolverProfiler.recordDynamicSubstep();
                }
                GasNetworkSimulator.recordSubstepCapIfExhausted(executedSubsteps, remainingTickFractions[gasIndex]);
            }
        }
        finally {
            GasSolverProfiler.finishNetwork();
        }
    }

    private static void assertNoCapHit(GameTestHelper helper, Summary summary, String scenario) {
        helper.assertValueEqual(summary.simulation().substepCapHitCount(), 0L, scenario + " cap-hit count");
        helper.assertValueEqual(summary.simulation().substepCapAffectedSampleCount(), 0, scenario + " affected sample count");
        helper.assertTrue(closeTo(summary.substepCapHitRate(), 0), scenario + " reported a non-zero cap-hit rate");
        helper.assertTrue(closeTo(summary.substepCapAffectedSampleRate(), 0), scenario + " reported a non-zero affected-sample rate");
        helper.assertTrue(closeTo(summary.simulation().maxSubstepCapRemainingTickFraction(), 0), scenario + " retained a non-zero worst remaining fraction");
    }

    private static boolean closeTo(double actual, double expected) {
        return Math.abs(actual - expected) <= EPSILON;
    }
}
