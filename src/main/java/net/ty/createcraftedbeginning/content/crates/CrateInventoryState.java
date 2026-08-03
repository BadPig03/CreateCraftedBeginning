package net.ty.createcraftedbeginning.content.crates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CrateInventoryState {
    private final ItemStack content;
    private final int count;

    private CrateInventoryState(ItemStack content, int count) {
        this.content = content;
        this.count = count;
    }

    public static CrateInventoryState normalize(ItemStack content, int count, int maxCount) {
        int capacity = Math.max(0, maxCount);
        if (capacity == 0 || content.isEmpty() || count <= 0) {
            return new CrateInventoryState(ItemStack.EMPTY, 0);
        }
        return new CrateInventoryState(content.copyWithCount(1), Math.min(count, capacity));
    }

    public ItemStack content() {
        return content;
    }

    public int count() {
        return count;
    }
}
