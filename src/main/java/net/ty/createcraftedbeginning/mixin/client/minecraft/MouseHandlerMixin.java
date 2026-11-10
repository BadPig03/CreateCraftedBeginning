package net.ty.createcraftedbeginning.mixin.client.minecraft;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.MouseHandler;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorControls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    private double accumulatedDX;
    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void ccb$turnPlayer(double movementTime, CallbackInfo callback) {
        if (!AmethystMirrorControls.dragMouse(accumulatedDX, accumulatedDY)) {
            return;
        }

        callback.cancel();
    }
}
