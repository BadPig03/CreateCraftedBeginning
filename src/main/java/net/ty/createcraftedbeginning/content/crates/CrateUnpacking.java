package net.ty.createcraftedbeginning.content.crates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.foundation.transaction.ResourceTransaction;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CrateUnpacking {

    private CrateUnpacking() {
    }

    @Internal
    public static boolean defaultUnpack(Level level, BlockPos pos, List<ItemStack> items, boolean simulate) {
        if (!(level.getBlockEntity(pos) instanceof CratesBlockEntity crate)) {
            return false;
        }

        CrateItemStackHandler handler = crate.getHandler();
        ItemStack originalContent = handler.getStoredItem(0);
        int originalCount = handler.getCountInSlot(0);
        int remainingCapacity = handler.getRemainingCapacity();
        int addedCount = 0;
        ItemStack expectedContent = originalContent;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }

            if (expectedContent.isEmpty()) {
                expectedContent = stack;
            }
            if (!ItemStack.isSameItemSameComponents(expectedContent, stack) || !handler.isItemValid(0, stack)) {
                return false;
            }

            int stackCount = stack.getCount();
            if (stackCount > remainingCapacity - addedCount) {
                return false;
            }

            addedCount += stackCount;
        }

        if (simulate) {
            return true;
        }

        ItemStack expectedStoredContent = expectedContent.copy();
        int expectedStoredCount = originalCount + addedCount;
        return handler.runInBatch(() -> new ResourceTransaction().add(ResourceTransaction.participant(() -> matchesCrateState(handler, originalContent, originalCount), () -> new CrateSnapshot(handler.getStoredItem(0).copy(), handler.getCountInSlot(0)), () -> {
            for (ItemStack stack : items) {
                if (stack.isEmpty()) {
                    continue;
                }

                if (!handler.insertItem(0, stack, false).isEmpty()) {
                    return false;
                }
            }

            return matchesCrateState(handler, expectedStoredContent, expectedStoredCount);
        }, snapshot -> handler.setStoredItems(0, snapshot.content().copy(), snapshot.count()))).commit());
    }

    private static boolean matchesCrateState(CrateItemStackHandler handler, ItemStack expectedContent, int expectedCount) {
        if (handler.getCountInSlot(0) != expectedCount) {
            return false;
        }

        ItemStack storedContent = handler.getStoredItem(0);
        return expectedCount == 0 || ItemStack.isSameItemSameComponents(storedContent, expectedContent);
    }

    private record CrateSnapshot(ItemStack content, int count) {}
}
