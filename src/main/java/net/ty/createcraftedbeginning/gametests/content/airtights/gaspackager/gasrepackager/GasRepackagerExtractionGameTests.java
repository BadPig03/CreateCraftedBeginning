package net.ty.createcraftedbeginning.gametests.content.airtights.gaspackager.gasrepackager;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonFactory;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerExtraction;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerExtraction.ExtractionResult;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.Candidate;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasRepackagerExtractionGameTests {
    private GasRepackagerExtractionGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void failedExtractionRestoresEarlierPackages(GameTestHelper helper) {
        checkFailedExtraction(helper, false);
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void rejectedRollbackReturnsPackagesToCaller(GameTestHelper helper) {
        checkFailedExtraction(helper, true);
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void changedPackageRejectsPlanBeforeExtraction(GameTestHelper helper) {
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 100);
        ItemStack expected = BalloonFactory.create(gas, "original");
        ItemStack changed = BalloonFactory.create(gas, "changed");
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, changed);

        ExtractionResult result = GasRepackagerExtraction.extractCandidates(inventory, List.of(new Candidate(0, expected, gas)));
        helper.assertTrue(!result.committed(), "Changed address must reject the extraction plan");
        helper.assertTrue(ItemStack.matches(inventory.getStackInSlot(0), changed), "Rejected plan mutated inventory");
        helper.assertTrue(result.rollbackRemainders().isEmpty(), "Validation failure produced rollback leftovers");
        helper.succeed();
    }

    private static void checkFailedExtraction(GameTestHelper helper, boolean rejectRollback) {
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 100);
        ItemStack balloon = BalloonFactory.create(gas, "destination");
        ItemStackHandler inventory = new ItemStackHandler(2) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot == 0 && !simulate) {
                    return ItemStack.EMPTY;
                }

                return super.extractItem(slot, amount, simulate);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (rejectRollback) {
                    return stack;
                }

                return super.insertItem(slot, stack, simulate);
            }
        };
        inventory.setStackInSlot(0, balloon.copy());
        inventory.setStackInSlot(1, balloon.copy());
        ExtractionResult result = GasRepackagerExtraction.extractCandidates(inventory, List.of(new Candidate(0, balloon, gas), new Candidate(1, balloon, gas)));
        int stored = inventory.getStackInSlot(0).getCount() + inventory.getStackInSlot(1).getCount();
        int returned = result.rollbackRemainders().stream().mapToInt(ItemStack::getCount).sum();

        helper.assertTrue(!result.committed(), "Partial extraction committed");
        helper.assertValueEqual(stored + returned, 2, "Packages after failed extraction");
        helper.assertValueEqual(returned, rejectRollback ? 1 : 0, "Rollback leftovers");
        helper.assertTrue(result.rollbackRemainders().stream().allMatch(stack -> ItemStack.isSameItemSameComponents(stack, balloon)), "Rollback changed package data");
        helper.succeed();
    }
}
