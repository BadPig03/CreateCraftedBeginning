package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.OverpressureBehaviour;
import net.ty.createcraftedbeginning.gas.overpressure.OverpressureStressCalculator;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasInjectionChamberOverpressureGameTests {
    private static final BlockPos CHAMBER_POS = new BlockPos(1, 1, 1);

    private GasInjectionChamberOverpressureGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 190)
    public static void idleInjectionChamberRupturesAtStructuralFailure(GameTestHelper helper) {
        GasStorageHandler storage = placeFilledChamber(helper);
        overpressureBehaviour(helper);
        int[] ticks = new int[1];

        helper.onEachTick(() -> {
            ticks[0]++;
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(CHAMBER_POS));
            if (ticks[0] <= OverpressureStressCalculator.STABILIZATION_GRACE_TICKS + 50) {
                helper.assertTrue(blockEntity instanceof GasInjectionChamberBlockEntity, "Injection chamber ruptured before structural stress could reach its failure threshold");
                return;
            }

            if (blockEntity instanceof GasInjectionChamberBlockEntity) {
                return;
            }

            helper.assertTrue(storage.getGasInTank(0).isEmpty(), "Injection chamber rupture did not vent the failed gas compartment");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 320)
    public static void injectionChamberStructuralStressRecoversAfterPressureReturnsSafe(GameTestHelper helper) {
        GasStorageHandler storage = placeFilledChamber(helper);
        OverpressureBehaviour overpressure = overpressureBehaviour(helper);
        boolean[] depressurized = new boolean[1];

        helper.onEachTick(() -> {
            if (!depressurized[0]) {
                if (overpressure.getStress() < 0.5) {
                    return;
                }

                storage.drain(Long.MAX_VALUE, GasAction.EXECUTE);
                helper.assertTrue(storage.getTankPressurePa(0) <= GasPressureLimits.SAFE_PRESSURE_PA, "Injection chamber remained overpressure after test depressurization");
                depressurized[0] = true;
                return;
            }

            if (overpressure.getStress() > 0.0) {
                return;
            }

            helper.assertTrue(!overpressure.isAtFailureThreshold(), "Recovered injection chamber remained at the structural failure threshold");
            helper.succeed();
        });
    }

    private static GasStorageHandler placeFilledChamber(GameTestHelper helper) {
        helper.setBlock(CHAMBER_POS, CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.get().defaultBlockState());
        GasHandler handler = helper.getLevel().getCapability(GasCapabilities.BLOCK, helper.absolutePos(CHAMBER_POS), Direction.UP);
        helper.assertTrue(handler instanceof GasStorageHandler, "Gas injection chamber did not expose pressure-aware gas storage on its top face");
        if (!(handler instanceof GasStorageHandler storage)) {
            throw new IllegalStateException("Gas injection chamber did not expose pressure-aware gas storage on its top face.");
        }

        helper.assertTrue(storage.getTankMaxPressurePa(0) >= GasPressureLimits.HARD_PRESSURE_PA, "Gas injection chamber pressure ceiling was below the integration-test pressure");
        long amount = GasPressure.amount(storage.getTankVolume(0), GasPressureLimits.HARD_PRESSURE_PA);
        long filled = storage.fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE);
        helper.assertValueEqual(filled, amount, "gas injection chamber overpressure test fill amount");
        helper.assertValueEqual(storage.getTankPressurePa(0), GasPressureLimits.HARD_PRESSURE_PA, "gas injection chamber overpressure test pressure");
        return storage;
    }

    private static OverpressureBehaviour overpressureBehaviour(GameTestHelper helper) {
        OverpressureBehaviour behaviour = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(CHAMBER_POS), OverpressureBehaviour.TYPE);
        helper.assertTrue(behaviour != null, "Gas injection chamber did not register OverpressureBehaviour");
        if (behaviour == null) {
            throw new NullPointerException("Gas injection chamber did not register OverpressureBehaviour.");
        }

        return behaviour;
    }
}
