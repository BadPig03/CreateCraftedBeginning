package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.storage.CreativeGasReservoir;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.storage.handler.CombinedGasStorageHandler;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasRecipePressureSpeed;
import net.ty.createcraftedbeginning.recipe.pressure.PressureRequirement;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.NoSuchElementException;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasRecipePressureSpeedGameTests {
    private static final long VOLUME = 1000;
    private static final long MAX_PRESSURE = GasPressure.pascals(20);
    private static final double EPSILON = 0.0001;

    private GasRecipePressureSpeedGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void exactMinimumFloorHasNoPressureBonus(GameTestHelper helper) {
        GasTank tank = tankAtPressure(CCBGases.NATURAL_AIR.get(), GasPressure.pascals(12));
        GasRecipeRequirement requirement = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 2000, PressureRequirement.atLeast(GasPressure.pascals(10)));
        GasConsumptionPlan plan = GasConsumptionPlanner.plan(requirement, tank).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'exactMinimumFloorHasNoPressureBonus'."));

        assertClose(helper, plan.minimumPostConsumptionPressureHeadroomPa(), 0, "Post-consumption headroom at the minimum pressure floor");
        assertClose(helper, GasRecipePressureSpeed.multiplier(plan), 1, "Pressure speed multiplier at the minimum pressure floor");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void unrestrictedRecipeUsesReferencePressureBaseline(GameTestHelper helper) {
        GasTank tank = tankAtPressure(CCBGases.NATURAL_AIR.get(), GasPressure.pascals(8));
        GasRecipeRequirement requirement = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 2000);
        GasConsumptionPlan plan = GasConsumptionPlanner.plan(requirement, tank).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'unrestrictedRecipeUsesReferencePressureBaseline'."));

        assertClose(helper, plan.minimumPostConsumptionPressureHeadroomPa(), GasPressure.pascals(5), "Unrestricted recipe post-consumption headroom");
        assertClose(helper, GasRecipePressureSpeed.multiplier(plan), 1.5, "Pressure speed multiplier at the half-bonus headroom");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void minimumPressureBaselineUsesPostConsumptionHeadroom(GameTestHelper helper) {
        GasTank tank = tankAtPressure(CCBGases.NATURAL_AIR.get(), GasPressure.pascals(17));
        GasRecipeRequirement requirement = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 2000, PressureRequirement.atLeast(GasPressure.pascals(10)));
        GasConsumptionPlan plan = GasConsumptionPlanner.plan(requirement, tank).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'minimumPressureBaselineUsesPostConsumptionHeadroom'."));

        assertClose(helper, plan.minimumPostConsumptionPressureHeadroomPa(), GasPressure.pascals(5), "Minimum-pressure recipe post-consumption headroom");
        assertClose(helper, GasRecipePressureSpeed.multiplier(plan), 1.5, "Minimum-pressure recipe pressure speed multiplier");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void multipleGasSourcesUseSmallestHeadroom(GameTestHelper helper) {
        GasTank naturalAir = tankAtPressure(CCBGases.NATURAL_AIR.get(), GasPressure.pascals(17));
        GasTank steam = tankAtPressure(CCBGases.STEAM.get(), GasPressure.pascals(9));
        CombinedGasStorageHandler storage = new CombinedGasStorageHandler(naturalAir, steam);
        GasRecipeRequirement naturalAirRequirement = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 2000, PressureRequirement.atLeast(GasPressure.pascals(10)));
        GasRecipeRequirement steamRequirement = GasRecipeRequirement.of(CCBGases.STEAM.get(), 2000, PressureRequirement.atLeast(GasPressure.pascals(5)));
        GasConsumptionPlan plan = GasConsumptionPlanner.plan(List.of(naturalAirRequirement, steamRequirement), storage).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'multipleGasSourcesUseSmallestHeadroom'."));

        assertClose(helper, plan.minimumPostConsumptionPressureHeadroomPa(), GasPressure.pascals(2), "Multi-gas bottleneck pressure headroom");
        double expectedMultiplier = 1.2857142857142856;
        assertClose(helper, GasRecipePressureSpeed.multiplier(plan), expectedMultiplier, "Multi-gas bottleneck pressure speed multiplier");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void fixedPressureSourceKeepsItsPressureAfterConsumption(GameTestHelper helper) {
        CreativeGasReservoir source = new CreativeGasReservoir(VOLUME, MAX_PRESSURE, GasPressure.pascals(6), () -> {});
        source.setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        GasRecipeRequirement requirement = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 5000);
        GasConsumptionPlan plan = GasConsumptionPlanner.plan(requirement, source).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'fixedPressureSourceKeepsItsPressureAfterConsumption'."));

        assertClose(helper, plan.minimumPostConsumptionPressureHeadroomPa(), GasPressure.pascals(5), "Fixed-pressure source post-consumption headroom");
        assertClose(helper, GasRecipePressureSpeed.multiplier(plan), 1.5, "Fixed-pressure source pressure speed multiplier");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void emptyPlanHasNoPressureBonus(GameTestHelper helper) {
        GasConsumptionPlan plan = GasConsumptionPlan.empty(0);

        assertClose(helper, plan.minimumPostConsumptionPressureHeadroomPa(), 0, "Empty plan pressure headroom");
        assertClose(helper, GasRecipePressureSpeed.multiplier(plan), 1, "Empty plan pressure speed multiplier");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void pressureSpeedCurveNeverExceedsMaximum(GameTestHelper helper) {
        float veryHighPressureMultiplier = GasRecipePressureSpeed.multiplierForHeadroom(GasPressure.pascals(1000));
        float infinitePressureMultiplier = GasRecipePressureSpeed.multiplierForHeadroom(Double.POSITIVE_INFINITY);

        helper.assertTrue(veryHighPressureMultiplier < GasRecipePressureSpeed.MAXIMUM_MULTIPLIER, "Finite pressure headroom reached or exceeded the asymptotic maximum");
        assertClose(helper, infinitePressureMultiplier, GasRecipePressureSpeed.MAXIMUM_MULTIPLIER, "Infinite pressure headroom maximum multiplier");
        helper.succeed();
    }

    private static GasTank tankAtPressure(Gas gas, long pressurePa) {
        GasTank tank = new GasTank(VOLUME, MAX_PRESSURE);
        long amount = GasPressure.amount(VOLUME, pressurePa);
        tank.tryReplaceContents(new GasStack(gas, amount)).requireAccepted();
        return tank;
    }

    private static void assertClose(GameTestHelper helper, double actual, double expected, String description) {
        helper.assertTrue(Math.abs(actual - expected) <= EPSILON, description + ": expected " + expected + ", got " + actual);
    }
}
