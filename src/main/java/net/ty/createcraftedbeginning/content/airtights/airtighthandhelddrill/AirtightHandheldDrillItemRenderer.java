package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class AirtightHandheldDrillItemRenderer extends CustomRenderedItemModelRenderer {
    private AirtightHandheldDrillItemRenderer() {
    }

    @SubscribeEvent
    private static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(SimpleCustomRenderer.create(CCBItems.AIRTIGHT_HANDHELD_DRILL.asItem(), new AirtightHandheldDrillItemRenderer()), CCBItems.AIRTIGHT_HANDHELD_DRILL.asItem());
    }

    @Override
    protected void render(ItemStack drill, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        renderer.render(model.getOriginalModel(), light);
        BakedModel drillFront = CCBPartialModels.AIRTIGHT_HANDHELD_DRILL_FRONT.get();
        float rotationAngle = AirtightHandheldDrillRenderHandler.INSTANCE.getRotationAngle(drill, AnimationTickHolder.getPartialTicks());

        poseStack.pushPose();

        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationAngle));
        renderer.render(drillFront, light);

        poseStack.popPose();
    }
}
