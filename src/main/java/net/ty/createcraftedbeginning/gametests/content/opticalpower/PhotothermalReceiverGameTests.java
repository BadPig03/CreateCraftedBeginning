package net.ty.createcraftedbeginning.gametests.content.opticalpower;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver.PhotothermalReceiverBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver.PhotothermalReceiverPort;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PhotothermalReceiverGameTests {
    private PhotothermalReceiverGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 530)
    public static void combinedLightHeatsBasinRecipesAndStopsAfterCooling(GameTestHelper helper) {
        BlockPos receiverPos = new BlockPos(1, 1, 1);
        BlockPos basinPos = receiverPos.above();
        helper.setBlock(receiverPos, CCBBlocks.PHOTOTHERMAL_RECEIVER_BLOCK.getDefaultState());
        helper.setBlock(basinPos, AllBlocks.BASIN.getDefaultState());
        PhotothermalReceiverBlockEntity receiver = helper.getBlockEntity(receiverPos);
        BasinBlockEntity basin = helper.getBlockEntity(basinPos);
        MixingRecipe heated = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/photothermal_heated")).require(Items.IRON_INGOT).requiresHeat(HeatCondition.HEATED).output(Items.GOLD_INGOT).build();
        MixingRecipe superheated = new Builder<>(MixingRecipe::new, CCBAPI.asResource("test/photothermal_superheated")).require(Items.IRON_INGOT).requiresHeat(HeatCondition.SUPERHEATED).output(Items.DIAMOND).build();
        int[] ticks = {0};
        helper.onEachTick(() -> {
            ticks[0]++;
            int tick = ticks[0];
            if (tick < 380) {
                int power = 8;
                if (tick >= 150) {
                    power = 16;
                }
                if (tick >= 300) {
                    power = 24;
                }

                receiver.receiveLaser(receiver.getBlockPos().west(), PhotothermalReceiverPort.WEST, power);
                receiver.receiveLaser(receiver.getBlockPos().west(), PhotothermalReceiverPort.WEST, power);
                receiver.receiveLaser(receiver.getBlockPos().east(), PhotothermalReceiverPort.EAST, power);
            }

            if (tick == 130) {
                helper.assertTrue(BasinBlockEntity.getHeatLevelOf(helper.getBlockState(receiverPos)) == HeatLevel.KINDLED, "Combined light did not heat the basin, or duplicate beams were counted twice.");
                basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
                helper.assertTrue(!BasinRecipe.match(basin, superheated), "Ordinary heating accepted a superheated recipe.");
                helper.assertTrue(BasinRecipe.apply(basin, heated), "Heated mixing recipe did not process above the receiver.");
                helper.assertTrue(basin.getInputInventory().getStackInSlot(0).isEmpty(), "Heated recipe did not consume its ingredient.");
            }

            if (tick == 270) {
                helper.assertTrue(BasinBlockEntity.getHeatLevelOf(helper.getBlockState(receiverPos)) == HeatLevel.KINDLED, "Heat above 100 percent enabled superheating before 150 percent.");
                basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
                helper.assertTrue(!BasinRecipe.match(basin, superheated), "Basin accepted a superheated recipe before 150 percent heat.");
            }

            if (tick == 370) {
                helper.assertTrue(BasinBlockEntity.getHeatLevelOf(helper.getBlockState(receiverPos)) == HeatLevel.SEETHING, "Receiver did not supply superheating to the basin.");
                basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
                helper.assertTrue(BasinRecipe.apply(basin, superheated), "Superheated mixing recipe did not process above the receiver.");
            }

            if (tick < 500) {
                return;
            }

            helper.assertTrue(BasinBlockEntity.getHeatLevelOf(helper.getBlockState(receiverPos)) == HeatLevel.NONE, "Basin retained heat after laser input and stored heat ended.");
            basin.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
            helper.assertTrue(!BasinRecipe.match(basin, heated), "Unheated basin continued accepting heated recipes.");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 480)
    public static void laserRayHeatsBoilerAndOcclusionExhaustsReserve(GameTestHelper helper) {
        BlockPos receiverPos = new BlockPos(4, 1, 4);
        BlockPos emitterPos = new BlockPos(1, 1, 4);
        BlockPos tankPos = receiverPos.above();
        helper.setBlock(receiverPos, CCBBlocks.PHOTOTHERMAL_RECEIVER_BLOCK.getDefaultState());
        helper.setBlock(emitterPos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.EAST));
        helper.setBlock(tankPos, AllBlocks.FLUID_TANK.getDefaultState());
        helper.setBlock(tankPos.above(), AllBlocks.STEAM_ENGINE.getDefaultState());
        LaserEmitterBlockEntity emitter = helper.getBlockEntity(emitterPos);
        FluidTankBlockEntity tank = helper.getBlockEntity(tankPos);
        int[] ticks = {0};
        helper.onEachTick(() -> {
            ticks[0]++;
            emitter.applyOpticalPowerAllocation(48);
            emitter.tick();
            if (ticks[0] == 330) {
                helper.assertTrue(tank.boiler.isActive(), "Steam engine did not activate the boiler for the heat update test.");
                helper.assertTrue(tank.boiler.activeHeat == 2, "Laser-heated receiver did not contribute two heat levels to the boiler.");
                helper.assertTrue(BoilerHeater.findHeat(helper.getLevel(), helper.absolutePos(receiverPos), helper.getBlockState(receiverPos)) == 2, "Photothermal receiver was not registered as a boiler heater.");
                helper.setBlock(emitterPos.east(), Blocks.STONE.defaultBlockState());
            }

            if (ticks[0] < 450) {
                return;
            }

            helper.assertTrue(tank.boiler.activeHeat == 0 && !tank.boiler.passiveHeat, "Blocked laser continued heating the boiler after its reserve expired.");
            helper.succeed();
        });
    }
}
