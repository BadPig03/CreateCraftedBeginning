package net.ty.createcraftedbeginning.mixin.client.create;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.ty.createcraftedbeginning.client.stockkeeper.GasFactoryPanelInteraction;
import net.ty.createcraftedbeginning.client.stockkeeper.GasRequestSteps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;
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
    private void ccb$renderInputItem(GuiGraphics graphics, int slot, BigItemStack entry, int mouseX, int mouseY, CallbackInfo callback) {
        if (restocker || craftingActive) {
            return;
        }

        if (!GasFactoryPanelInteraction.renderInput(graphics, font, guiLeft, guiTop, slot, entry, mouseX, mouseY)) {
            return;
        }

        callback.cancel();
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void ccb$mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> callback) {
        if (restocker || craftingActive || scrollY == 0) {
            return;
        }

        boolean control = hasControlDown();
        int step = GasRequestSteps.getStep(hasAltDown(), control, hasShiftDown());
        if (!GasFactoryPanelInteraction.scroll(inputConfig, guiLeft, guiTop, mouseX, mouseY, scrollY, step, control)) {
            return;
        }

        callback.setReturnValue(true);
    }
}
