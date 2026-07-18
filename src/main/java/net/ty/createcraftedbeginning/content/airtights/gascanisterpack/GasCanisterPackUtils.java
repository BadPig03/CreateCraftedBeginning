package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasCanisterPackUtils {
    private GasCanisterPackUtils() {
    }

    public static boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
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

        Set<DataComponentType<?>> newKeys = getComparedKeys(newComponents);
        Set<DataComponentType<?>> oldKeys = getComparedKeys(oldComponents);
        return !newKeys.equals(oldKeys) || !newKeys.stream().allMatch(key -> Objects.equals(newComponents.get(key), oldComponents.get(key)));
    }

    private static Set<DataComponentType<?>> getComparedKeys(DataComponentMap components) {
        Set<DataComponentType<?>> keys = new HashSet<>(components.keySet());
        keys.remove(CCBDataComponents.GAS_CANISTER_PACK_FLAGS);
        keys.remove(CCBDataComponents.CANISTER_CONTAINER_CONTENTS);
        keys.remove(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES);
        return keys;
    }

    public static boolean isCanisterPackMenuOpened(Player player, ItemStack pack) {
        return player.containerMenu instanceof GasCanisterPackMenu menu && ItemStack.matches(menu.contentHolder, pack);
    }
}
