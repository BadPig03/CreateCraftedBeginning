package net.ty.createcraftedbeginning.mixin.client.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageRenderer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonEntityBehaviour;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonWorldPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = PackageRenderer.class, remap = false)
public abstract class PackageRendererMixin extends EntityRenderer<PackageEntity> {
    private PackageRendererMixin(Context context) {
        super(context);
    }

    @SuppressWarnings("MethodMayBeStatic")
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/box/PackageRenderer;renderBox(Lnet/minecraft/world/entity/Entity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILdev/engine_room/flywheel/lib/model/baked/PartialModel;)V", shift = Shift.BEFORE))
    private void ccb$renderBefore(PackageEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo callback) {
        if (!BalloonEntityBehaviour.isBalloon(entity)) {
            return;
        }

        float scale = BalloonWorldPhysics.of(entity.getBox(), entity.level(), entity.blockPosition()).linearScale();
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
    }

    @SuppressWarnings("MethodMayBeStatic")
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/box/PackageRenderer;renderBox(Lnet/minecraft/world/entity/Entity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILdev/engine_room/flywheel/lib/model/baked/PartialModel;)V", shift = Shift.AFTER))
    private void ccb$renderAfter(PackageEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo callback) {
        if (!BalloonEntityBehaviour.isBalloon(entity)) {
            return;
        }

        poseStack.popPose();
    }
}
