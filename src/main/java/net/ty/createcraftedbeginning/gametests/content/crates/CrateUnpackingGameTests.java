package net.ty.createcraftedbeginning.gametests.content.crates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import net.ty.createcraftedbeginning.content.crates.CrateUnpacking;
import net.ty.createcraftedbeginning.content.crates.CratesBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrateUnpackingGameTests {
    private CrateUnpackingGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void simulationAndOverflowKeepStoredItems(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CCBBlocks.ANDESITE_CRATE_BLOCK.getDefaultState());
        CratesBlockEntity crate = helper.getBlockEntity(pos);
        CrateItemStackHandler handler = crate.getHandler();
        handler.setStoredItems(0, new ItemStack(Items.IRON_INGOT), 10);
        List<ItemStack> fitting = List.of(new ItemStack(Items.IRON_INGOT, 3));
        BlockPos worldPos = helper.absolutePos(pos);

        helper.assertTrue(CrateUnpacking.defaultUnpack(level, worldPos, fitting, true), "Valid package simulation failed");
        helper.assertValueEqual(handler.getCountInSlot(0), 10, "Stored items after simulation");
        List<ItemStack> overflow = List.of(new ItemStack(Items.IRON_INGOT, handler.getRemainingCapacity() + 1));
        helper.assertTrue(!CrateUnpacking.defaultUnpack(level, worldPos, overflow, false), "Oversized package was accepted");
        helper.assertValueEqual(handler.getCountInSlot(0), 10, "Stored items after overflow rejection");
        helper.assertTrue(CrateUnpacking.defaultUnpack(level, worldPos, fitting, false), "Valid package did not commit");
        helper.assertValueEqual(handler.getCountInSlot(0), 13, "Stored items after commit");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void mixedPackageDoesNotPartiallyFillEmptyCrate(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CCBBlocks.ANDESITE_CRATE_BLOCK.getDefaultState());
        CratesBlockEntity crate = helper.getBlockEntity(pos);
        List<ItemStack> mixed = List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLD_INGOT));

        helper.assertTrue(!CrateUnpacking.defaultUnpack(helper.getLevel(), helper.absolutePos(pos), mixed, false), "Mixed package was accepted");
        helper.assertValueEqual(crate.getHandler().getCountInSlot(0), 0, "Empty crate after rejected mixed package");
        helper.succeed();
    }
}
