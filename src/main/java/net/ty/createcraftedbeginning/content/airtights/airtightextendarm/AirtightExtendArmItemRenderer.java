package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
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

@EventBusSubscriber(value = Dist.CLIENT, modid = CreateCraftedBeginning.MOD_ID)
public class AirtightExtendArmItemRenderer extends CustomRenderedItemModelRenderer {
    private static final PartialModel COGS = CCBPartialModels.AIRTIGHT_EXTEND_ARM_COGS;
    private static final PartialModel SPRING = CCBPartialModels.AIRTIGHT_EXTEND_ARM_SPRING;
    private static final PartialModel SPRING_CAP = CCBPartialModels.AIRTIGHT_EXTEND_ARM_SPRING_CAP;
    private static final PartialModel POINTING = CCBPartialModels.AIRTIGHT_EXTEND_ARM_POINTING;

    @SubscribeEvent
    public static void register(@NotNull RegisterClientExtensionsEvent event) {
        event.registerItem(SimpleCustomRenderer.create(CCBItems.AIRTIGHT_EXTEND_ARM.asItem(), new AirtightExtendArmItemRenderer()), CCBItems.AIRTIGHT_EXTEND_ARM.asItem());
    }

    @Override
    protected void render(ItemStack arm, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, @NotNull PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        PoseTransformStack transformStack = TransformStack.of(ms);
        AirtightExtendArmRenderHandler renderHandler = CreateCraftedBeginningClient.AIRTIGHT_EXTEND_ARM_RENDER_HANDLER;
        float animation;
        boolean shouldRender = transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        if (shouldRender) {
            animation = renderHandler.getAnimation(AnimationTickHolder.getPartialTicks());
        }
        else {
            animation = 0.0f;
        }
        renderer.renderSolid(model.getOriginalModel(), light);

        ms.pushPose();
        ms.translate(0, 0, -0.625 - animation * 1.125);
        ms.scale(1 - animation * 0.125f, 1 - animation * 0.125f, 1 + animation * 9.0f);
        renderer.renderSolid(SPRING.get(), light);
        ms.popPose();

        for (int i = 0; i < 4; i++) {
            ms.pushPose();
            ms.translate(0, 0, -0.7375 + 0.075 * i - animation * 0.5625 * (3.5 - i));
            ms.scale(1 - animation * 0.125f, 1 - animation * 0.125f, 1 + animation * 1.5f);
            renderer.renderSolid(SPRING_CAP.get(), light);
            ms.popPose();
        }

        ms.pushPose();
        ms.translate(0, 0, -animation * 2.25);
        renderer.renderSolid(shouldRender ? renderHandler.pose.get() : POINTING.get(), light);
        ms.popPose();

        ms.pushPose();
        float angle = AnimationTickHolder.getRenderTime() * -2;
        if (shouldRender) {
            angle += 360 * animation * animation * animation;
        }
        angle %= 360;
        transformStack.translate(0, 0.0625, 0).rotateZDegrees(angle).translateBack(0, 0.0625, 0);
        renderer.renderSolid(COGS.get(), light);
        ms.popPose();
    }
}
