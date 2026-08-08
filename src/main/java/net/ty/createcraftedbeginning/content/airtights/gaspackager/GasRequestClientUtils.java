package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu.SorterProofSlot;
import com.simibubi.create.foundation.gui.menu.GhostItemSubmitPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.access.RedstoneRequesterScreenAccess;
import net.ty.createcraftedbeginning.platform.access.StockKeeperRequestScreenAccess;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class GasRequestClientUtils {
    private GasRequestClientUtils() {
    }

    public static int getScrollStep() {
        return CCBConfig.client().gasRequestScrollStep.get();
    }

    public static int getAltStep() {
        return CCBConfig.client().gasRequestAltScrollStep.get();
    }

    public static int getCtrlStep() {
        return CCBConfig.client().gasRequestCtrlScrollStep.get();
    }

    public static int getShiftStep() {
        return CCBConfig.client().gasRequestShiftScrollStep.get();
    }

    public static int getStep(boolean alt, boolean ctrl, boolean shift) {
        if (alt) {
            return getAltStep();
        }

        if (ctrl) {
            return getCtrlStep();
        }

        if (shift) {
            return getShiftStep();
        }
        return getScrollStep();
    }

    public static boolean onSlotClicked(AbstractContainerScreen<?> screen, RedstoneRequesterMenu requesterMenu, @Nullable Slot slot, int mouseButton, ClickType clickType) {
        if (!(screen instanceof RedstoneRequesterScreenAccess screenAccessor) || !(slot instanceof SorterProofSlot requestSlot)) {
            return false;
        }

        int slotIndex = requestSlot.getSlotIndex();
        ItemStackHandler inventory = requesterMenu.ghostInventory;
        if (slotIndex < 0 || slotIndex >= inventory.getSlots()) {
            return false;
        }

        ItemStack carried = requesterMenu.getCarried();
        ItemStack existing = inventory.getStackInSlot(slotIndex);
        if (GasVirtualUtils.isVirtualItem(existing)) {
            return handleVirtualSlot(screenAccessor, requesterMenu, carried, slotIndex, clickType);
        }

        boolean rightPickup = clickType == ClickType.PICKUP && mouseButton == InputConstants.MOUSE_BUTTON_RIGHT;
        boolean rightQuickCraft = clickType == ClickType.QUICK_CRAFT && AbstractContainerMenu.getQuickcraftType(mouseButton) == InputConstants.MOUSE_BUTTON_RIGHT;
        if (!rightPickup && !rightQuickCraft) {
            return false;
        }

        if (carried.isEmpty() || !existing.isEmpty()) {
            return false;
        }

        List<ItemStack> virtualItems = GasVirtualUtils.getVirtualItems(carried);
        if (virtualItems.isEmpty()) {
            return false;
        }

        if (rightQuickCraft) {
            submitVirtualItem(screenAccessor, requesterMenu, virtualItems.getFirst(), slotIndex, getScrollStep());
            return true;
        }

        fillRequesterSlots(screenAccessor, requesterMenu, inventory, virtualItems, slotIndex);
        return true;
    }

    private static boolean handleVirtualSlot(RedstoneRequesterScreenAccess screenAccessor, RedstoneRequesterMenu requesterMenu, ItemStack carried, int slotIndex, ClickType clickType) {
        if (clickType == ClickType.CLONE || clickType == ClickType.THROW) {
            return true;
        }

        if (carried.isEmpty()) {
            resetRequesterSlot(screenAccessor, requesterMenu, slotIndex, true);
            return true;
        }

        List<ItemStack> virtualItems = GasVirtualUtils.getVirtualItems(carried);
        if (virtualItems.isEmpty()) {
            resetRequesterSlot(screenAccessor, requesterMenu, slotIndex, false);
            return false;
        }

        submitVirtualItem(screenAccessor, requesterMenu, virtualItems.getFirst(), slotIndex, -1);
        return true;
    }

    private static void fillRequesterSlots(RedstoneRequesterScreenAccess screenAccessor, RedstoneRequesterMenu requesterMenu, ItemStackHandler inventory, List<ItemStack> virtualItems, int firstSlot) {
        int gasIndex = 0;
        for (int slot = firstSlot; slot < inventory.getSlots() && gasIndex < virtualItems.size(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                continue;
            }

            submitVirtualItem(screenAccessor, requesterMenu, virtualItems.get(gasIndex), slot, getScrollStep());
            gasIndex++;
        }
    }

    public static void submitVirtualItem(RedstoneRequesterScreenAccess screenAccessor, RedstoneRequesterMenu requesterMenu, ItemStack stack, int slotIndex, int amount) {
        List<Integer> amounts = screenAccessor.ccb$getAmounts();
        if (slotIndex < 0 || slotIndex >= amounts.size()) {
            return;
        }

        ItemStack submitted = stack.copyWithCount(1);
        requesterMenu.ghostInventory.setStackInSlot(slotIndex, submitted);
        if (amount > 0) {
            amounts.set(slotIndex, amount);
        }

        CatnipServices.NETWORK.sendToServer(new GhostItemSubmitPacket(submitted, slotIndex));
    }

    public static List<Component> getTooltipLines(StockKeeperRequestScreenAccess accessor, BigItemStack entry, boolean orderHovered) {
        List<Component> tooltips = new ArrayList<>();
        ItemStack virtualItem = entry.stack;
        tooltips.add(CCBLang.itemName(virtualItem).component());

        int available = accessor.getBlockEntity().getLastClientsideStockSnapshotAsSummary().getCountOf(virtualItem);
        if (orderHovered) {
            BigItemStack orderItem = accessor.ccb$getOrderForItem(virtualItem);
            if (orderItem != null && orderItem.count > 0) {
                tooltips.add(CCBLang.translate("gui.gas_virtual_item.requested", GasRequestUtils.formatPrecise(orderItem.count)).style(ChatFormatting.DARK_GRAY).component());
            }
        }
        else {
            tooltips.add(CCBLang.translate("gui.gas_virtual_item.available", GasRequestUtils.formatPrecise(available)).style(ChatFormatting.DARK_GRAY).component());
        }

        long multiplier = orderHovered ? 1 : 10;
        addScrollTooltip(tooltips, "gui.gas_virtual_item.scroll", getScrollStep() * multiplier);
        addScrollTooltip(tooltips, "gui.gas_virtual_item.shift_to_scroll", getShiftStep() * multiplier);
        addScrollTooltip(tooltips, "gui.gas_virtual_item.alt_to_scroll", getAltStep() * multiplier);
        addScrollTooltip(tooltips, "gui.gas_virtual_item.ctrl_to_scroll", getCtrlStep() * multiplier);
        tooltips.addAll(getExtraTooltips(virtualItem));
        return tooltips;
    }

    public static List<Component> getExtraTooltips(ItemStack virtualItem) {
        if (!GasVirtualUtils.isVirtualItem(virtualItem)) {
            return List.of();
        }

        List<Component> tooltips = new ArrayList<>();
        if (Minecraft.getInstance().options.advancedItemTooltips) {
            String gasId = GasVirtualUtils.getGasType(virtualItem).getGasType().getResourceLocation().toString();
            tooltips.add(CCBLang.text(gasId).style(ChatFormatting.DARK_GRAY).component());
        }
        tooltips.add(CCBLang.text(CCBAPI.NAME).style(ChatFormatting.BLUE).style(ChatFormatting.ITALIC).component());
        return tooltips;
    }

    private static void addScrollTooltip(List<Component> tooltips, String key, long amount) {
        tooltips.add(CCBLang.translate(key, GasAmountUtils.formatPrecise(amount)).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
    }

    private static void resetRequesterSlot(RedstoneRequesterScreenAccess screenAccessor, RedstoneRequesterMenu requesterMenu, int slotIndex, boolean clear) {
        screenAccessor.ccb$getAmounts().set(slotIndex, 1);
        if (!clear) {
            return;
        }

        requesterMenu.ghostInventory.setStackInSlot(slotIndex, ItemStack.EMPTY);
        CatnipServices.NETWORK.sendToServer(new GhostItemSubmitPacket(ItemStack.EMPTY, slotIndex));
    }
}
