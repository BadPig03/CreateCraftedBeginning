package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.CreateCraftedBeginningClient;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID, value = Dist.CLIENT)
public class AirtightHandheldDrillItemRenderer extends CustomRenderedItemModelRenderer {
    private final Map<ItemStack, Float> rotationAngles = new WeakHashMap<>();

    @SubscribeEvent
    public static void register(@NotNull RegisterClientExtensionsEvent event) {
        event.registerItem(SimpleCustomRenderer.create(CCBItems.AIRTIGHT_HANDHELD_DRILL.asItem(), new AirtightHandheldDrillItemRenderer()), CCBItems.AIRTIGHT_HANDHELD_DRILL.asItem());
    }

    @Override
    protected void render(ItemStack drill, @NotNull CustomRenderedItemModel model, @NotNull PartialItemModelRenderer renderer, ItemDisplayContext transformType, @NotNull PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        renderer.render(model.getOriginalModel(), light);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        BakedModel drillFrontModel = CCBPartialModels.AIRTIGHT_HANDHELD_DRILL_FRONT.get();
        float angle = rotationAngles.getOrDefault(drill, 0.0f);
        if (player.getMainHandItem() != drill && player.getOffhandItem() != drill) {
            ms.pushPose();
            ms.mulPose(Axis.ZP.rotationDegrees(angle));
            renderer.render(drillFrontModel, light);
            ms.popPose();
            return;
        }

        float speed = CreateCraftedBeginningClient.AIRTIGHT_HAND_DRILL_RENDER_HANDLER.getAnimation(AnimationTickHolder.getPartialTicks());
        angle = (angle + speed) % 360;
        rotationAngles.put(drill, angle);

        ms.pushPose();
        ms.mulPose(Axis.ZP.rotationDegrees(angle));
        renderer.render(drillFrontModel, light);
        ms.popPose();
    }
}
