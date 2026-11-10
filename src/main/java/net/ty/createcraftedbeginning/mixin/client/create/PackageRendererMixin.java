package net.ty.createcraftedbeginning.mixin.client.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonEntityBehaviour;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonWorldPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = PackageRenderer.class, remap = false)
public abstract class PackageRendererMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/box/PackageRenderer;renderBox(Lnet/minecraft/world/entity/Entity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILdev/engine_room/flywheel/lib/model/baked/PartialModel;)V"))
    private void ccb$render(Entity entity, float yaw, PoseStack poseStack, MultiBufferSource buffer, int light, PartialModel model, Operation<Void> original) {
        if (!(entity instanceof PackageEntity packageEntity) || !BalloonEntityBehaviour.isBalloon(packageEntity)) {
            original.call(entity, yaw, poseStack, buffer, light, model);
            return;
        }

        float scale = BalloonWorldPhysics.of(packageEntity.getBox(), packageEntity.level(), packageEntity.blockPosition()).linearScale();
        poseStack.pushPose();
        try {
            poseStack.scale(scale, scale, scale);
            original.call(entity, yaw, poseStack, buffer, light, model);
        }
        finally {
            poseStack.popPose();
        }
    }
}
