package net.ty.createcraftedbeginning.gametests.content.breezes.breezechamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberConversionPlanner;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberConversionPlanner.GasConversionPlan;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberRecipeIndex.GasConversion;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.NoSuchElementException;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BreezeChamberConversionGameTests {
    private BreezeChamberConversionGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void conversionThroughputFallsWithOutputBackpressure(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CCBBlocks.BREEZE_CHAMBER_BLOCK.getDefaultState());
        BreezeChamberBlockEntity chamber = helper.getBlockEntity(pos);
        if (chamber == null) {
            throw new NullPointerException("Expected a breeze chamber at " + pos + '.');
        }

        BreezeChamberConversionPlanner planner = new BreezeChamberConversionPlanner(chamber);
        GasTank output = chamber.getTankBehaviourInternal().getPrimaryHandler();
        long ambientPressure = AtmosphereStateResolver.resolvePressurePa(helper.getLevel(), helper.absolutePos(pos));
        long ambientAmount = GasPressure.amount(output.getVolume(), ambientPressure);
        int[] fillPercentages = {0, 25, 50, 75, 100};
        int[] expectedRates = {2500, 1875, 1250, 625, 0};
        for (int index = 0; index < fillPercentages.length; index++) {
            output.tryReplaceContents(new GasStack(CCBGases.ENERGIZED_NATURAL_AIR.get(), ambientAmount * fillPercentages[index] / 100)).requireAccepted();
            helper.assertValueEqual(planner.getProcessingAmount(1), expectedRates[index], "Conversion budget at output backpressure");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void conversionReservesAmbientHeadroomWithoutMutation(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CCBBlocks.BREEZE_CHAMBER_BLOCK.getDefaultState());
        BreezeChamberBlockEntity chamber = helper.getBlockEntity(pos);
        BreezeChamberConversionPlanner planner = new BreezeChamberConversionPlanner(chamber);
        GasTank input = new GasTank(10000);
        GasTank output = new GasTank(10000);
        long pressure = AtmosphereStateResolver.resolvePressurePa(helper.getLevel(), helper.absolutePos(pos));
        long capacity = Math.min(output.getMaxAmount(), GasPressure.amount(output.getVolume(), pressure));
        long initialOutput = capacity - 250;
        input.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), 1000));
        output.tryReplaceContents(new GasStack(CCBGases.ENERGIZED_NATURAL_AIR.get(), initialOutput));
        GasConversion conversion = new GasConversion(GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 100), new GasStack(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100));

        GasConversionPlan plan = planner.planConversion(conversion, input, output, 1000).orElseThrow(() -> new NoSuchElementException("Expected a valid processing plan in test 'conversionReservesAmbientHeadroomWithoutMutation'."));
        helper.assertValueEqual(plan.batchCount(), 2L, "Output limited conversion batches");
        helper.assertValueEqual(input.getStoredAmount(), 1000L, "Input after planning");
        helper.assertValueEqual(output.getStoredAmount(), initialOutput, "Output after planning");
        helper.assertTrue(planner.planConversion(conversion, input, output, 99).isEmpty(), "Insufficient work budget produced a batch");
        output.tryReplaceContents(new GasStack(CCBGases.ULTRAWARM_AIR.get(), 100));
        helper.assertTrue(planner.planConversion(conversion, input, output, 1000).isEmpty(), "Different output gas was accepted");
        helper.succeed();
    }
}
