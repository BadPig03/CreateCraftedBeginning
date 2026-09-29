package net.ty.createcraftedbeginning.mixin.client.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportRenderer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.balloon.FrogBalloonRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = FrogportRenderer.class, remap = false)
public abstract class FrogportRendererMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @Inject(method = "renderPackage", at = @At("HEAD"), cancellable = true)
    private void ccb$renderPackage(FrogportBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, Vec3 diff, float scale, float itemDistance, CallbackInfo callback) {
        if (!FrogBalloonRenderer.render(blockEntity, poseStack, buffer, light, overlay, diff, scale, itemDistance)) {
            return;
        }

        callback.cancel();
    }
}
