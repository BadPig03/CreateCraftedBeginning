package net.ty.createcraftedbeginning.content.crates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CrateContainersUtils {
    private CrateContainersUtils() {
    }

    public static void dropContents(Level level, double x, double y, double z, CrateItemStackHandler handler) {
        dropContents(level, x, y, z, handler.getStackInSlot(0), handler.getCountInSlot(0));
    }

    private static void dropContents(Level level, double x, double y, double z, ItemStack content, int count) {
        if (content.isEmpty() || count <= 0) {
            return;
        }

        int limit = content.getMaxStackSize();
        while (count > 0) {
            int dropCount = Math.min(count, limit);
            Containers.dropItemStack(level, x, y, z, content.copyWithCount(dropCount));
            count -= dropCount;
        }
    }

    public static int calculateRedstoneSignal(CrateItemStackHandler handler) {
        int count = handler.getCountInSlot(0);
        if (count <= 0) {
            return 0;
        }

        int limit = handler.getSlotLimit(0);
        if (limit <= 0) {
            return 0;
        }
        return Mth.clamp(Mth.floor((double) count / limit * 14) + 1, 0, 15);
    }

    public static boolean defaultUnpack(Level level, BlockPos pos, List<ItemStack> items, boolean simulate) {
        if (!(level.getBlockEntity(pos) instanceof CratesBlockEntity crate)) {
            return false;
        }

        CrateItemStackHandler handler = crate.getHandler();
        ItemStack originalContent = handler.getStoredItem(0);
        int originalCount = handler.getCountInSlot(0);
        int available = handler.getSlotLimit(0) - originalCount;
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
            if (stackCount > available - addedCount) {
                return false;
            }

            addedCount += stackCount;
        }

        if (simulate) {
            return true;
        }

        ItemStack validatedContent = expectedContent;
        int expectedCount = originalCount + addedCount;
        return handler.runInBatch(() -> {
            for (ItemStack stack : items) {
                if (stack.isEmpty()) {
                    continue;
                }

                ItemStack remainder = handler.insertItem(0, stack, false);
                if (remainder.isEmpty()) {
                    continue;
                }

                handler.setStoredItems(0, originalContent, originalCount);
                return false;
            }

            ItemStack storedContent = handler.getStoredItem(0);
            boolean hasExpectedState = handler.getCountInSlot(0) == expectedCount && (expectedCount == 0 || ItemStack.isSameItemSameComponents(storedContent, validatedContent));
            if (!hasExpectedState) {
                handler.setStoredItems(0, originalContent, originalCount);
                return false;
            }

            return true;
        });
    }
}
