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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class AirtightChestplateLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public AirtightChestplateLayer(RenderLayerParent<T, M> renderer) {
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
        AirtightChestplateLayer<?, ?> layer = new AirtightChestplateLayer<>(livingRenderer);
        livingRenderer.addLayer((AirtightChestplateLayer) layer);
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
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int light, @NotNull LivingEntity entity, float yaw, float pitch, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(getParentModel() instanceof HumanoidModel<?> model)) {
            return;
        }
        if (!(entity instanceof Player player) || player.getPose() == Pose.SLEEPING) {
            return;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return;
        }

        SuperByteBuffer backpack = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_JETPACK, CCBBlocks.GAS_CANISTER_BLOCK.getDefaultState());
        SuperByteBuffer elytra = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_ELYTRA, CCBBlocks.GAS_CANISTER_BLOCK.getDefaultState());

        poseStack.pushPose();

        model.body.translateAndRotate(poseStack);
        poseStack.translate(0.5f, 0.75f, 0);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        backpack.rotateZ(Mth.PI).disableDiffuse().light(light).renderInto(poseStack, vertexConsumer);
        elytra.rotateZ(Mth.PI).disableDiffuse().light(light).renderInto(poseStack, vertexConsumer);

        poseStack.popPose();
    }
}
