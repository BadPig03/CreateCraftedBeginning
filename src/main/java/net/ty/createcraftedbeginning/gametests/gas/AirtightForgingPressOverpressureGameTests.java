package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.OverpressureBehaviour;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.gas.overpressure.OverpressureStressCalculator;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightForgingPressOverpressureGameTests {
    private static final BlockPos PRESS_POS = new BlockPos(2, 2, 2);

    private AirtightForgingPressOverpressureGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 190)
    public static void idleForgingPressRupturesAtSharedStructuralFailure(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(PRESS_POS, CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK.get().defaultBlockState());

        SmartGasTankBehaviour[] tankBehaviour = new SmartGasTankBehaviour[1];
        OverpressureBehaviour[] overpressure = new OverpressureBehaviour[1];
        long[] initializedAtTick = {-1};

        helper.onEachTick(() -> {
            long currentTick = helper.getTick();

            if (initializedAtTick[0] < 0) {
                if (currentTick < 2) {
                    return;
                }

                BlockEntity blockEntity = level.getBlockEntity(helper.absolutePos(PRESS_POS));
                helper.assertTrue(blockEntity instanceof AirtightForgingPressBlockEntity, "Forging press did not survive multiblock assembly");

                tankBehaviour[0] = gasTankBehaviour(helper);
                GasStorageHandler storage = tankBehaviour[0].getCapability();
                fillToHardPressure(helper, storage);

                overpressure[0] = overpressureBehaviour(helper);
                helper.assertValueEqual(overpressure[0].getChannelCount(), 1, "forging press overpressure channel count");
                initializedAtTick[0] = currentTick;
                return;
            }

            long ticksUnderPressure = currentTick - initializedAtTick[0];
            BlockEntity currentBlockEntity = level.getBlockEntity(helper.absolutePos(PRESS_POS));
            if (ticksUnderPressure <= OverpressureStressCalculator.STABILIZATION_GRACE_TICKS + 50L) {
                helper.assertTrue(currentBlockEntity instanceof AirtightForgingPressBlockEntity, "Forging press ruptured before structural stress could reach its failure threshold");
                return;
            }

            if (currentBlockEntity instanceof AirtightForgingPressBlockEntity) {
                return;
            }

            helper.assertTrue(tankBehaviour[0].getCapability().getGasInTank(0).isEmpty(), "Forging press rupture did not vent the failed gas compartment");
            helper.succeed();
        });
    }

    private static SmartGasTankBehaviour gasTankBehaviour(GameTestHelper helper) {
        SmartGasTankBehaviour behaviour = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(PRESS_POS), SmartGasTankBehaviour.INPUT);
        helper.assertTrue(behaviour != null, "Forging press did not register its input gas tank behaviour");
        if (behaviour == null) {
            throw new NullPointerException("Forging press did not register its input gas tank behaviour.");
        }

        return behaviour;
    }

    private static OverpressureBehaviour overpressureBehaviour(GameTestHelper helper) {
        OverpressureBehaviour behaviour = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(PRESS_POS), OverpressureBehaviour.TYPE);
        helper.assertTrue(behaviour != null, "Forging press did not register OverpressureBehaviour");
        if (behaviour == null) {
            throw new NullPointerException("Forging press did not register OverpressureBehaviour.");
        }

        return behaviour;
    }

    private static void fillToHardPressure(GameTestHelper helper, GasStorageHandler storage) {
        helper.assertTrue(storage.getTankMaxPressurePa(0) >= GasPressureLimits.HARD_PRESSURE_PA, "Forging press pressure ceiling was below 24 atm");
        long amount = GasPressure.amount(storage.getTankVolume(0), GasPressureLimits.HARD_PRESSURE_PA);
        long filled = storage.fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE);
        helper.assertValueEqual(filled, amount, "forging press hard-pressure fill amount");
        helper.assertValueEqual(storage.getTankPressurePa(0), GasPressureLimits.HARD_PRESSURE_PA, "forging press hard-pressure fill pressure");
    }
}
