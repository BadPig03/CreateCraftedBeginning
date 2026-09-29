package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.OverpressureBehaviour;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.gas.overpressure.OverpressureStressCalculator;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightReactorKettleOverpressureGameTests {
    private static final BlockPos KETTLE_POS = new BlockPos(2, 2, 2);
    private static final int FIRST_OUTPUT_CHANNEL = 3;

    private AirtightReactorKettleOverpressureGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 190)
    public static void reactorKettleTracksFiveGasCompartmentsIndependently(GameTestHelper helper) {
        helper.setBlock(KETTLE_POS, CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.get().defaultBlockState());

        SmartGasTankBehaviour[] inputGasTank = new SmartGasTankBehaviour[1];
        SmartGasTankBehaviour[] outputGasTank = new SmartGasTankBehaviour[1];
        OverpressureBehaviour[] overpressure = new OverpressureBehaviour[1];
        long[] initializedAtTick = {-1};
        long[] switchedAtTick = {-1};
        double[] inputStressBeforeRecovery = new double[1];

        helper.onEachTick(() -> {
            long currentTick = helper.getTick();

            if (initializedAtTick[0] < 0) {
                if (currentTick < 2) {
                    return;
                }

                BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(KETTLE_POS));
                helper.assertTrue(blockEntity instanceof AirtightReactorKettleBlockEntity, "Reactor kettle did not survive multiblock assembly");

                inputGasTank[0] = gasTankBehaviour(helper, SmartGasTankBehaviour.INPUT, "input");
                outputGasTank[0] = gasTankBehaviour(helper, SmartGasTankBehaviour.OUTPUT, "output");
                overpressure[0] = overpressureBehaviour(helper);
                helper.assertValueEqual(overpressure[0].getChannelCount(), 5, "reactor kettle overpressure channel count");

                setTankToHardPressure(helper, inputGasTank[0], "reactor input channel");
                initializedAtTick[0] = currentTick;
                return;
            }

            if (switchedAtTick[0] < 0) {
                long ticksUnderPressure = currentTick - initializedAtTick[0];
                if (ticksUnderPressure < OverpressureStressCalculator.STABILIZATION_GRACE_TICKS + 50L) {
                    return;
                }

                inputStressBeforeRecovery[0] = overpressure[0].getStress(0);
                helper.assertTrue(inputStressBeforeRecovery[0] > 0.40 && inputStressBeforeRecovery[0] < 0.60, "Reactor input channel did not accumulate the expected independent stress");
                helper.assertTrue(overpressure[0].getStress(1) == 0.0 && overpressure[0].getStress(2) == 0.0 && overpressure[0].getStress(FIRST_OUTPUT_CHANNEL) == 0.0 && overpressure[0].getStress(4) == 0.0, "Unpressurized reactor compartments accumulated structural stress");

                clearTank(helper, inputGasTank[0]);
                setTankToHardPressure(helper, outputGasTank[0], "reactor output channel");
                switchedAtTick[0] = currentTick;
                return;
            }

            if (currentTick - switchedAtTick[0] < 50L) {
                return;
            }

            helper.assertTrue(overpressure[0].getStress(0) < inputStressBeforeRecovery[0], "Depressurized reactor input channel did not recover independently");
            helper.assertTrue(overpressure[0].getStress(FIRST_OUTPUT_CHANNEL) > 0.40, "Reactor output channel did not accumulate independent stress");
            helper.assertTrue(overpressure[0].getStress(1) == 0.0 && overpressure[0].getStress(2) == 0.0 && overpressure[0].getStress(4) == 0.0, "Unpressurized reactor compartments accumulated structural stress");
            helper.assertValueEqual(overpressure[0].getMostStressedChannel(), FIRST_OUTPUT_CHANNEL, "reactor most-stressed gas channel");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 80)
    public static void reactorKettleRupturesWhenOneGasCompartmentFails(GameTestHelper helper) {
        helper.setBlock(KETTLE_POS, CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.get().defaultBlockState());

        SmartGasTankBehaviour[] outputGasTank = new SmartGasTankBehaviour[1];
        long[] initializedAtTick = {-1};

        helper.onEachTick(() -> {
            long currentTick = helper.getTick();
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(KETTLE_POS));

            if (initializedAtTick[0] < 0) {
                if (currentTick < 2) {
                    return;
                }

                helper.assertTrue(blockEntity instanceof AirtightReactorKettleBlockEntity, "Reactor kettle did not survive multiblock assembly");
                outputGasTank[0] = gasTankBehaviour(helper, SmartGasTankBehaviour.OUTPUT, "output");
                setTankToHardPressure(helper, outputGasTank[0], "reactor output rupture channel");

                OverpressureBehaviour overpressure = overpressureBehaviour(helper);
                overpressure.setStress(FIRST_OUTPUT_CHANNEL, 1.0);
                initializedAtTick[0] = currentTick;
                return;
            }

            long ticksAtFailureStress = currentTick - initializedAtTick[0];
            if (ticksAtFailureStress < OverpressureStressCalculator.STABILIZATION_GRACE_TICKS - 2L) {
                helper.assertTrue(blockEntity instanceof AirtightReactorKettleBlockEntity, "Reactor kettle ruptured during its stabilization grace period");
                return;
            }

            if (blockEntity instanceof AirtightReactorKettleBlockEntity) {
                return;
            }

            helper.assertTrue(outputGasTank[0].getCapability().getGasInTank(0).isEmpty(), "Reactor rupture did not vent the failed gas compartment");
            helper.succeed();
        });
    }

    private static SmartGasTankBehaviour gasTankBehaviour(GameTestHelper helper, BehaviourType<SmartGasTankBehaviour> type, String label) {
        SmartGasTankBehaviour behaviour = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(KETTLE_POS), type);
        helper.assertTrue(behaviour != null, "Reactor kettle did not register its " + label + " gas tank behaviour");
        if (behaviour == null) {
            throw new NullPointerException("Reactor kettle did not register its " + label + " gas tank behaviour" + '.');
        }

        return behaviour;
    }

    private static OverpressureBehaviour overpressureBehaviour(GameTestHelper helper) {
        OverpressureBehaviour behaviour = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(KETTLE_POS), OverpressureBehaviour.TYPE);
        helper.assertTrue(behaviour != null, "Reactor kettle did not register OverpressureBehaviour");
        if (behaviour == null) {
            throw new NullPointerException("Reactor kettle did not register OverpressureBehaviour.");
        }

        return behaviour;
    }

    private static void setTankToHardPressure(GameTestHelper helper, SmartGasTankBehaviour behaviour, String label) {
        GasTankState[] states = behaviour.snapshotStates();
        helper.assertTrue(states.length > 0, label + " tank index was out of range");

        GasTankState current = states[0];
        helper.assertTrue(current.limits().maxPressurePa() >= GasPressureLimits.HARD_PRESSURE_PA, label + " pressure ceiling was below 24 atm");
        long amount = GasPressure.amount(current.limits().volumeLiters(), GasPressureLimits.HARD_PRESSURE_PA);
        states[0] = new GasTankState(current.limits(), new GasStack(CCBGases.NATURAL_AIR.get(), amount));
        behaviour.replaceStates(states, 0);

        GasStorageHandler storage = behaviour.getCapability();
        helper.assertValueEqual(storage.getGasInTank(0).getAmount(), amount, label + " hard-pressure stored amount");
        helper.assertValueEqual(storage.getTankPressurePa(0), GasPressureLimits.HARD_PRESSURE_PA, label + " hard-pressure fill pressure");
    }

    private static void clearTank(GameTestHelper helper, SmartGasTankBehaviour behaviour) {
        GasTankState[] states = behaviour.snapshotStates();
        helper.assertTrue(states.length > 0, "reactor input channel tank index was out of range");

        GasTankState current = states[0];
        states[0] = new GasTankState(current.limits(), GasStack.EMPTY);
        behaviour.replaceStates(states, 0);

        GasStorageHandler storage = behaviour.getCapability();
        helper.assertTrue(storage.getGasInTank(0).isEmpty(), "reactor input channel did not clear during the test transition");
        helper.assertTrue(storage.getTankPressurePa(0) <= GasPressureLimits.SAFE_PRESSURE_PA, "reactor input channel remained overpressure after the test transition");
    }
}
