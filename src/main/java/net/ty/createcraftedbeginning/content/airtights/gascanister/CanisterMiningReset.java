package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CanisterMiningReset {
    private CanisterMiningReset() {
    }

    public static boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack, Set<DataComponentType<?>> ignoredComponents) {
        if (!newStack.is(oldStack.getItem())) {
            return true;
        }

        if (!newStack.isDamageableItem() || !oldStack.isDamageableItem()) {
            return !ItemStack.isSameItemSameComponents(newStack, oldStack);
        }

        DataComponentMap newComponents = newStack.getComponents();
        DataComponentMap oldComponents = oldStack.getComponents();
        if (newComponents.isEmpty() || oldComponents.isEmpty()) {
            return !newComponents.isEmpty() || !oldComponents.isEmpty();
        }

        Set<DataComponentType<?>> newKeys = collectRelevantKeys(newComponents, ignoredComponents);
        Set<DataComponentType<?>> oldKeys = collectRelevantKeys(oldComponents, ignoredComponents);
        return !newKeys.equals(oldKeys) || !newKeys.stream().allMatch(key -> Objects.equals(newComponents.get(key), oldComponents.get(key)));
    }

    private static Set<DataComponentType<?>> collectRelevantKeys(DataComponentMap components, Set<DataComponentType<?>> ignoredComponents) {
        Set<DataComponentType<?>> relevantKeys = new HashSet<>(components.keySet());
        relevantKeys.removeAll(ignoredComponents);
        return relevantKeys;
    }
}
