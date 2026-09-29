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
import net.ty.createcraftedbeginning.gas.storage.CreativeGasReservoir;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.storage.handler.CombinedGasStorageHandler;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import net.ty.createcraftedbeginning.recipe.pressure.PressureRequirement;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasConsumptionPlannerGameTests {
    private static final long VOLUME = 1000;
    private static final long MAX_PRESSURE = GasPressure.pascals(20);

    private GasConsumptionPlannerGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void variablePressureMinimumIsPostConsumptionFloor(GameTestHelper helper) {
        GasTank tank = tankAtPressure(GasPressure.pascals(12));
        GasRecipeRequirement exactFloorDrain = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 2000, PressureRequirement.atLeast(GasPressure.pascals(10)));
        GasRecipeRequirement belowFloorDrain = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 2001, PressureRequirement.atLeast(GasPressure.pascals(10)));

        Optional<GasConsumptionPlan> plan = GasConsumptionPlanner.plan(exactFloorDrain, tank);
        helper.assertTrue(plan.isPresent(), "Planner rejected a drain that ends exactly on the minimum pressure floor");
        helper.assertTrue(GasConsumptionPlanner.plan(belowFloorDrain, tank).isEmpty(), "Planner allowed a drain below the minimum pressure floor");
        if (plan.isEmpty()) {
            return;
        }

        helper.assertTrue(plan.get().execute(), "Pressure-floor consumption plan failed to execute");
        helper.assertValueEqual(tank.getPressurePa(), GasPressure.pascals(10), "Variable tank pressure after floor-limited consumption");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void maximumPressureChecksSourceBeforeConsumption(GameTestHelper helper) {
        GasTank tank = tankAtPressure(GasPressure.pascals(12));
        GasRecipeRequirement requirement = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 500, PressureRequirement.atMost(GasPressure.pascals(10)));

        helper.assertTrue(GasConsumptionPlanner.plan(requirement, tank).isEmpty(), "Maximum pressure ceiling accepted an over-pressure variable source");
        tank.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), 10000)).requireAccepted();
        helper.assertTrue(GasConsumptionPlanner.plan(requirement, tank).isPresent(), "Maximum pressure ceiling rejected its inclusive boundary");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void fixedPressureSourceMustRemainInsideFullRange(GameTestHelper helper) {
        CreativeGasReservoir source = new CreativeGasReservoir(VOLUME, MAX_PRESSURE, GasPressure.pascals(12), () -> {});
        source.setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        GasRecipeRequirement accepted = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 5000, PressureRequirement.between(GasPressure.pascals(10), GasPressure.pascals(15)));
        GasRecipeRequirement rejected = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 1, PressureRequirement.atMost(GasPressure.pascals(10)));

        Optional<GasConsumptionPlan> plan = GasConsumptionPlanner.plan(accepted, source);
        helper.assertTrue(plan.isPresent() && plan.get().execute(), "Fixed-pressure source inside the requested range was rejected");
        helper.assertValueEqual(source.getPressurePa(), GasPressure.pascals(12), "Fixed source pressure changed after recipe consumption");
        helper.assertTrue(GasConsumptionPlanner.plan(rejected, source).isEmpty(), "Fixed-pressure source outside the requested range was accepted");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void multiTankPlannerAllocatesByPressureContract(GameTestHelper helper) {
        GasTank highPressure = tankAtPressure(GasPressure.pascals(12));
        GasTank lowPressure = tankAtPressure(GasPressure.pascals(5));
        CombinedGasStorageHandler storage = new CombinedGasStorageHandler(highPressure, lowPressure);
        GasRecipeRequirement highFloor = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 1000, PressureRequirement.atLeast(GasPressure.pascals(10)));
        GasRecipeRequirement lowCeiling = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 1000, PressureRequirement.atMost(GasPressure.pascals(5)));

        Optional<GasConsumptionPlan> plan = GasConsumptionPlanner.plan(List.of(highFloor, lowCeiling), storage);
        helper.assertTrue(plan.isPresent(), "Multi-tank planner failed to separate incompatible pressure requirements");
        if (plan.isEmpty()) {
            return;
        }

        helper.assertTrue(plan.get().execute(), "Multi-tank pressure-aware plan failed to execute");
        helper.assertValueEqual(highPressure.getPressurePa(), GasPressure.pascals(11), "High-pressure source was not assigned to the minimum-floor requirement");
        helper.assertValueEqual(lowPressure.getPressurePa(), GasPressure.pascals(4), "Low-pressure source was not assigned to the maximum-ceiling requirement");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void sharedTankUsesStrictestAssignedMinimum(GameTestHelper helper) {
        GasTank tank = tankAtPressure(GasPressure.pascals(12));
        CombinedGasStorageHandler storage = new CombinedGasStorageHandler(tank);
        GasRecipeRequirement highFloor = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 1000, PressureRequirement.atLeast(GasPressure.pascals(10)));
        GasRecipeRequirement unrestricted = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 1500);

        helper.assertTrue(GasConsumptionPlanner.plan(List.of(highFloor, unrestricted), storage).isEmpty(), "Planner allowed total consumption from a shared tank to cross the strictest assigned pressure floor");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void planRevalidatesPressureBeforeCommit(GameTestHelper helper) {
        GasTank tank = tankAtPressure(GasPressure.pascals(5));
        GasRecipeRequirement requirement = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 100, PressureRequirement.atMost(GasPressure.pascals(5)));
        GasConsumptionPlan plan = GasConsumptionPlanner.plan(requirement, tank).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'planRevalidatesPressureBeforeCommit'."));

        helper.assertValueEqual(tank.fill(new GasStack(CCBGases.NATURAL_AIR.get(), 1000), GasAction.EXECUTE), 1000L, "Test tank accepted pressure-changing fill");
        helper.assertTrue(!plan.canExecute(), "Consumption plan did not revalidate a changed maximum pressure before commit");
        helper.assertTrue(!plan.execute(), "Stale pressure plan executed after its source exceeded the maximum pressure");
        helper.succeed();
    }

    private static GasTank tankAtPressure(long pressurePa) {
        GasTank tank = new GasTank(VOLUME, MAX_PRESSURE);
        long amount = GasPressure.amount(VOLUME, pressurePa);
        tank.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), amount)).requireAccepted();
        return tank;
    }
}
