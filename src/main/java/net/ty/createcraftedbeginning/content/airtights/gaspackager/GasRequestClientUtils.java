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
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
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

        ItemStack carriedStack = requesterMenu.getCarried();
        ItemStack currentStack = inventory.getStackInSlot(slotIndex);
        if (GasVirtualUtils.isVirtualItem(currentStack)) {
            return handleVirtualSlot(screenAccessor, requesterMenu, carriedStack, slotIndex, clickType);
        }

        boolean isRightClickPickup = clickType == ClickType.PICKUP && mouseButton == InputConstants.MOUSE_BUTTON_RIGHT;
        boolean isRightQuickCraft = clickType == ClickType.QUICK_CRAFT && AbstractContainerMenu.getQuickcraftType(mouseButton) == InputConstants.MOUSE_BUTTON_RIGHT;
        if (!isRightClickPickup && !isRightQuickCraft) {
            return false;
        }

        if (carriedStack.isEmpty() || !currentStack.isEmpty()) {
            return false;
        }

        List<ItemStack> virtualGasItems = GasVirtualUtils.getVirtualItems(carriedStack);
        if (virtualGasItems.isEmpty()) {
            return false;
        }

        if (isRightQuickCraft) {
            submitVirtualItem(screenAccessor, requesterMenu, virtualGasItems.getFirst(), slotIndex, getScrollStep());
            return true;
        }

        fillRequesterSlots(screenAccessor, requesterMenu, inventory, virtualGasItems, slotIndex);
        return true;
    }

    private static boolean handleVirtualSlot(RedstoneRequesterScreenAccess screenAccessor, RedstoneRequesterMenu requesterMenu, ItemStack carriedStack, int slotIndex, ClickType clickType) {
        if (clickType == ClickType.CLONE || clickType == ClickType.THROW) {
            return true;
        }

        if (carriedStack.isEmpty()) {
            resetRequesterSlot(screenAccessor, requesterMenu, slotIndex, true);
            return true;
        }

        List<ItemStack> virtualGasItems = GasVirtualUtils.getVirtualItems(carriedStack);
        if (virtualGasItems.isEmpty()) {
            resetRequesterSlot(screenAccessor, requesterMenu, slotIndex, false);
            return false;
        }

        submitVirtualItem(screenAccessor, requesterMenu, virtualGasItems.getFirst(), slotIndex, -1);
        return true;
    }

    private static void fillRequesterSlots(RedstoneRequesterScreenAccess screenAccessor, RedstoneRequesterMenu requesterMenu, ItemStackHandler inventory, List<ItemStack> virtualGasItems, int firstSlot) {
        int virtualItemIndex = 0;
        for (int targetSlot = firstSlot; targetSlot < inventory.getSlots() && virtualItemIndex < virtualGasItems.size(); targetSlot++) {
            if (!inventory.getStackInSlot(targetSlot).isEmpty()) {
                continue;
            }

            submitVirtualItem(screenAccessor, requesterMenu, virtualGasItems.get(virtualItemIndex), targetSlot, getScrollStep());
            virtualItemIndex++;
        }
    }

    public static void submitVirtualItem(RedstoneRequesterScreenAccess screenAccessor, RedstoneRequesterMenu requesterMenu, ItemStack stack, int slotIndex, int amount) {
        List<Integer> requestedAmounts = screenAccessor.ccb$getAmounts();
        if (slotIndex < 0 || slotIndex >= requestedAmounts.size()) {
            return;
        }

        ItemStack submittedItem = stack.copyWithCount(1);
        requesterMenu.ghostInventory.setStackInSlot(slotIndex, submittedItem);
        if (amount > 0) {
            requestedAmounts.set(slotIndex, amount);
        }

        CatnipServices.NETWORK.sendToServer(new GhostItemSubmitPacket(submittedItem, slotIndex));
    }

    public static List<Component> getTooltipLines(StockKeeperRequestScreenAccess accessor, BigItemStack entry, boolean orderHovered) {
        List<Component> tooltipLines = new ArrayList<>();
        ItemStack virtualItem = entry.stack;
        tooltipLines.add(CCBLang.itemName(virtualItem).component());

        int availableAmount = accessor.ccb$getBlockEntity().getLastClientsideStockSnapshotAsSummary().getCountOf(virtualItem);
        if (orderHovered) {
            BigItemStack requestedOrderItem = accessor.ccb$getOrderForItem(virtualItem);
            if (requestedOrderItem != null && requestedOrderItem.count > 0) {
                tooltipLines.add(CCBLang.translate("gui.gas_virtual_item.requested", GasRequestUtils.formatPrecise(requestedOrderItem.count)).style(ChatFormatting.DARK_GRAY).component());
            }
        }
        else {
            tooltipLines.add(CCBLang.translate("gui.gas_virtual_item.available", GasRequestUtils.formatPrecise(availableAmount)).style(ChatFormatting.DARK_GRAY).component());
        }

        long scrollMultiplier = orderHovered ? 1 : 10;
        addScrollTooltip(tooltipLines, "gui.gas_virtual_item.scroll", getScrollStep() * scrollMultiplier);
        addScrollTooltip(tooltipLines, "gui.gas_virtual_item.shift_to_scroll", getShiftStep() * scrollMultiplier);
        addScrollTooltip(tooltipLines, "gui.gas_virtual_item.alt_to_scroll", getAltStep() * scrollMultiplier);
        addScrollTooltip(tooltipLines, "gui.gas_virtual_item.ctrl_to_scroll", getCtrlStep() * scrollMultiplier);
        tooltipLines.addAll(getExtraTooltips(virtualItem));
        return tooltipLines;
    }

    public static List<Component> getExtraTooltips(ItemStack virtualItem) {
        if (!GasVirtualUtils.isVirtualItem(virtualItem)) {
            return List.of();
        }

        List<Component> tooltipLines = new ArrayList<>();
        if (Minecraft.getInstance().options.advancedItemTooltips) {
            String gasId = GasVirtualUtils.getGasType(virtualItem).getGasType().getResourceLocation().toString();
            tooltipLines.add(CCBLang.text(gasId).style(ChatFormatting.DARK_GRAY).component());
        }
        tooltipLines.add(CCBLang.text(CCBAPI.NAME).style(ChatFormatting.BLUE).style(ChatFormatting.ITALIC).component());
        return tooltipLines;
    }

    private static void addScrollTooltip(List<Component> tooltipLines, String key, long amount) {
        tooltipLines.add(CCBLang.translate(key, GasAmounts.formatPrecise(amount)).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
    }

    private static void resetRequesterSlot(RedstoneRequesterScreenAccess screenAccessor, RedstoneRequesterMenu requesterMenu, int slotIndex, boolean shouldClear) {
        screenAccessor.ccb$getAmounts().set(slotIndex, 1);
        if (!shouldClear) {
            return;
        }

        requesterMenu.ghostInventory.setStackInSlot(slotIndex, ItemStack.EMPTY);
        CatnipServices.NETWORK.sendToServer(new GhostItemSubmitPacket(ItemStack.EMPTY, slotIndex));
    }
}
