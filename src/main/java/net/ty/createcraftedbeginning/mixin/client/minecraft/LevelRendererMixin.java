package net.ty.createcraftedbeginning.mixin.client.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LevelRenderer;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender.LaserBeamCoverageRenderer;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender.LaserCloudOcclusion;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=translucent", shift = At.Shift.AFTER))
    private void ccb$renderLevel$beforeTranslucent(CallbackInfo callback) {
        LaserBeamCoverageRenderer.renderBeforeTranslucent();
    }

    @SuppressWarnings("MethodMayBeStatic")
    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V"))
    private void ccb$renderLevel(LevelRenderer renderer, PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, double camX, double camY, double camZ, Operation<Void> original) {
        LaserCloudOcclusion.render(() -> original.call(renderer, poseStack, frustumMatrix, projectionMatrix, partialTick, camX, camY, camZ));
    }
}
