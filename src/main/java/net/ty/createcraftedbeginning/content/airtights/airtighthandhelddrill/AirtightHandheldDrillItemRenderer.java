package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class AirtightHandheldDrillItemRenderer extends CustomRenderedItemModelRenderer {
    private final Map<ItemStack, Float> rotationAngles = new WeakHashMap<>();

    private AirtightHandheldDrillItemRenderer() {
    }

    @SubscribeEvent
    private static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(SimpleCustomRenderer.create(CCBItems.AIRTIGHT_HANDHELD_DRILL.asItem(), new AirtightHandheldDrillItemRenderer()), CCBItems.AIRTIGHT_HANDHELD_DRILL.asItem());
    }

    @Override
    protected void render(ItemStack drill, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        renderer.render(model.getOriginalModel(), light);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        BakedModel drillFront = CCBPartialModels.AIRTIGHT_HANDHELD_DRILL_FRONT.get();
        float rotationAngle = rotationAngles.getOrDefault(drill, 0.0f);
        boolean isDrillHeld = player.getMainHandItem() == drill || player.getOffhandItem() == drill;
        if (isDrillHeld) {
            float rotationSpeed = AirtightHandheldDrillRenderHandler.INSTANCE.getAnimation(AnimationTickHolder.getPartialTicks());
            rotationAngle = (rotationAngle + rotationSpeed) % 360;
            rotationAngles.put(drill, rotationAngle);
        }

        ms.pushPose();
        ms.mulPose(Axis.ZP.rotationDegrees(rotationAngle));
        renderer.render(drillFront, light);
        ms.popPose();
    }
}
