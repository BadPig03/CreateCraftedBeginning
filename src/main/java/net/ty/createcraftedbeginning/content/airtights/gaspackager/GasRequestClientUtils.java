package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu.SorterProofSlot;
import com.simibubi.create.foundation.gui.menu.GhostItemSubmitPacket;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.mixin.client.accessor.RedstoneRequesterScreenAccessor;
import net.ty.createcraftedbeginning.mixin.client.accessor.StockKeeperRequestScreenAccessor;
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

    @OnlyIn(Dist.CLIENT)
    public static boolean onSlotClicked(AbstractContainerScreen<?> screen, RedstoneRequesterMenu requesterMenu, @Nullable Slot slot, int mouseButton, ClickType clickType) {
        if (!(screen instanceof RedstoneRequesterScreenAccessor screenAccessor) || !(slot instanceof SorterProofSlot itemHandler)) {
            return false;
        }

        int slotIndex = itemHandler.getSlotIndex();
        ItemStackHandler inventory = requesterMenu.ghostInventory;
        if (slotIndex < 0 || slotIndex >= inventory.getSlots()) {
            return false;
        }

        ItemStack carried = requesterMenu.getCarried();
        ItemStack existing = inventory.getStackInSlot(slotIndex);
        if (GasVirtualUtils.isVirtualItem(existing)) {
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
            submitVirtualItem(screenAccessor, requesterMenu, virtualItems.getFirst(), slotIndex, GasRequestUtils.getScrollStep());
            return true;
        }

        int gasIndex = 0;
        for (int i = slotIndex; i < inventory.getSlots() && gasIndex < virtualItems.size(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                continue;
            }

            submitVirtualItem(screenAccessor, requesterMenu, virtualItems.get(gasIndex), i, GasRequestUtils.getScrollStep());
            gasIndex++;
        }
        return true;
    }

    public static void submitVirtualItem(RedstoneRequesterScreenAccessor screenAccessor, RedstoneRequesterMenu requesterMenu, ItemStack stack, int slotIndex, int amount) {
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

    public static List<Component> getTooltipLines(ItemStack virtualItem, List<Integer> amounts, int slotIndex) {
        List<Component> tooltips = new ArrayList<>();
        tooltips.add(CCBLang.translate("gui.tooltips.gas_virtual_item.send_item", CCBLang.itemName(virtualItem).add(CCBLang.text(" x" + GasRequestUtils.formatPrecise(amounts.get(slotIndex))))).color(ScrollInput.HEADER_RGB).component());
        tooltips.add(CCBLang.translate("gui.tooltips.gas_virtual_item.scroll", GasRequestUtils.getScrollStep()).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltips.add(CCBLang.translate("gui.tooltips.gas_virtual_item.shift_to_scroll", GasRequestUtils.getShiftStep()).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltips.add(CCBLang.translate("gui.tooltips.gas_virtual_item.alt_to_scroll", GasRequestUtils.getAltStep()).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltips.add(CCBLang.translate("gui.tooltips.gas_virtual_item.ctrl_to_scroll", GasRequestUtils.getCtrlStep()).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        return tooltips;
    }

    public static List<Component> getTooltipLines(StockKeeperRequestScreenAccessor accessor, BigItemStack entry, boolean orderHovered) {
        List<Component> tooltips = new ArrayList<>();
        ItemStack virtualItem = entry.stack;
        tooltips.add(CCBLang.itemName(virtualItem).component());
        int available = accessor.getBlockEntity().getLastClientsideStockSnapshotAsSummary().getCountOf(virtualItem);
        if (orderHovered) {
            BigItemStack orderItem = accessor.ccb$getOrderForItem(virtualItem);
            if (orderItem != null && orderItem.count > 0) {
                tooltips.add(CCBLang.translate("gui.tooltips.gas_virtual_item.requested", GasRequestUtils.formatPrecise(orderItem.count)).style(ChatFormatting.DARK_GRAY).component());
            }
        }
        else {
            tooltips.add(CCBLang.translate("gui.tooltips.gas_virtual_item.available", GasRequestUtils.formatPrecise(available)).style(ChatFormatting.DARK_GRAY).component());
        }

        int multiplier = orderHovered ? 1 : 10;
        tooltips.add(CCBLang.translate("gui.tooltips.gas_virtual_item.scroll", GasRequestUtils.getScrollStep() * multiplier).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltips.add(CCBLang.translate("gui.tooltips.gas_virtual_item.shift_to_scroll", GasRequestUtils.getShiftStep() * multiplier).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltips.add(CCBLang.translate("gui.tooltips.gas_virtual_item.alt_to_scroll", GasRequestUtils.getAltStep() * multiplier).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltips.add(CCBLang.translate("gui.tooltips.gas_virtual_item.ctrl_to_scroll", GasRequestUtils.getCtrlStep() * multiplier).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltips.addAll(getExtraTooltips(virtualItem));
        return tooltips;
    }

    public static List<Component> getExtraTooltips(ItemStack virtualItem) {
        if (!GasVirtualUtils.isVirtualItem(virtualItem)) {
            return List.of();
        }

        List<Component> tooltips = new ArrayList<>();
        if (Minecraft.getInstance().options.advancedItemTooltips) {
            tooltips.add(CCBLang.text(GasVirtualUtils.getGasType(virtualItem).getGasType().getResourceLocation().toString()).style(ChatFormatting.DARK_GRAY).component());
        }
        tooltips.add(CCBLang.text(CreateCraftedBeginning.NAME).style(ChatFormatting.BLUE).style(ChatFormatting.ITALIC).component());
        return tooltips;
    }

    private static void resetRequesterSlot(RedstoneRequesterScreenAccessor screenAccessor, RedstoneRequesterMenu requesterMenu, int slotIndex, boolean clear) {
        screenAccessor.ccb$getAmounts().set(slotIndex, 1);
        if (!clear) {
            return;
        }

        requesterMenu.ghostInventory.setStackInSlot(slotIndex, ItemStack.EMPTY);
        CatnipServices.NETWORK.sendToServer(new GhostItemSubmitPacket(ItemStack.EMPTY, slotIndex));
    }
}
