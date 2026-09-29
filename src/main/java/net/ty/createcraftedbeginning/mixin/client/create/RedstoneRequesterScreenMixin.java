package net.ty.createcraftedbeginning.mixin.client.create;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu.SorterProofSlot;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.client.stockkeeper.GasRequestSteps;
import net.ty.createcraftedbeginning.client.stockkeeper.GasRequestTooltips;
import net.ty.createcraftedbeginning.client.stockkeeper.RedstoneRequesterInteraction;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestFormat;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = RedstoneRequesterScreen.class, remap = false)
public abstract class RedstoneRequesterScreenMixin extends AbstractSimiContainerScreen<RedstoneRequesterMenu> {
    @Shadow
    @Final
    private List<Integer> amounts;

    private RedstoneRequesterScreenMixin(RedstoneRequesterMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @ModifyArgs(method = "renderForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private void ccb$renderForeground(Args args) {
        ItemStack stack = args.get(1);
        if (!VirtualGasItems.isVirtualItem(stack)) {
            return;
        }

        int slotIndex = ((int) args.get(2) - 27 - getGuiLeft()) / 20;
        if (slotIndex < 0 || slotIndex >= amounts.size()) {
            return;
        }

        args.set(4, GasRequestFormat.formatDecoration(amounts.get(slotIndex), false));
    }

    @Inject(method = "renderForeground", at = @At("TAIL"))
    private void ccb$renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo callback) {
        if (!(hoveredSlot instanceof SorterProofSlot)) {
            return;
        }

        int slotIndex = hoveredSlot.getSlotIndex();
        ItemStackHandler inventory = menu.ghostInventory;
        if (slotIndex < 0 || slotIndex >= inventory.getSlots()) {
            return;
        }

        ItemStack carried = menu.getCarried();
        List<ItemStack> virtualItems = VirtualGasItems.createVirtualItems(carried);
        if (virtualItems.isEmpty()) {
            return;
        }

        ItemStack existing = inventory.getStackInSlot(slotIndex);
        String text;
        if (VirtualGasItems.isVirtualItem(existing) && !ItemStack.isSameItemSameComponents(existing, virtualItems.getFirst())) {
            text = "gui.gas_virtual_item.replace_gas_types";
        }
        else if (existing.isEmpty()) {
            text = "gui.gas_virtual_item.set_gas_types";
        }
        else {
            return;
        }

        graphics.renderComponentTooltip(font, List.of(CCBLang.translateDirect(text).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)), mouseX, mouseY);
    }

    @Inject(method = "getTooltipFromContainerItem", at = @At("HEAD"), cancellable = true)
    private void ccb$getTooltipFromContainerItem(ItemStack stack, CallbackInfoReturnable<List<Component>> callback) {
        if (!(hoveredSlot instanceof SorterProofSlot)) {
            return;
        }

        int slotIndex = hoveredSlot.getSlotIndex();
        ItemStackHandler inventory = menu.ghostInventory;
        if (slotIndex < 0 || slotIndex >= inventory.getSlots()) {
            return;
        }

        ItemStack stackInSlot = inventory.getStackInSlot(slotIndex);
        if (!VirtualGasItems.isVirtualItem(stackInSlot)) {
            return;
        }

        callback.setReturnValue(GasRequestTooltips.getRequesterAmount(stack, amounts.get(slotIndex)));
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void ccb$mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> callback) {
        boolean control = hasControlDown();
        int step = GasRequestSteps.getStep(hasAltDown(), control, hasShiftDown());
        if (!new RedstoneRequesterInteraction(this, menu).scroll(amounts, getGuiLeft(), getGuiTop(), mouseX, mouseY, scrollY, step, control)) {
            return;
        }

        callback.setReturnValue(true);
    }

    @Dynamic
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void ccb$slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType clickType, CallbackInfo callback) {
        if (!new RedstoneRequesterInteraction(this, menu).onSlotClicked(slot, mouseButton, clickType)) {
            return;
        }

        callback.cancel();
    }
}
