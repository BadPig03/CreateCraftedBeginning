package net.ty.createcraftedbeginning.gametests.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandler;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlers;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TeslaTurbinePressureGameTests {
    private static final BlockPos TURBINE_POS = new BlockPos(3, 1, 3);
    private static final BlockPos NOZZLE_POS = new BlockPos(5, 1, 4);

    private TeslaTurbinePressureGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void builtInCurvesPreserveEndpointsAndInterpolatePressure(GameTestHelper helper) {
        List<Gas> gases = List.of(CCBGases.NATURAL_AIR.get(), CCBGases.ENERGIZED_NATURAL_AIR.get(), CCBGases.ULTRAWARM_AIR.get(), CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), CCBGases.ETHEREAL_AIR.get(), CCBGases.ENERGIZED_ETHEREAL_AIR.get(), CCBGases.MOIST_AIR.get(), CCBGases.SPORE_AIR.get(), CCBGases.SCULK_AIR.get(), CCBGases.STEAM.get(), CCBGases.CREATIVE_AIR.get());
        float[] baseLevels = {1, 2, 1.5F, 3, 2, 4, 1, 1, 1, 4, 16};
        float[] maximumLevels = {4, 8, 6, 12, 8, 16, 4, 4, 4, 12, 16};
        for (int index = 0; index < gases.size(); index++) {
            AirtightTurbineHandler handler = AirtightTurbineHandlers.resolve(gases.get(index));
            helper.assertValueEqual(handler.getLevel(Long.MIN_VALUE), baseLevels[index], "Turbine minimum pressure endpoint for " + gases.get(index));
            helper.assertValueEqual(handler.getLevel(GasPressure.pascals(0.4)), baseLevels[index], "Turbine sub-atmospheric pressure endpoint");
            helper.assertValueEqual(handler.getLevel(GasPressure.pascals(1)), baseLevels[index], "Turbine one-atmosphere endpoint");
            helper.assertValueEqual(handler.getLevel(GasPressure.pascals(5.5)), (baseLevels[index] + maximumLevels[index]) / 2, "Turbine pressure midpoint");
            helper.assertValueEqual(handler.getLevel(GasPressure.pascals(10)), maximumLevels[index], "Turbine ten-atmosphere endpoint");
            helper.assertValueEqual(handler.getLevel(Long.MAX_VALUE), maximumLevels[index], "Turbine pressure plateau");
        }

        AirtightTurbineHandler energizedEthereal = AirtightTurbineHandlers.resolve(CCBGases.ENERGIZED_ETHEREAL_AIR.get());
        helper.assertTrue(Math.abs(energizedEthereal.getLevel(GasPressure.pascals(8)) - 13.333333f) < 0.0001F, "Eight-atmosphere gas must interpolate to level 13 1/3");
        helper.assertTrue(Math.abs(energizedEthereal.getLevel(GasPressure.pascals(9.9)) - 15.866667F) < 0.0001F, "Pressure below ten atmospheres must approach the maximum continuously");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 90)
    public static void eachInletClampsPressureBeforeWeightingAndSurvivesReload(GameTestHelper helper) {
        runFlowCase(helper, 8, 512, 12, 512, true, 224, true);
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 90)
    public static void inletLevelsAreWeightedByAcceptedAmount(GameTestHelper helper) {
        runFlowCase(helper, 1, 256, 10, 768, true, 208, false);
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 90)
    public static void opposingInletsLimitSupplyByNetFlow(GameTestHelper helper) {
        runFlowCase(helper, 10, 1024, 1, 512, false, 128, false);
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 90)
    public static void opposingEqualFlowsProduceNoRotation(GameTestHelper helper) {
        runFlowCase(helper, 8, 1024, 12, 1024, false, 0, false);
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 210)
    public static void pressureCrossingAndSupplyLossRetainSamplingWindow(GameTestHelper helper) {
        TeslaTurbineBlockEntity turbine = placeTurbine(helper);
        GasPressureBoundary inlet = (GasPressureBoundary) turbine.createGasHandler(true);
        GasStack gas = new GasStack(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 1024);
        int[] elapsedTicks = {-10};
        helper.onEachTick(() -> {
            int tick = elapsedTicks[0]++;
            if (tick < 0) {
                return;
            }

            if (tick == 60) {
                helper.assertValueEqual(Math.abs(turbine.getGeneratedSpeed()), 240.0F, "Turbine speed at 9.9 atmospheres");
            }
            if (tick == 120) {
                helper.assertValueEqual(Math.abs(turbine.getGeneratedSpeed()), 256.0F, "Turbine maximum speed at ten atmospheres");
            }
            if (tick == 175) {
                helper.assertValueEqual(turbine.getGeneratedSpeed(), 0.0F, "Turbine must stop after its supply window empties");
                helper.succeed();
                return;
            }

            if (tick >= 120) {
                return;
            }

            long pressurePa = GasPressure.pascals(tick < 60 ? 9.9 : 10);
            helper.assertValueEqual(inlet.fillFromPressure(gas, pressurePa, GasAction.EXECUTE), 1024L, "Continuous energized ethereal air acceptance");
        });
    }

    private static TeslaTurbineBlockEntity placeTurbine(GameTestHelper helper) {
        helper.setBlock(TURBINE_POS, CCBBlocks.TESLA_TURBINE_BLOCK.getDefaultState().setValue(TeslaTurbineBlock.AXIS, Axis.Y).setValue(TeslaTurbineBlock.ROTOR, 8));
        TeslaTurbineBlockEntity turbine = helper.getBlockEntity(TURBINE_POS);
        helper.runAtTickTime(5, () -> helper.setBlock(NOZZLE_POS, CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK.getDefaultState().setValue(TeslaTurbineNozzleBlock.FACING, Direction.EAST).setValue(TeslaTurbineNozzleBlock.CLOCKWISE, true)));
        return turbine;
    }

    private static void runFlowCase(GameTestHelper helper, double firstPressure, long firstAmount, double secondPressure, long secondAmount, boolean secondClockwise, float expectedSpeed, boolean reload) {
        TeslaTurbineBlockEntity turbine = placeTurbine(helper);
        GasPressureBoundary firstInlet = (GasPressureBoundary) turbine.createGasHandler(true);
        GasPressureBoundary secondInlet = (GasPressureBoundary) turbine.createGasHandler(secondClockwise);
        GasStack firstGas = new GasStack(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), firstAmount);
        GasStack secondGas = firstGas.copyWithAmount(secondAmount);
        Provider provider = helper.getLevel().registryAccess();
        int[] elapsedTicks = {-10};
        helper.onEachTick(() -> {
            int tick = elapsedTicks[0]++;
            if (tick < 0) {
                return;
            }

            if (tick == 60) {
                helper.assertValueEqual(Math.abs(turbine.getGeneratedSpeed()), expectedSpeed, "Turbine speed after pressure-weighted supply settles");
                helper.succeed();
                return;
            }

            CompoundTag beforeSimulation = turbine.saveWithoutMetadata(provider);
            helper.assertValueEqual(firstInlet.fillFromPressure(firstGas, GasPressure.pascals(firstPressure), GasAction.SIMULATE), firstAmount, "First inlet simulated acceptance");
            helper.assertValueEqual(secondInlet.fillFromPressure(secondGas, GasPressure.pascals(secondPressure), GasAction.SIMULATE), secondAmount, "Second inlet simulated acceptance");
            helper.assertValueEqual(turbine.saveWithoutMetadata(provider), beforeSimulation, "Simulated turbine fill must preserve all persistent state");
            helper.assertValueEqual(firstInlet.fillFromPressure(firstGas, GasPressure.pascals(firstPressure), GasAction.EXECUTE), firstAmount, "First inlet executed acceptance");
            helper.assertValueEqual(secondInlet.fillFromPressure(secondGas, GasPressure.pascals(secondPressure), GasAction.EXECUTE), secondAmount, "Second inlet executed acceptance");
            if (!reload || tick != 27) {
                return;
            }

            CompoundTag saved = turbine.saveWithoutMetadata(provider);
            CompoundTag savedFlow = saved.getCompound("Core").getCompound("FlowMeter").copy();
            helper.assertTrue(savedFlow.getDouble("GatheredWeightedLevels") > 0, "Reload test must retain an unfinished weighted sample");
            turbine.loadWithComponents(saved, provider);
            helper.assertValueEqual(turbine.saveWithoutMetadata(provider).getCompound("Core").getCompound("FlowMeter"), savedFlow, "Reload must preserve pending pressure weights, sample history and timing");
        });
    }
}
