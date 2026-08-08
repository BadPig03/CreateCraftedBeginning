package net.ty.createcraftedbeginning.mixin.client.create;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestClientUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestUtils;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = FactoryPanelScreen.class, remap = false)
public abstract class FactoryPanelScreenMixin extends AbstractSimiScreen {
    @Shadow
    private boolean restocker;

    @Shadow
    private boolean craftingActive;

    @Shadow
    private List<BigItemStack> inputConfig;

    @Inject(method = "renderInputItem", at = @At("HEAD"), cancellable = true)
    private void ccb$renderInputItem(GuiGraphics graphics, int slot, BigItemStack entry, int mouseX, int mouseY, CallbackInfo ci) {
        if (restocker || craftingActive) {
            return;
        }

        ItemStack item = entry.stack;
        if (item.isEmpty() || !GasVirtualUtils.isVirtualItem(item)) {
            return;
        }

        int x = guiLeft + 68 + slot % 3 * 20;
        int y = guiTop + 28 + slot / 3 * 20;
        int count = entry.count;
        graphics.renderItem(item, x, y);
        graphics.renderItemDecorations(font, item, x, y, GasRequestUtils.format(count, false));
        if (mouseX >= x - 2 && mouseX < x + 18 && mouseY >= y - 2 && mouseY < y + 18) {
            List<Component> tooltips = new ArrayList<>();
            tooltips.add(CCBLang.translate("gui.gas_factory_gauge.sending_item", CCBLang.itemName(item).add(CCBLang.text(" x" + GasRequestUtils.formatPrecise(count)))).color(ScrollInput.HEADER_RGB).component());
            tooltips.add(CCBLang.translate("gui.gas_virtual_item.scroll", GasAmountUtils.formatPrecise(GasRequestClientUtils.getScrollStep())).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
            tooltips.add(CCBLang.translate("gui.gas_virtual_item.shift_to_scroll", GasAmountUtils.formatPrecise(GasRequestClientUtils.getShiftStep())).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
            tooltips.add(CCBLang.translate("gui.gas_virtual_item.alt_to_scroll", GasAmountUtils.formatPrecise(GasRequestClientUtils.getAltStep())).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
            tooltips.add(CCBLang.translate("gui.gas_virtual_item.ctrl_to_scroll", GasAmountUtils.formatPrecise(GasRequestClientUtils.getCtrlStep())).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
            tooltips.add(CCBLang.translate("gui.gas_factory_gauge.left_click_disconnect").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
            graphics.renderComponentTooltip(font, tooltips, mouseX, mouseY);
        }
        ci.cancel();
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void ccb$mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (restocker || craftingActive || scrollY == 0) {
            return;
        }

        for (int i = 0; i < inputConfig.size(); i++) {
            int inputX = guiLeft + 68 + i % 3 * 20;
            int inputY = guiTop + 26 + i / 3 * 20;
            if (mouseX < inputX || mouseX >= inputX + 16 || mouseY < inputY || mouseY >= inputY + 16) {
                continue;
            }

            BigItemStack entry = inputConfig.get(i);
            if (entry.stack.isEmpty() || !GasVirtualUtils.isVirtualItem(entry.stack)) {
                return;
            }

            int step = GasRequestClientUtils.getStep(hasAltDown(), hasControlDown(), hasShiftDown());
            if (!hasControlDown() && scrollY > 0 && entry.count == 1 && step > 1) {
                step--;
            }
            int candidate = entry.count + (scrollY > 0 ? step : -step);
            entry.count = Math.clamp(candidate, 1, GasFactoryGaugeBehaviour.MAX_TARGET_AMOUNT);
            cir.setReturnValue(true);
            return;
        }
    }
}
