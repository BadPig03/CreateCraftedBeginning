package net.ty.createcraftedbeginning.content.crates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record DiscardingCrateInsertionPlan(ItemStack content, int count, boolean trackedDiscard) {
    public static DiscardingCrateInsertionPlan plan(ItemStack storedContent, int storedCount, ItemStack incomingStack, int maxCount, Predicate<ItemStack> trackedItemPredicate) {
        int incomingCount = incomingStack.getCount();
        if (storedContent.isEmpty()) {
            int nextCount = Math.min(incomingCount, maxCount);
            boolean trackedDiscard = trackedItemPredicate.test(incomingStack) && nextCount < incomingCount;
            return new DiscardingCrateInsertionPlan(incomingStack, nextCount, trackedDiscard);
        }

        if (ItemStack.isSameItemSameComponents(storedContent, incomingStack)) {
            int acceptedCount = Math.clamp(maxCount - storedCount, 0, incomingCount);
            int nextCount = storedCount + acceptedCount;
            boolean trackedDiscard = trackedItemPredicate.test(incomingStack) && acceptedCount < incomingCount;
            return new DiscardingCrateInsertionPlan(storedContent, nextCount, trackedDiscard);
        }

        int nextCount = Math.min(incomingCount, maxCount);
        boolean trackedDiscard = storedCount > 0 && trackedItemPredicate.test(storedContent) || trackedItemPredicate.test(incomingStack) && nextCount < incomingCount;
        return new DiscardingCrateInsertionPlan(incomingStack, nextCount, trackedDiscard);
    }
}
