package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.mixin.accessor.EntityRenderDispatcherAccessor;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
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
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class AirtightLeggingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public AirtightLeggingsLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerOn(EntityRenderer<?> entityRenderer) {
        if (!(entityRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer)) {
            return;
        }
        if (!(livingRenderer.getModel() instanceof HumanoidModel)) {
            return;
        }
        AirtightLeggingsLayer<?, ?> layer = new AirtightLeggingsLayer<>(livingRenderer);
        livingRenderer.addLayer((AirtightLeggingsLayer) layer);
    }

    public static void registerOnAll(@NotNull EntityRenderDispatcher renderManager) {
        for (EntityRenderer<? extends Player> renderer : renderManager.getSkinMap().values()) {
            registerOn(renderer);
        }
        for (EntityRenderer<?> renderer : ((EntityRenderDispatcherAccessor) renderManager).create$getRenderers().values()) {
            registerOn(renderer);
        }
    }

    @Override
    public void render(@NotNull PoseStack ms, @NotNull MultiBufferSource buffer, int light, @NotNull LivingEntity entity, float yaw, float pitch, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(getParentModel() instanceof HumanoidModel<?> model)) {
            return;
        }
        if (!(entity instanceof Player player) || !player.getItemBySlot(EquipmentSlot.LEGS).is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return;
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(Sheets.cutoutBlockSheet());
        SuperByteBuffer shield = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_SHIELD, CCBBlocks.GAS_CANISTER_BLOCK.getDefaultState());

        ms.pushPose();

        model.body.translateAndRotate(ms);
        ms.translate(0.5f, 0.75f, 0);

        shield.rotateZ(Mth.PI).disableDiffuse().light(light).renderInto(ms, vertexConsumer);
        
        ms.popPose();
    }
}
