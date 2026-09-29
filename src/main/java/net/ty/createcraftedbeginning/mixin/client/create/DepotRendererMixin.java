package net.ty.createcraftedbeginning.mixin.client.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = DepotRenderer.class, remap = false)
public abstract class DepotRendererMixin {
    @WrapOperation(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"))
    private static void ccb$renderItem(ItemRenderer renderer, ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, BakedModel model, Operation<Void> original, @Local(argsOnly = true) Vec3 itemPosition) {
        Level level = Minecraft.getInstance().level;
        float scale = BalloonRenderHelper.getLinearScale(stack, level, BlockPos.containing(itemPosition));
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
}
