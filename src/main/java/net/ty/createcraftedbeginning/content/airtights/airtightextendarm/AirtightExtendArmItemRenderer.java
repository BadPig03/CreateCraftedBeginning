package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
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

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(value = Dist.CLIENT, modid = CCBAPI.MOD_ID)
final class AirtightExtendArmItemRenderer extends CustomRenderedItemModelRenderer {
    private static final PartialModel COGS = CCBPartialModels.AIRTIGHT_EXTEND_ARM_COGS;
    private static final PartialModel SPRING = CCBPartialModels.AIRTIGHT_EXTEND_ARM_SPRING;
    private static final PartialModel SPRING_CAP = CCBPartialModels.AIRTIGHT_EXTEND_ARM_SPRING_CAP;
    private static final PartialModel POINTING = CCBPartialModels.AIRTIGHT_EXTEND_ARM_POINTING;

    private AirtightExtendArmItemRenderer() {
    }

    @SubscribeEvent
    private static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(SimpleCustomRenderer.create(CCBItems.AIRTIGHT_EXTEND_ARM.asItem(), new AirtightExtendArmItemRenderer()), CCBItems.AIRTIGHT_EXTEND_ARM.asItem());
    }

    @Override
    protected void render(ItemStack arm, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        AirtightExtendArmRenderHandler renderHandler = AirtightExtendArmRenderHandler.INSTANCE;
        boolean isFirstPerson = transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        float extensionProgress = isFirstPerson ? renderHandler.getAnimation(AnimationTickHolder.getPartialTicks()) : 0;
        renderer.renderSolid(model.getOriginalModel(), light);

        ms.pushPose();
        ms.translate(0, 0, -0.625 - extensionProgress * 1.125);
        ms.scale(1 - extensionProgress * 0.125f, 1 - extensionProgress * 0.125f, 1 + extensionProgress * 9);
        renderer.renderSolid(SPRING.get(), light);
        ms.popPose();

        for (int springIndex = 0; springIndex < 4; springIndex++) {
            ms.pushPose();
            ms.translate(0, 0, -0.7375 + 0.075 * springIndex - extensionProgress * 0.5625 * (3.5 - springIndex));
            ms.scale(1 - extensionProgress * 0.125f, 1 - extensionProgress * 0.125f, 1 + extensionProgress * 1.5f);
            renderer.renderSolid(SPRING_CAP.get(), light);
            ms.popPose();
        }

        ms.pushPose();
        ms.translate(0, 0, -extensionProgress * 2.25);
        renderer.renderSolid(isFirstPerson ? renderHandler.getPose().get() : POINTING.get(), light);
        ms.popPose();

        ms.pushPose();
        float cogAngle = AnimationTickHolder.getRenderTime() * -2;
        if (isFirstPerson) {
            cogAngle += 360 * extensionProgress * extensionProgress * extensionProgress;
        }
        cogAngle %= 360;
        TransformStack.of(ms).translate(0, 0.0625, 0).rotateZDegrees(cogAngle).translateBack(0, 0.0625, 0);
        renderer.renderSolid(COGS.get(), light);
        ms.popPose();
    }
}
