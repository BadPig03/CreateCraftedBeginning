package net.ty.createcraftedbeginning.gametests.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkSimulator;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReactorKettlePressureTransferGameTests {
    private static final BlockPos KETTLE = new BlockPos(2, 1, 3);
    private static final BlockPos PIPE = new BlockPos(4, 0, 3);

    private ReactorKettlePressureTransferGameTests() {
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 160)
    public static void reactorInputReachesExactAtmosphericBalance(GameTestHelper helper) {
        assertAtmosphericBalance(helper, false);
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 160)
    public static void reactorOutputDrainsToExactAtmosphericBalance(GameTestHelper helper) {
        assertAtmosphericBalance(helper, true);
    }

    private static void assertAtmosphericBalance(GameTestHelper helper, boolean output) {
        helper.setBlock(KETTLE, CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.getDefaultState());
        helper.runAfterDelay(2, () -> {
            AirtightReactorKettleBlockEntity kettle = helper.getBlockEntity(KETTLE);
            SmartGasTankBehaviour storage = output ? kettle.getOutputGasTank() : kettle.getInputGasTank();
            long target = storage.getCapability().getTankVolume(0);
            setAmount(storage, target + (output ? 64 : -64));
            helper.setBlock(PIPE, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(RotatedPillarBlock.AXIS, Axis.X));
            GasStorageHandler port = (GasStorageHandler) kettle.getGasPortCapability();
            helper.assertTrue(helper.getLevel().getCapability(GasCapabilities.BLOCK, helper.absolutePos(PIPE.west()), Direction.EAST) == port, "Pipe is not connected to the reactor gas port");
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(PIPE.east())).isAir(), "Pipe atmospheric outlet is obstructed");
            int[] stableTicks = {0};
            helper.succeedWhen(() -> {
                GasNetworkSimulator.simulateTick(helper.getLevel(), helper.absolutePos(PIPE));
                long amount = storage.getCapability().getGasInTank(0).getAmount();
                helper.assertTrue(output ? amount >= target : amount <= target, "Reactor transfer crossed atmospheric equilibrium");
                for (int tank = 0; tank < port.getTanks(); tank++) {
                    if (tank != (output ? 3 : 0)) {
                        helper.assertValueEqual(port.getGasInTank(tank).getAmount(), 0L, "Atmospheric transfer changed another reactor compartment");
                    }
                }
                if (amount != target) {
                    stableTicks[0] = 0;
                }
                helper.assertValueEqual(amount, target, "Reactor atmospheric equilibrium");
                helper.assertValueEqual(storage.getCapability().getTankPressurePa(0), GasPressure.REFERENCE_PRESSURE_PA, "Reactor equilibrium pressure");
                helper.assertTrue(++stableTicks[0] >= 12, "Reactor equilibrium has not remained stable for 12 ticks");
            });
        });
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 20)
    public static void reactorPredictionPreservesPortRestrictionsWithoutMutation(GameTestHelper helper) {
        helper.setBlock(KETTLE, CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.getDefaultState());
        helper.runAfterDelay(2, () -> {
            AirtightReactorKettleBlockEntity kettle = helper.getBlockEntity(KETTLE);
            setAmount(kettle.getInputGasTank(), 100);
            GasStorageHandler port = (GasStorageHandler) kettle.getGasPortCapability();
            GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
            var input = port.getPressureCompartment(0);
            var predicted = input.predictTransferLimits(gas, 101);
            helper.assertTrue(predicted != null, "Reactor input cannot predict quantized transfers");
            helper.assertValueEqual(predicted.drainLimit(), 101L, "Predicted input drain limit");
            helper.assertValueEqual(predicted.fillLimit(), input.getMaxAmount() - 101, "Predicted input fill limit");
            helper.assertValueEqual(input.predictTransferLimits(gas, input.getMaxAmount()).fillLimit(), 0L, "Predicted full input fill limit");
            helper.assertTrue(input.predictTransferLimits(gas, 0) == null, "Emptying a variety slot must not hide sibling routing changes");
            helper.assertValueEqual(port.getPressureCompartment(1).predictTransferLimits(gas, 0).fillLimit(), 0L, "Prediction opened a duplicate gas slot");
            kettle.getInputGasTank().forbidInsertion();
            helper.assertValueEqual(input.predictTransferLimits(gas, 101).fillLimit(), 0L, "Prediction bypassed insertion restriction");
            kettle.getInputGasTank().allowInsertion().forbidExtraction();
            helper.assertValueEqual(input.predictTransferLimits(gas, 101).drainLimit(), 0L, "Prediction bypassed extraction restriction");
            kettle.getInputGasTank().allowExtraction();
            setAmount(kettle.getOutputGasTank(), 50);
            var blocked = input.predictTransferLimits(gas, 101);
            helper.assertTrue(blocked != null && blocked.drainLimit() == 0 && blocked.fillLimit() == 0, "Prediction bypassed matching output priority");
            var output = port.getPressureCompartment(3);
            var draining = output.predictTransferLimits(gas, 49);
            helper.assertTrue(draining != null && draining.drainLimit() == 49 && draining.fillLimit() == 0, "Output prediction lost drain access or allowed insertion");
            helper.assertTrue(output.predictTransferLimits(gas, 0) == null, "Emptying output must not hide newly unblocked input endpoints");
            helper.assertValueEqual(port.getGasInTank(0).getAmount(), 100L, "Prediction mutated reactor input");
            helper.assertValueEqual(port.getGasInTank(3).getAmount(), 50L, "Prediction mutated reactor output");
            helper.succeed();
        });
    }

    private static void setAmount(SmartGasTankBehaviour storage, long amount) {
        GasTankState[] states = storage.snapshotStates();
        states[0] = new GasTankState(states[0].limits(), new GasStack(CCBGases.NATURAL_AIR.get(), amount));
        storage.replaceStates(states, 0);
    }
}
