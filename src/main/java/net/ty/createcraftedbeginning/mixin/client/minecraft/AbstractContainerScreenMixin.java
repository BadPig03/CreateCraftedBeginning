package net.ty.createcraftedbeginning.mixin.client.minecraft;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.ty.createcraftedbeginning.client.stockkeeper.RedstoneRequesterInteraction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> {
    @Shadow
    protected T menu;

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void ccb$slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType clickType, CallbackInfo callback) {
        if (!(menu instanceof RedstoneRequesterMenu requesterMenu)) {
            return;
        }

        if (!new RedstoneRequesterInteraction((AbstractContainerScreen<?>) (Object) this, requesterMenu).onSlotClicked(slot, mouseButton, clickType)) {
            return;
        }

        callback.cancel();
    }
}
