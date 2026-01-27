package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class GasCanisterUtils {
    public static final int COLOR_RED = 0xFFFF5D6C;
    public static final int COLOR_CYAN = 0xFF71C7D5;
    public static final int COLOR_WHITE = 0xFFEFEFEF;

    private GasCanisterUtils() {
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
        newKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CONTENTS);
        newKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES);
        oldKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CONTENTS);
        oldKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES);
        return !newKeys.equals(oldKeys) || !newKeys.stream().allMatch(key -> Objects.equals(newComponents.get(key), oldComponents.get(key)));
    }

    public static boolean isCanisterInjectable(@NotNull ItemStack itemStack, GasStack resource) {
        if (!(itemStack.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return false;
        }

        GasStack gasContent = canisterContents.getGasInTank(0);
        return gasContent.isEmpty() || GasStack.isSameGasSameComponents(gasContent, resource) && !canisterContents.isFull();
    }
}
