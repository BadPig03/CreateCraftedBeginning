package net.ty.createcraftedbeginning.client.stockkeeper;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu.SorterProofSlot;
import com.simibubi.create.foundation.gui.menu.GhostItemSubmitPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestAmounts;
import net.ty.createcraftedbeginning.platform.client.RequestScreenBridge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class RedstoneRequesterInteraction {
    private final AbstractContainerScreen<?> screen;
    private final RedstoneRequesterMenu requesterMenu;

    public RedstoneRequesterInteraction(AbstractContainerScreen<?> screen, RedstoneRequesterMenu requesterMenu) {
        this.screen = screen;
        this.requesterMenu = requesterMenu;
    }

    public static void submitVirtualItem(AbstractContainerScreen<?> screen, RedstoneRequesterMenu requesterMenu, ItemStack stack, int slotIndex, int amount) {
        if (!RequestScreenBridge.hasRequesterAmountSlot(screen, slotIndex)) {
            return;
        }

        ItemStack submittedItem = stack.copyWithCount(1);
        requesterMenu.ghostInventory.setStackInSlot(slotIndex, submittedItem);
        if (amount > 0) {
            RequestScreenBridge.setRequesterAmount(screen, slotIndex, amount);
        }

        CatnipServices.NETWORK.sendToServer(new GhostItemSubmitPacket(submittedItem, slotIndex));
    }

    public boolean onSlotClicked(@Nullable Slot slot, int mouseButton, ClickType clickType) {
        if (!RequestScreenBridge.supportsRequesterAmounts(screen) || !(slot instanceof SorterProofSlot requestSlot)) {
            return false;
        }

        int slotIndex = requestSlot.getSlotIndex();
        ItemStackHandler inventory = requesterMenu.ghostInventory;
        if (slotIndex < 0 || slotIndex >= inventory.getSlots()) {
            return false;
        }

        ItemStack carriedStack = requesterMenu.getCarried();
        ItemStack currentStack = inventory.getStackInSlot(slotIndex);
        if (VirtualGasItems.isVirtualItem(currentStack)) {
            return handleVirtualSlot(carriedStack, slotIndex, clickType);
        }

        boolean isRightClickPickup = clickType == ClickType.PICKUP && mouseButton == InputConstants.MOUSE_BUTTON_RIGHT;
        boolean isRightQuickCraft = clickType == ClickType.QUICK_CRAFT && AbstractContainerMenu.getQuickcraftType(mouseButton) == InputConstants.MOUSE_BUTTON_RIGHT;
        if (!isRightClickPickup && !isRightQuickCraft) {
            return false;
        }

        if (carriedStack.isEmpty() || !currentStack.isEmpty()) {
            return false;
        }

        List<ItemStack> virtualGasItems = VirtualGasItems.createVirtualItems(carriedStack);
        if (virtualGasItems.isEmpty()) {
            return false;
        }

        if (isRightQuickCraft) {
            submitVirtualItem(screen, requesterMenu, virtualGasItems.getFirst(), slotIndex, GasRequestSteps.getScrollStep());
            return true;
        }

        fillRequesterSlots(inventory, virtualGasItems, slotIndex);
        return true;
    }

    public boolean scroll(List<Integer> amounts, int left, int top, double mouseX, double mouseY, double direction, int step, boolean control) {
        double relX = mouseX - left - 27;
        double relY = mouseY - top - 28;
        if (relX < 0 || relY < 0 || relY >= 16) {
            return false;
        }

        int slot = (int) (relX / 20);
        int localX = (int) (relX % 20);
        if (localX >= 16 || slot < 0 || slot >= amounts.size() || !VirtualGasItems.isVirtualItem(requesterMenu.ghostInventory.getStackInSlot(slot))) {
            return false;
        }

        amounts.set(slot, GasRequestAmounts.scroll(amounts.get(slot), step, direction, control, Integer.MAX_VALUE));
        return true;
    }

    private boolean handleVirtualSlot(ItemStack carriedStack, int slotIndex, ClickType clickType) {
        if (clickType == ClickType.CLONE || clickType == ClickType.THROW) {
            return true;
        }

        if (carriedStack.isEmpty()) {
            resetRequesterSlot(slotIndex, true);
            return true;
        }

        List<ItemStack> virtualGasItems = VirtualGasItems.createVirtualItems(carriedStack);
        if (virtualGasItems.isEmpty()) {
            resetRequesterSlot(slotIndex, false);
            return false;
        }

        submitVirtualItem(screen, requesterMenu, virtualGasItems.getFirst(), slotIndex, -1);
        return true;
    }

    private void fillRequesterSlots(ItemStackHandler inventory, List<ItemStack> virtualGasItems, int firstSlot) {
        int virtualItemIndex = 0;
        for (int targetSlot = firstSlot; targetSlot < inventory.getSlots() && virtualItemIndex < virtualGasItems.size(); targetSlot++) {
            if (!inventory.getStackInSlot(targetSlot).isEmpty()) {
                continue;
            }

            submitVirtualItem(screen, requesterMenu, virtualGasItems.get(virtualItemIndex), targetSlot, GasRequestSteps.getScrollStep());
            virtualItemIndex++;
        }
    }

    private void resetRequesterSlot(int slotIndex, boolean shouldClear) {
        if (!RequestScreenBridge.setRequesterAmount(screen, slotIndex, 1) || !shouldClear) {
            return;
        }

        requesterMenu.ghostInventory.setStackInSlot(slotIndex, ItemStack.EMPTY);
        CatnipServices.NETWORK.sendToServer(new GhostItemSubmitPacket(ItemStack.EMPTY, slotIndex));
    }
}
