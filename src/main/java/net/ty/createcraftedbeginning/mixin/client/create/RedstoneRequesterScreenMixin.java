package net.ty.createcraftedbeginning.mixin.client.create;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu.SorterProofSlot;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestClientUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestUtils;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
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

    @Unique
    private int ccb$getRequesterSlotAt(double mouseX, double mouseY) {
        double relX = mouseX - getGuiLeft() - 27;
        double relY = mouseY - getGuiTop() - 28;
        if (relX < 0 || relY < 0 || relY >= 16) {
            return -1;
        }

        int slot = (int) (relX / 20);
        int localX = (int) (relX % 20);
        if (localX >= 16 || slot < 0 || slot >= amounts.size()) {
            return -1;
        }
        return slot;
    }

    @ModifyArgs(method = "renderForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private void ccb$renderForeground(Args args) {
        ItemStack stack = args.get(1);
        if (!GasVirtualUtils.isVirtualItem(stack)) {
            return;
        }

        int slotIndex = ((int) args.get(2) - 27 - getGuiLeft()) / 20;
        if (slotIndex < 0 || slotIndex >= amounts.size()) {
            return;
        }

        args.set(4, GasRequestUtils.format(amounts.get(slotIndex), false));
    }

    @Inject(method = "getTooltipFromContainerItem", at = @At("HEAD"), cancellable = true)
    private void ccb$getTooltipFromContainerItem(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        if (!(hoveredSlot instanceof SorterProofSlot)) {
            return;
        }

        int slotIndex = hoveredSlot.getSlotIndex();
        ItemStackHandler inventory = menu.ghostInventory;
        if (slotIndex < 0 || slotIndex >= inventory.getSlots()) {
            return;
        }

        ItemStack stackInSlot = inventory.getStackInSlot(slotIndex);
        if (!GasVirtualUtils.isVirtualItem(stackInSlot)) {
            return;
        }

        List<Component> tooltips = new ArrayList<>();
        tooltips.add(CCBLang.translate("gui.gas_virtual_item.send_item", CCBLang.itemName(stack).add(CCBLang.text(" x" + GasRequestUtils.formatPrecise(amounts.get(slotIndex))))).color(ScrollInput.HEADER_RGB).component());
        tooltips.add(CCBLang.translate("gui.gas_virtual_item.scroll", GasAmountUtils.formatPrecise(GasRequestClientUtils.getScrollStep())).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltips.add(CCBLang.translate("gui.gas_virtual_item.shift_to_scroll", GasAmountUtils.formatPrecise(GasRequestClientUtils.getShiftStep())).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltips.add(CCBLang.translate("gui.gas_virtual_item.alt_to_scroll", GasAmountUtils.formatPrecise(GasRequestClientUtils.getAltStep())).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltips.add(CCBLang.translate("gui.gas_virtual_item.ctrl_to_scroll", GasAmountUtils.formatPrecise(GasRequestClientUtils.getCtrlStep())).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        cir.setReturnValue(tooltips);
    }

    @Inject(method = "renderForeground", at = @At("TAIL"))
    private void ccb$renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!(hoveredSlot instanceof SorterProofSlot)) {
            return;
        }

        int slotIndex = hoveredSlot.getSlotIndex();
        ItemStackHandler inventory = menu.ghostInventory;
        if (slotIndex < 0 || slotIndex >= inventory.getSlots()) {
            return;
        }

        ItemStack carried = menu.getCarried();
        List<ItemStack> virtualItems = GasVirtualUtils.getVirtualItems(carried);
        if (virtualItems.isEmpty()) {
            return;
        }

        ItemStack existing = inventory.getStackInSlot(slotIndex);
        String text;
        if (GasVirtualUtils.isVirtualItem(existing) && !ItemStack.isSameItemSameComponents(existing, virtualItems.getFirst())) {
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

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void ccb$mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        int slot = ccb$getRequesterSlotAt(mouseX, mouseY);
        if (slot == -1) {
            return;
        }

        ItemStack stack = menu.ghostInventory.getStackInSlot(slot);
        if (!GasVirtualUtils.isVirtualItem(stack)) {
            return;
        }

        int current = amounts.get(slot);
        int step = GasRequestClientUtils.getStep(hasAltDown(), hasControlDown(), hasShiftDown());
        if (!hasControlDown() && scrollY > 0 && current == 1 && step > 1) {
            step--;
        }
        int next = Mth.clamp(current + (scrollY >= 0 ? step : -step), 1, Integer.MAX_VALUE);
        amounts.set(slot, next);
        cir.setReturnValue(true);
    }

    @Dynamic
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void ccb$slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType clickType, CallbackInfo ci) {
        if (!GasRequestClientUtils.onSlotClicked(this, menu, slot, mouseButton, clickType)) {
            return;
        }

        ci.cancel();
    }
}
