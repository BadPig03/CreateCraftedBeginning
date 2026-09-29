package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.gas.overpressure.OverpressureState;
import net.ty.createcraftedbeginning.gas.overpressure.OverpressureStressCalculator;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OverpressureBehaviourGameTests {
    private static final double EPSILON = 1.0E-9;

    private OverpressureBehaviourGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void stressCurveUsesSixteenToTwentyFourAtmosphereRange(GameTestHelper helper) {
        assertClose(helper, OverpressureStressCalculator.normalizedOverpressure(GasPressureLimits.SAFE_PRESSURE_PA), 0.0, "16 atm normalized stress");
        assertClose(helper, OverpressureStressCalculator.normalizedOverpressure(GasPressure.pascals(20)), 0.5, "20 atm normalized stress");
        assertClose(helper, OverpressureStressCalculator.normalizedOverpressure(GasPressureLimits.HARD_PRESSURE_PA), 1.0, "24 atm normalized stress");
        assertClose(helper, OverpressureStressCalculator.stressIncreasePerTick(GasPressure.pascals(20)), 0.0025, "20 atm per-tick stress");
        assertClose(helper, OverpressureStressCalculator.stressIncreasePerTick(GasPressureLimits.HARD_PRESSURE_PA), 0.01, "24 atm per-tick stress");
        assertClose(helper, OverpressureStressCalculator.stressIncreasePerTick(GasPressure.pascals(30)), 0.01, "pressure above hard limit");
        helper.assertTrue(OverpressureStressCalculator.stressIncreasePerTick(GasPressureLimits.SAFE_PRESSURE_PA + 1) > 0.0, "Pressure above 16 atm did not begin accumulating stress");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void stabilizationGraceFreezesStressBeforeFiveSecondHardLimitFailure(GameTestHelper helper) {
        OverpressureState state = new OverpressureState();
        for (int tick = 0; tick < OverpressureStressCalculator.STABILIZATION_GRACE_TICKS; tick++) {
            state.tick(GasPressureLimits.HARD_PRESSURE_PA);
        }
        assertClose(helper, state.getStress(), 0.0, "stress during stabilization grace");
        helper.assertTrue(!state.isStabilizing(), "Stabilization grace did not expire after 40 ticks");

        for (int tick = 0; tick < OverpressureStressCalculator.HARD_LIMIT_FAILURE_TICKS - 1; tick++) {
            state.tick(GasPressureLimits.HARD_PRESSURE_PA);
        }
        helper.assertTrue(!state.isAtFailureThreshold(), "24 atm reached failure before five active seconds");

        state.tick(GasPressureLimits.HARD_PRESSURE_PA);
        assertClose(helper, state.getStress(), 1.0, "stress after five active seconds at 24 atm");
        helper.assertTrue(state.isFailureReady(), "Full stress was not failure-ready after stabilization ended");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void exactSafePressureRecoversFullStressInFifteenSeconds(GameTestHelper helper) {
        OverpressureState state = fullyStressedState();
        for (int tick = 0; tick < OverpressureStressCalculator.FULL_RECOVERY_TICKS - 1; tick++) {
            state.tick(GasPressureLimits.SAFE_PRESSURE_PA);
        }
        helper.assertTrue(state.getStress() > 0.0, "Stress recovered completely before fifteen seconds elapsed");

        state.tick(GasPressureLimits.SAFE_PRESSURE_PA);
        assertClose(helper, state.getStress(), 0.0, "stress after fifteen safe seconds");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void loadPreservesStressAndRestartsGraceWithoutFreeRecovery(GameTestHelper helper) {
        OverpressureState original = new OverpressureState();
        exhaustGrace(original);
        for (int tick = 0; tick < OverpressureStressCalculator.HARD_LIMIT_FAILURE_TICKS / 2; tick++) {
            original.tick(GasPressureLimits.HARD_PRESSURE_PA);
        }
        assertClose(helper, original.getStress(), 0.5, "pre-save stress");

        CompoundTag saved = new CompoundTag();
        original.writeStress(saved);
        OverpressureState loaded = new OverpressureState();
        loaded.readStress(saved);
        loaded.restartStabilizationGrace();
        assertClose(helper, loaded.getStress(), 0.5, "loaded stress");

        for (int tick = 0; tick < OverpressureStressCalculator.STABILIZATION_GRACE_TICKS; tick++) {
            loaded.tick(GasPressure.VACUUM_PA);
        }
        assertClose(helper, loaded.getStress(), 0.5, "stress after safe-pressure load grace");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void lightAndSevereOverpressureFollowQuadraticTiming(GameTestHelper helper) {
        OverpressureState eighteenAtm = new OverpressureState();
        exhaustGrace(eighteenAtm);
        for (int tick = 0; tick < 1600; tick++) {
            eighteenAtm.tick(GasPressure.pascals(18));
        }
        assertClose(helper, eighteenAtm.getStress(), 1.0, "18 atm failure time");

        OverpressureState twentyTwoAtm = new OverpressureState();
        exhaustGrace(twentyTwoAtm);
        for (int tick = 0; tick < 177; tick++) {
            twentyTwoAtm.tick(GasPressure.pascals(22));
        }
        helper.assertTrue(!twentyTwoAtm.isAtFailureThreshold(), "22 atm reached full stress before the quadratic curve expected");
        twentyTwoAtm.tick(GasPressure.pascals(22));
        helper.assertTrue(twentyTwoAtm.isAtFailureThreshold(), "22 atm did not reach full stress on the expected next tick");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void safePressureCannotBecomeFailureReadyAtEndOfLoadGrace(GameTestHelper helper) {
        CompoundTag saved = new CompoundTag();
        saved.putDouble(OverpressureState.COMPOUND_KEY_STRESS, 1.0);

        OverpressureState loaded = new OverpressureState();
        loaded.readStress(saved);
        loaded.restartStabilizationGrace();
        for (int tick = 0; tick < OverpressureStressCalculator.STABILIZATION_GRACE_TICKS; tick++) {
            loaded.tick(GasPressureLimits.SAFE_PRESSURE_PA);
        }

        helper.assertTrue(!loaded.isFailureReady(), "Safe pressure became failure-ready when stabilization grace ended");
        loaded.tick(GasPressureLimits.SAFE_PRESSURE_PA);
        helper.assertTrue(loaded.getStress() < 1.0, "Stress did not begin recovering after safe-pressure stabilization grace");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void serializedStressIsSanitizedIntoUnitInterval(GameTestHelper helper) {
        CompoundTag oversized = new CompoundTag();
        oversized.putDouble(OverpressureState.COMPOUND_KEY_STRESS, 2.0);
        OverpressureState clamped = new OverpressureState();
        clamped.readStress(oversized);
        assertClose(helper, clamped.getStress(), 1.0, "oversized saved stress");

        CompoundTag invalid = new CompoundTag();
        invalid.putDouble(OverpressureState.COMPOUND_KEY_STRESS, Double.NaN);
        OverpressureState reset = new OverpressureState();
        reset.readStress(invalid);
        assertClose(helper, reset.getStress(), 0.0, "invalid saved stress");
        helper.succeed();
    }

    private static OverpressureState fullyStressedState() {
        OverpressureState state = new OverpressureState();
        exhaustGrace(state);
        for (int tick = 0; tick < OverpressureStressCalculator.HARD_LIMIT_FAILURE_TICKS; tick++) {
            state.tick(GasPressureLimits.HARD_PRESSURE_PA);
        }
        return state;
    }

    private static void exhaustGrace(OverpressureState state) {
        for (int tick = 0; tick < OverpressureStressCalculator.STABILIZATION_GRACE_TICKS; tick++) {
            state.tick(GasPressure.VACUUM_PA);
        }
    }

    private static void assertClose(GameTestHelper helper, double actual, double expected, String label) {
        helper.assertTrue(Math.abs(actual - expected) <= EPSILON, label + ": expected " + expected + ", got " + actual);
    }
}
