package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class GasCanisterPackUtils {
    private GasCanisterPackUtils() {
    }

    public static boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        if (!newStack.is(oldStack.getItem())) {
            return true;
        }

        if (!newStack.isDamageableItem() || !oldStack.isDamageableItem()) {
            return !ItemStack.isSameItemSameComponents(newStack, oldStack);
        }

        DataComponentMap newComponents = newStack.getComponents();
        DataComponentMap oldComponents = oldStack.getComponents();
        if (newComponents.isEmpty() || oldComponents.isEmpty()) {
            return !(newComponents.isEmpty() && oldComponents.isEmpty());
        }

        Set<DataComponentType<?>> newKeys = new HashSet<>(newComponents.keySet());
        Set<DataComponentType<?>> oldKeys = new HashSet<>(oldComponents.keySet());
        newKeys.remove(CCBDataComponents.GAS_CANISTER_PACK_FLAGS);
        newKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CONTENTS);
        newKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES);
        oldKeys.remove(CCBDataComponents.GAS_CANISTER_PACK_FLAGS);
        oldKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CONTENTS);
        oldKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES);
        return !newKeys.equals(oldKeys) || !newKeys.stream().allMatch(key -> Objects.equals(newComponents.get(key), oldComponents.get(key)));
    }

    public static boolean isCanisterPackMenuOpened(@NotNull Player player, ItemStack pack) {
        return player.containerMenu instanceof GasCanisterPackMenu menu && ItemStack.matches(menu.contentHolder, pack);
    }
}
