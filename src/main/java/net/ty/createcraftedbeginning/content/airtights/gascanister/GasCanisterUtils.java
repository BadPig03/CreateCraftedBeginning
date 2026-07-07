package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterContainerContents;
import net.ty.createcraftedbeginning.data.CCBGases;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasCanisterUtils {
    public static final int COLOR_RED = 0xFFFF5D6C;
    public static final int COLOR_CYAN = 0xFF71C7D5;
    public static final int COLOR_WHITE = 0xFFEFEFEF;

    private GasCanisterUtils() {
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

        Set<DataComponentType<?>> newKeys = new HashSet<>(newComponents.keySet());
        Set<DataComponentType<?>> oldKeys = new HashSet<>(oldComponents.keySet());
        newKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CONTENTS);
        newKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES);
        oldKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CONTENTS);
        oldKeys.remove(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES);
        return !newKeys.equals(oldKeys) || !newKeys.stream().allMatch(key -> Objects.equals(newComponents.get(key), oldComponents.get(key)));
    }

    public static boolean canInjectCanister(ItemStack itemStack, GasStack resource) {
        if (!(itemStack.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return false;
        }

        GasStack gasContent = canisterContents.getGasInTank(0);
        return gasContent.isEmpty() || GasStack.isSameGasSameComponents(gasContent, resource) && !canisterContents.isFull();
    }

    public static void displayCustomWarningHint(Player player, String key, Object... args) {
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        player.displayClientMessage(CCBLang.translateDirect(key, args).withStyle(ChatFormatting.RED), true);
        CCBSoundEvents.DENY.playOnServer(level, player.blockPosition(), 1, 1);
    }

    public static List<ItemStack> getAllCanisters() {
        List<ItemStack> items = new ArrayList<>(List.of(new ItemStack(CCBItems.GAS_CANISTER.asItem())));
        CCBGases.GAS_REGISTER.getEntries().forEach(entry -> {
            ItemStack canister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
            if (canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents containerContents) {
                containerContents.fill(0, new GasStack(entry, containerContents.getTankCapacity(0)), GasAction.EXECUTE);
            }
            items.add(canister);
        });
        items.add(new ItemStack(CCBItems.CREATIVE_GAS_CANISTER.asItem()));
        CCBGases.GAS_REGISTER.getEntries().forEach(entry -> {
            ItemStack canister = new ItemStack(CCBItems.CREATIVE_GAS_CANISTER.asItem());
            if (canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents creativeGasCanisterContainerContents) {
                creativeGasCanisterContainerContents.setGasInTank(0, new GasStack(entry, creativeGasCanisterContainerContents.getTankCapacity(0)));
            }
            items.add(canister);
        });
        return items;
    }
}
