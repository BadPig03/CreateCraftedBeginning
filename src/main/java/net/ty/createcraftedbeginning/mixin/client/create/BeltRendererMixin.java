package net.ty.createcraftedbeginning.mixin.client.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BeltRenderer.class, remap = false)
public abstract class BeltRendererMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @WrapOperation(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"))
    private void ccb$renderItem(ItemRenderer renderer, ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, BakedModel model, Operation<Void> original, @Local(argsOnly = true) BeltBlockEntity belt, @Local(argsOnly = true) float partialTicks, @Local(argsOnly = true) TransportedItemStack transported) {
        float scale = BalloonRenderHelper.getBeltLinearScale(stack, belt, transported, partialTicks);
        if (scale == 1) {
            original.call(renderer, stack, displayContext, leftHand, poseStack, buffer, light, overlay, model);
            return;
        }

        poseStack.pushPose();
        BalloonRenderHelper.applyBottomAnchoredFixedItemScale(poseStack, scale);
        try {
            original.call(renderer, stack, displayContext, leftHand, poseStack, buffer, light, overlay, model);
        }
        finally {
            poseStack.popPose();
        }
    }

    @SuppressWarnings("MethodMayBeStatic")
    @WrapOperation(method = "renderItem", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/render/ShadowRenderHelper;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;FF)V"))
    private void ccb$renderShadow(PoseStack poseStack, MultiBufferSource buffer, float opacity, float radius, Operation<Void> original, @Local(argsOnly = true) BeltBlockEntity belt, @Local(argsOnly = true) float partialTicks, @Local(argsOnly = true) TransportedItemStack transported) {
        float scale = BalloonRenderHelper.getBeltLinearScale(transported.stack, belt, transported, partialTicks);
        original.call(poseStack, buffer, opacity, radius * scale);
    }
}
