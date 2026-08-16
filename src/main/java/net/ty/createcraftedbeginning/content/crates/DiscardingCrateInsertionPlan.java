package net.ty.createcraftedbeginning.content.crates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record DiscardingCrateInsertionPlan(ItemStack content, int count, boolean trackedDiscard) {
    public static DiscardingCrateInsertionPlan plan(ItemStack storedContent, int storedCount, ItemStack incoming, int maxCount, Predicate<ItemStack> trackedItemPredicate) {
        int incomingCount = incoming.getCount();
        if (storedContent.isEmpty()) {
            int nextCount = Math.min(incomingCount, maxCount);
            boolean trackedDiscard = trackedItemPredicate.test(incoming) && nextCount < incomingCount;
            return new DiscardingCrateInsertionPlan(incoming, nextCount, trackedDiscard);
        }

        if (ItemStack.isSameItemSameComponents(storedContent, incoming)) {
            int accepted = Math.clamp(maxCount - storedCount, 0, incomingCount);
            int nextCount = storedCount + accepted;
            boolean trackedDiscard = trackedItemPredicate.test(incoming) && accepted < incomingCount;
            return new DiscardingCrateInsertionPlan(storedContent, nextCount, trackedDiscard);
        }

        int nextCount = Math.min(incomingCount, maxCount);
        boolean trackedDiscard = storedCount > 0 && trackedItemPredicate.test(storedContent) || trackedItemPredicate.test(incoming) && nextCount < incomingCount;
        return new DiscardingCrateInsertionPlan(incoming, nextCount, trackedDiscard);
    }
}
