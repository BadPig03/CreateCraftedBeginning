package net.ty.createcraftedbeginning.mixin.client.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRenderer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.ty.createcraftedbeginning.content.airtights.balloon.ChainBalloonRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = ChainConveyorRenderer.class, remap = false)
public abstract class ChainConveyorRendererMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @Inject(method = "renderBox", at = @At("HEAD"), cancellable = true)
    private void ccb$renderBox(ChainConveyorBlockEntity conveyor, PoseStack poseStack, MultiBufferSource buffer, int overlay, BlockPos pos, ChainConveyorPackage box, float partialTicks, CallbackInfo callback) {
        if (!ChainBalloonRenderer.render(conveyor, poseStack, buffer, overlay, pos, box, partialTicks)) {
            return;
        }

        callback.cancel();
    }
}
