package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockConnectivity;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasAirtightTankReleaseGameTests {
    private static final BlockPos SINGLE_TANK_POS = new BlockPos(3, 1, 3);
    private static final BlockPos SINGLE_EFFECT_POS = new BlockPos(4, 1, 3);
    private static final BlockPos MULTIBLOCK_CONTROLLER_POS = new BlockPos(2, 1, 2);
    private static final BlockPos MULTIBLOCK_EAST_POS = new BlockPos(3, 1, 2);
    private static final BlockPos MULTIBLOCK_SOUTH_POS = new BlockPos(2, 1, 3);
    private static final BlockPos MULTIBLOCK_REMOVED_POS = new BlockPos(3, 1, 3);
    private static final long MULTIBLOCK_GAS_AMOUNT = 8001;

    private GasAirtightTankReleaseGameTests() {
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 20)
    public static void singleTankRemovalReleasesContentsRadially(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AirtightTankBlockEntity tank = placeTank(helper, SINGLE_TANK_POS);
        helper.assertTrue(tank.getTankInventory().getMaxAmount() >= GasReleaseService.EFFECT_INTERVAL, "Single airtight tank cannot hold one gas release effect interval");
        tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), GasReleaseService.EFFECT_INTERVAL)).requireAccepted();

        Piglin piglin = helper.spawn(EntityType.PIGLIN, SINGLE_EFFECT_POS);
        helper.assertTrue(!piglin.hasEffect(CCBMobEffects.ZOMBIFICATION), "Single-tank release test piglin started with the zombification effect");

        boolean destroyed = level.destroyBlock(helper.absolutePos(SINGLE_TANK_POS), false);
        helper.assertTrue(destroyed, "Single airtight tank could not be destroyed");
        helper.assertTrue(level.getBlockEntity(helper.absolutePos(SINGLE_TANK_POS)) == null, "Single airtight tank block entity remained after removal");
        helper.assertTrue(piglin.hasEffect(CCBMobEffects.ZOMBIFICATION), "Single airtight tank removal did not apply the radial Natural Air release effect");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 20)
    public static void multiblockTankRemovalConservesReleasedAndSurvivingGas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AirtightTankBlockEntity controllerCandidate = placeTank(helper, MULTIBLOCK_CONTROLLER_POS);
        placeTank(helper, MULTIBLOCK_EAST_POS);
        placeTank(helper, MULTIBLOCK_SOUTH_POS);
        placeTank(helper, MULTIBLOCK_REMOVED_POS);
        GasTankMultiblockConnectivity.formMultiblock(controllerCandidate, level);

        AirtightTankBlockEntity controller = requireTank(helper, MULTIBLOCK_CONTROLLER_POS);
        helper.assertTrue(controller.isController(), "Expected multiblock controller was not the controller after formation");
        helper.assertValueEqual(controller.getWidth(), 2, "formed airtight tank multiblock width");
        helper.assertValueEqual(controller.getHeight(), 1, "formed airtight tank multiblock height");
        helper.assertValueEqual(controller.getTotalTankSize(), 4, "formed airtight tank multiblock block count");
        helper.assertTrue(controller.getTankInventory().getMaxAmount() >= MULTIBLOCK_GAS_AMOUNT, "Four-block airtight tank cannot hold the conservation test gas amount");
        controller.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), MULTIBLOCK_GAS_AMOUNT)).requireAccepted();

        Piglin piglin = helper.spawn(EntityType.PIGLIN, MULTIBLOCK_REMOVED_POS.offset(0, 0, 1));
        helper.assertTrue(!piglin.hasEffect(CCBMobEffects.ZOMBIFICATION), "Multiblock release test piglin started with the zombification effect");

        int originalTankCount = controller.getTotalTankSize();
        long originalAmount = controller.getTankInventory().getStoredAmount();
        long expectedReleasedAmount = divideCeil(originalAmount, originalTankCount);
        long expectedSurvivingAmount = originalAmount - expectedReleasedAmount;
        helper.assertTrue(expectedReleasedAmount >= GasReleaseService.EFFECT_INTERVAL, "Removed multiblock share is too small to exercise the gas release handler");

        boolean destroyed = level.destroyBlock(helper.absolutePos(MULTIBLOCK_REMOVED_POS), false);
        helper.assertTrue(destroyed, "Airtight tank multiblock part could not be destroyed");

        long survivingAmount = Stream.of(MULTIBLOCK_CONTROLLER_POS, MULTIBLOCK_EAST_POS, MULTIBLOCK_SOUTH_POS).map(pos -> requireTank(helper, pos)).mapToLong(tank -> tank.getTankInventory().getStoredAmount()).sum();
        helper.assertValueEqual(survivingAmount, expectedSurvivingAmount, "surviving multiblock gas after one tank removal");
        helper.assertValueEqual(expectedReleasedAmount + survivingAmount, originalAmount, "released plus surviving multiblock gas conservation");
        helper.assertTrue(piglin.hasEffect(CCBMobEffects.ZOMBIFICATION), "Removed multiblock tank share did not apply the radial Natural Air release effect");
        helper.succeed();
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

    private static long divideCeil(long amount, int divisor) {
        long quotient = amount / divisor;
        if (amount % divisor == 0) {
            return quotient;
        }

        return quotient + 1;
    }
}
