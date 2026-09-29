package net.ty.createcraftedbeginning.gametests.gas;

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
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.OverpressureBehaviour;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockConnectivity;
import net.ty.createcraftedbeginning.gas.overpressure.OverpressureStressCalculator;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightTankOverpressureGameTests {
    private static final BlockPos LOWER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos UPPER_POS = new BlockPos(1, 2, 1);

    private AirtightTankOverpressureGameTests() {
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 150)
    public static void multiblockOnlyAdvancesControllerStructuralStress(GameTestHelper helper) {
        AirtightTankBlockEntity controller = formTwoHighTank(helper);
        AirtightTankBlockEntity member = requireTank(helper, UPPER_POS);
        fillControllerToHardPressure(helper, controller);

        OverpressureBehaviour controllerStress = overpressureBehaviour(helper, LOWER_POS);
        OverpressureBehaviour memberStress = overpressureBehaviour(helper, UPPER_POS);
        int finishTick = OverpressureStressCalculator.STABILIZATION_GRACE_TICKS + 50;
        int[] ticks = new int[1];

        helper.onEachTick(() -> {
            ticks[0]++;
            if (ticks[0] < finishTick) {
                return;
            }

            helper.assertTrue(controller.isController(), "Two-high airtight tank controller changed unexpectedly");
            helper.assertTrue(!member.isController(), "Two-high airtight tank member became a controller unexpectedly");
            helper.assertTrue(controllerStress.getStress() > 0.40 && controllerStress.getStress() < 0.60, "Airtight tank controller did not advance the shared structural stress at the expected rate");
            helper.assertTrue(memberStress.getStress() == 0.0, "Non-controller airtight tank part advanced structural stress independently");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 160)
    public static void splittingTankPreservesControllerStructuralStress(GameTestHelper helper) {
        AirtightTankBlockEntity controller = formTwoHighTank(helper);
        fillControllerToHardPressure(helper, controller);
        OverpressureBehaviour controllerStress = overpressureBehaviour(helper, LOWER_POS);
        int splitTick = OverpressureStressCalculator.STABILIZATION_GRACE_TICKS + 50;
        boolean[] split = new boolean[1];
        double[] stressBeforeSplit = new double[1];
        int[] ticks = new int[1];

        helper.onEachTick(() -> {
            ticks[0]++;
            if (!split[0] && ticks[0] >= splitTick) {
                stressBeforeSplit[0] = controllerStress.getStress();
                helper.assertTrue(stressBeforeSplit[0] > 0.40, "Airtight tank did not accumulate structural stress before split");
                boolean destroyed = helper.getLevel().destroyBlock(helper.absolutePos(LOWER_POS), false);
                helper.assertTrue(destroyed, "Airtight tank controller could not be removed for split test");
                split[0] = true;
                return;
            }

            if (!split[0]) {
                return;
            }

            AirtightTankBlockEntity survivor = requireTank(helper, UPPER_POS);
            OverpressureBehaviour survivorStress = overpressureBehaviour(helper, UPPER_POS);
            helper.assertTrue(survivor.isController(), "Surviving airtight tank part did not become a standalone controller");
            helper.assertTrue(Math.abs(survivorStress.getStress() - stressBeforeSplit[0]) < 1.0E-6, "Airtight tank split reset or changed accumulated structural stress");
            helper.assertTrue(survivorStress.isStabilizing(), "Split airtight tank did not receive a fresh stabilization grace period");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 30)
    public static void formationKeepsMaximumStressAndCopiesSnapshotToMembers(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(2, 1, 2);
        BlockPos eastPos = new BlockPos(3, 1, 2);
        BlockPos southPos = new BlockPos(2, 1, 3);
        BlockPos southEastPos = new BlockPos(3, 1, 3);
        AirtightTankBlockEntity controllerCandidate = placeTank(helper, controllerPos);
        placeTank(helper, southEastPos);
        overpressureBehaviour(helper, controllerPos).setStress(0.25);
        overpressureBehaviour(helper, southEastPos).setStress(0.75);

        placeTank(helper, eastPos);
        placeTank(helper, southPos);
        GasTankMultiblockConnectivity.formMultiblock(controllerCandidate, helper.getLevel());

        AirtightTankBlockEntity controller = requireTank(helper, controllerPos);
        helper.assertTrue(controller.isController(), "Expected northwest airtight tank to control the 2x2 structure");
        helper.assertValueEqual(controller.getWidth(), 2, "2x2 airtight tank width");
        helper.assertValueEqual(controller.getHeight(), 1, "2x2 airtight tank height");
        for (BlockPos pos : new BlockPos[] {controllerPos, eastPos, southPos, southEastPos}) {
            double stress = overpressureBehaviour(helper, pos).getStress();
            helper.assertTrue(Math.abs(stress - 0.75) < 1.0E-6, "Airtight tank formation did not preserve the maximum structural stress at " + pos);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 80)
    public static void multiblockRuptureVentsSharedVesselAndDestroysController(GameTestHelper helper) {
        AirtightTankBlockEntity controller = formTwoHighTank(helper);
        fillControllerToHardPressure(helper, controller);
        overpressureBehaviour(helper, LOWER_POS).setStress(1.0);
        long[] initializedAtTick = {helper.getTick()};

        helper.onEachTick(() -> {
            long ticksAtFailureStress = helper.getTick() - initializedAtTick[0];
            BlockEntity controllerBlockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(LOWER_POS));
            if (ticksAtFailureStress < OverpressureStressCalculator.STABILIZATION_GRACE_TICKS - 2L) {
                helper.assertTrue(controllerBlockEntity instanceof AirtightTankBlockEntity, "Airtight tank ruptured during its stabilization grace period");
                return;
            }

            if (controllerBlockEntity instanceof AirtightTankBlockEntity) {
                return;
            }

            helper.assertTrue(controller.getTankInventory().getGasInTank(0).isEmpty(), "Airtight tank rupture did not drain the failed shared vessel before the blast");

            if (!CCBConfig.server().gas.pressureRupture.explosionDamagesSurroundingBlocks.get()) {
                AirtightTankBlockEntity survivor = requireTank(helper, UPPER_POS);
                helper.assertTrue(survivor.isController(), "Airtight tank survivor did not become the controller after rupture");
                helper.assertTrue(survivor.getTankInventory().getGasInTank(0).isEmpty(), "Airtight tank rupture did not vent the shared vessel");
                helper.assertTrue(survivor.getTankInventory().getPressurePa() <= GasPressureLimits.SAFE_PRESSURE_PA, "Airtight tank survivor remained overpressure after rupture venting");
                helper.assertTrue(overpressureBehaviour(helper, UPPER_POS).isStabilizing(), "Airtight tank survivor did not receive a fresh stabilization grace period after rupture split");
            }
            helper.succeed();
        });
    }

    private static AirtightTankBlockEntity formTwoHighTank(GameTestHelper helper) {
        AirtightTankBlockEntity controllerCandidate = placeTank(helper, LOWER_POS);
        placeTank(helper, UPPER_POS);
        GasTankMultiblockConnectivity.formMultiblock(controllerCandidate, helper.getLevel());
        AirtightTankBlockEntity controller = requireTank(helper, LOWER_POS);
        helper.assertTrue(controller.isController(), "Expected lower airtight tank to be the controller");
        helper.assertValueEqual(controller.getHeight(), 2, "two-high airtight tank height");
        helper.assertValueEqual(controller.getTotalTankSize(), 2, "two-high airtight tank block count");
        return controller;
    }

    private static void fillControllerToHardPressure(GameTestHelper helper, AirtightTankBlockEntity controller) {
        long amount = GasPressure.amount(controller.getTankInventory().getVolume(), GasPressureLimits.HARD_PRESSURE_PA);
        controller.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), amount)).requireAccepted();
        helper.assertValueEqual(controller.getTankInventory().getPressurePa(), GasPressureLimits.HARD_PRESSURE_PA, "airtight tank hard-pressure test fill");
    }

    private static AirtightTankBlockEntity placeTank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        return requireTank(helper, pos);
    }

    private static AirtightTankBlockEntity requireTank(GameTestHelper helper, BlockPos pos) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof AirtightTankBlockEntity, "Airtight tank block entity was not initialized at " + pos);
        if (!(blockEntity instanceof AirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Airtight tank block entity missing at " + pos + '.');
        }

        return tank;
    }

    private static OverpressureBehaviour overpressureBehaviour(GameTestHelper helper, BlockPos pos) {
        OverpressureBehaviour behaviour = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(pos), OverpressureBehaviour.TYPE);
        helper.assertTrue(behaviour != null, "Airtight tank did not register OverpressureBehaviour at " + pos);
        if (behaviour == null) {
            throw new NullPointerException("Airtight tank did not register OverpressureBehaviour at " + pos + '.');
        }

        return behaviour;
    }
}
