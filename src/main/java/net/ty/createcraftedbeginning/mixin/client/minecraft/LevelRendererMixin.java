package net.ty.createcraftedbeginning.mixin.client.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LevelRenderer;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender.LaserCloudOcclusion;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V"))
    private void ccb$renderLevel(LevelRenderer renderer, PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, double camX, double camY, double camZ, Operation<Void> original) {
        LaserCloudOcclusion.render(() -> original.call(renderer, poseStack, frustumMatrix, projectionMatrix, partialTick, camX, camY, camZ));
    }
}
