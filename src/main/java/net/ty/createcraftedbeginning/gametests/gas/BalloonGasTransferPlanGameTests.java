package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasTransferPlan;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasTransferPlan.ExecutionResult;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasTransferPlan.TransferPolicy;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BalloonGasTransferPlanGameTests {
    private static final long TANK_VOLUME = 10000;
    private static final long MAX_PRESSURE = GasPressure.pascals(16);
    private static final long LOW_PRESSURE = GasPressure.pascals(0.4);

    private BalloonGasTransferPlanGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void oneAtmBalloonAtLowPressureSeparatesPackagerAndUnpackagerPolicies(GameTestHelper helper) {
        GasTank packagerTarget = new GasTank(TANK_VOLUME, MAX_PRESSURE);
        BalloonGasTransferPlan fullOnly = BalloonGasTransferPlan.plan(packagerTarget, naturalAir(10000), LOW_PRESSURE, TransferPolicy.FULL_ONLY);

        helper.assertTrue(!fullOnly.acceptsEntireBalloon(), "FULL_ONLY accepted a 1 atm balloon that only fits partially at 0.4 atm");
        helper.assertValueEqual(fullOnly.acceptedAmount(), 0L, "FULL_ONLY planned a partial transfer");
        helper.assertValueEqual(fullOnly.remainingAmount(), 10000L, "FULL_ONLY rejected balloon remainder");
        ExecutionResult rejectedExecution = fullOnly.execute();
        helper.assertValueEqual(rejectedExecution.transferredAmount(), 0L, "FULL_ONLY executed a rejected transfer");
        helper.assertTrue(!rejectedExecution.complete(), "FULL_ONLY reported a rejected transfer as complete");
        helper.assertValueEqual(packagerTarget.getStoredAmount(), 0L, "FULL_ONLY mutated the target after rejection");

        GasTank unpackagerTarget = new GasTank(TANK_VOLUME, MAX_PRESSURE);
        BalloonGasTransferPlan bestEffort = BalloonGasTransferPlan.plan(unpackagerTarget, naturalAir(10000), LOW_PRESSURE, TransferPolicy.BEST_EFFORT);
        helper.assertValueEqual(bestEffort.acceptedAmount(), 4000L, "BEST_EFFORT amount accepted at 0.4 atm");
        helper.assertValueEqual(bestEffort.remainingAmount(), 6000L, "BEST_EFFORT balloon remainder at 0.4 atm");

        ExecutionResult execution = bestEffort.execute();
        helper.assertValueEqual(execution.transferredAmount(), 4000L, "BEST_EFFORT executed transfer amount");
        helper.assertValueEqual(unpackagerTarget.getStoredAmount(), 4000L, "BEST_EFFORT target amount");
        helper.assertValueEqual(unpackagerTarget.getPressurePa(), LOW_PRESSURE, "BEST_EFFORT target pressure");
        helper.assertValueEqual(unpackagerTarget.getStoredAmount() + bestEffort.remainingAmount(), 10000L, "BEST_EFFORT gas conservation");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void bestEffortAccountsForExistingTargetPressure(GameTestHelper helper) {
        GasTank target = new GasTank(TANK_VOLUME, MAX_PRESSURE);
        helper.assertValueEqual(target.fill(naturalAir(2000), GasAction.EXECUTE), 2000L, "test target prefill amount");

        BalloonGasTransferPlan plan = BalloonGasTransferPlan.plan(target, naturalAir(10000), LOW_PRESSURE, TransferPolicy.BEST_EFFORT);
        helper.assertValueEqual(plan.acceptedAmount(), 2000L, "BEST_EFFORT amount remaining before pressure equilibrium");

        ExecutionResult execution = plan.execute();
        helper.assertValueEqual(execution.transferredAmount(), 2000L, "BEST_EFFORT pressure-limited transfer amount");
        helper.assertValueEqual(target.getStoredAmount(), 4000L, "prefilled target amount at equilibrium");
        helper.assertValueEqual(target.getPressurePa(), LOW_PRESSURE, "prefilled target pressure at equilibrium");
        helper.assertValueEqual(target.getStoredAmount() + plan.remainingAmount(), 12000L, "prefilled target plus balloon gas conservation");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void lowPressureBalloonFullyUnpacksAtOneAtmosphere(GameTestHelper helper) {
        GasTank target = new GasTank(TANK_VOLUME, MAX_PRESSURE);
        BalloonGasTransferPlan plan = BalloonGasTransferPlan.plan(target, naturalAir(4000), GasPressure.REFERENCE_PRESSURE_PA, TransferPolicy.FULL_ONLY);

        helper.assertTrue(plan.acceptsEntireBalloon(), "FULL_ONLY rejected a 0.4 atm standard balloon at 1 atm");
        ExecutionResult execution = plan.execute();
        helper.assertTrue(execution.complete(), "FULL_ONLY did not complete a 4000 GU transfer at 1 atm");
        helper.assertValueEqual(execution.transferredAmount(), 4000L, "FULL_ONLY transfer amount from 0.4 atm packaging");
        helper.assertValueEqual(target.getStoredAmount(), 4000L, "target amount after 0.4 atm to 1 atm unpack");
        helper.assertValueEqual(target.getPressurePa(), LOW_PRESSURE, "target pressure after receiving 4000 GU");
        helper.assertValueEqual(plan.remainingAmount(), 0L, "FULL_ONLY remainder after 0.4 atm to 1 atm transfer");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void vacuumRejectsBothTransferPolicies(GameTestHelper helper) {
        GasTank fullOnlyTarget = new GasTank(TANK_VOLUME, MAX_PRESSURE);
        BalloonGasTransferPlan fullOnly = BalloonGasTransferPlan.plan(fullOnlyTarget, naturalAir(10000), GasPressure.VACUUM_PA, TransferPolicy.FULL_ONLY);
        helper.assertValueEqual(fullOnly.acceptedAmount(), 0L, "FULL_ONLY accepted gas in vacuum");
        helper.assertValueEqual(fullOnly.remainingAmount(), 10000L, "FULL_ONLY vacuum remainder");
        helper.assertValueEqual(fullOnly.execute().transferredAmount(), 0L, "FULL_ONLY transferred gas in vacuum");
        helper.assertValueEqual(fullOnlyTarget.getStoredAmount(), 0L, "FULL_ONLY mutated a target in vacuum");

        GasTank bestEffortTarget = new GasTank(TANK_VOLUME, MAX_PRESSURE);
        BalloonGasTransferPlan bestEffort = BalloonGasTransferPlan.plan(bestEffortTarget, naturalAir(10000), GasPressure.VACUUM_PA, TransferPolicy.BEST_EFFORT);
        helper.assertValueEqual(bestEffort.acceptedAmount(), 0L, "BEST_EFFORT accepted gas in vacuum");
        helper.assertValueEqual(bestEffort.remainingAmount(), 10000L, "BEST_EFFORT vacuum remainder");
        helper.assertValueEqual(bestEffort.execute().transferredAmount(), 0L, "BEST_EFFORT transferred gas in vacuum");
        helper.assertValueEqual(bestEffortTarget.getStoredAmount(), 0L, "BEST_EFFORT mutated a target in vacuum");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void fullOnlyRechecksTargetStateAtCommit(GameTestHelper helper) {
        GasTank target = new GasTank(TANK_VOLUME, MAX_PRESSURE);
        BalloonGasTransferPlan plan = BalloonGasTransferPlan.plan(target, naturalAir(10000), GasPressure.REFERENCE_PRESSURE_PA, TransferPolicy.FULL_ONLY);
        helper.assertTrue(plan.acceptsEntireBalloon(), "FULL_ONLY setup did not initially accept the whole balloon");

        helper.assertValueEqual(target.fill(naturalAir(1000), GasAction.EXECUTE), 1000L, "target mutation before FULL_ONLY commit");
        ExecutionResult execution = plan.execute();
        helper.assertValueEqual(execution.transferredAmount(), 0L, "FULL_ONLY partially committed after target state changed");
        helper.assertTrue(!execution.complete(), "FULL_ONLY reported completion after target state changed");
        helper.assertValueEqual(target.getStoredAmount(), 1000L, "FULL_ONLY modified the target after atomic commit rejection");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void fullOnlyAcceptsWholeBalloonAtOneAtmosphere(GameTestHelper helper) {
        GasTank target = new GasTank(TANK_VOLUME, MAX_PRESSURE);
        BalloonGasTransferPlan plan = BalloonGasTransferPlan.plan(target, naturalAir(10000), GasPressure.REFERENCE_PRESSURE_PA, TransferPolicy.FULL_ONLY);

        helper.assertTrue(plan.acceptsEntireBalloon(), "FULL_ONLY rejected a whole balloon that fits at 1 atm");
        ExecutionResult execution = plan.execute();
        helper.assertTrue(execution.complete(), "FULL_ONLY did not report a complete transfer");
        helper.assertValueEqual(execution.transferredAmount(), 10000L, "FULL_ONLY executed amount at 1 atm");
        helper.assertValueEqual(target.getStoredAmount(), 10000L, "target amount after full atomic unpack");
        helper.succeed();
    }

    private static GasStack naturalAir(long amount) {
        return new GasStack(CCBGases.NATURAL_AIR.get(), amount);
    }
}
