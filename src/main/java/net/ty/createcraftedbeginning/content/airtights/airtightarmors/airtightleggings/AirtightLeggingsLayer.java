package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightLeggingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private AirtightLeggingsLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    private static void registerOn(EntityRenderer<?> renderer) {
        if (!(renderer instanceof LivingEntityRenderer<?, ?> livingRenderer)) {
            return;
        }

        registerOnLiving(livingRenderer);
    }

    private static <T extends LivingEntity, M extends EntityModel<T>> void registerOnLiving(LivingEntityRenderer<T, M> renderer) {
        if (!(renderer.getModel() instanceof HumanoidModel<?>)) {
            return;
        }

        renderer.addLayer(new AirtightLeggingsLayer<>(renderer));
    }

    public static void registerOnAll(EntityRenderDispatcher dispatcher) {
        for (EntityRenderer<? extends Player> renderer : dispatcher.getSkinMap().values()) {
            registerOn(renderer);
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, LivingEntity entity, float yaw, float pitch, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(getParentModel() instanceof HumanoidModel<?> model) || !(entity instanceof Player player) || !player.getItemBySlot(EquipmentSlot.LEGS).is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        SuperByteBuffer shield = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_SHIELD, CCBBlocks.GAS_CANISTER_BLOCK.getDefaultState());
        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);
        poseStack.translate(0.5, 0.75, 0);
        shield.rotateZ(Mth.PI).disableDiffuse().light(light).renderInto(poseStack, consumer);
        poseStack.popPose();
    }
}
