package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.ty.createcraftedbeginning.mixin.accessor.ItemInHandRendererAccessor;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class AirtightExtendArmRenderHandler {
    protected float handAnimation;
    protected float lastHandAnimation;
    protected PartialModel pose = CCBPartialModels.AIRTIGHT_EXTEND_ARM_PUNCHING;

    public void tick() {
        lastHandAnimation = handAnimation;
        handAnimation *= Mth.clamp(handAnimation, 0.8f, 0.99f);
        updatePose();
    }

    public float getAnimation(float partialTicks) {
        return Mth.lerp(partialTicks, lastHandAnimation, handAnimation);
    }

    public void registerListeners(@NotNull IEventBus bus) {
        bus.addListener(this::onRenderPlayerHand);
    }

    protected void updatePose() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(mc.getEntityRenderDispatcher().getItemInHandRenderer() instanceof ItemInHandRendererAccessor accessor)) {
            return;
        }

        pose = CCBPartialModels.AIRTIGHT_EXTEND_ARM_PUNCHING;
        if (!accessor.getOffHandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM)) {
            return;
        }

        ItemStack mainHandItem = accessor.getMainHandItem();
        if (mainHandItem.isEmpty() || !(mainHandItem.getItem() instanceof BlockItem) || !mc.getItemRenderer().getModel(mainHandItem, null, null, 0).isGui3d()) {
            return;
        }

        pose = CCBPartialModels.AIRTIGHT_EXTEND_ARM_HOLDING;
    }

    protected void onRenderPlayerHand(@NotNull RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(mc.getEntityRenderDispatcher().getItemInHandRenderer() instanceof ItemInHandRendererAccessor accessor)) {
            return;
        }

        ItemStack offhandItem = accessor.getOffHandItem();
        ItemStack heldItem = event.getItemStack();
        boolean notInOffhand = !offhandItem.is(CCBItems.AIRTIGHT_EXTEND_ARM);
        if (notInOffhand && !heldItem.is(CCBItems.AIRTIGHT_EXTEND_ARM)) {
            return;
        }

        boolean rightHand = event.getHand() == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
        float flip = rightHand ? 1 : -1;
        float swingProgress = event.getSwingProgress();
        boolean blockItem = heldItem.getItem() instanceof BlockItem;
        float equipProgress = blockItem ? 0 : event.getEquipProgress() / 4;

        PoseStack ms = event.getPoseStack();
        AbstractClientPlayer clientPlayer = mc.player;
        RenderSystem.setShaderTexture(0, clientPlayer.getSkin().texture());

        ms.pushPose();
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            if (1 - swingProgress > handAnimation && swingProgress > 0) {
                handAnimation = 0.95f;
            }

            float animation = getAnimation(AnimationTickHolder.getPartialTicks());
            ms.translate(flip * 0.54, -0.4 - 0.6 * equipProgress, -0.42);

            ms.pushPose();
            PoseTransformStack transformStack = TransformStack.of(ms);
            transformStack.rotateYDegrees(flip * 75);
            ms.translate(flip * -1, 3.6, 3.5);
            transformStack.rotateZDegrees(flip * 120).rotateXDegrees(200).rotateYDegrees(flip * -135);
            ms.translate(flip * 5.6, 0, 0);
            transformStack.rotateYDegrees(flip * 40);
            ms.translate(flip * 0.05, -0.3, -0.3);

            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource bufferSource = event.getMultiBufferSource();
            int packedLight = event.getPackedLight();
            EntityRenderDispatcher renderDispatcher = mc.getEntityRenderDispatcher();
            if (renderDispatcher.getRenderer(player) instanceof PlayerRenderer playerRenderer) {
                if (rightHand) {
                    playerRenderer.renderRightHand(poseStack, bufferSource, packedLight, player);
                }
                else {
                    playerRenderer.renderLeftHand(poseStack, bufferSource, packedLight, player);
                }
            }
            ms.popPose();

            ms.pushPose();
            ms.translate(flip * -0.1, 0, -0.3);
            ItemInHandRenderer firstPersonRenderer = renderDispatcher.getItemInHandRenderer();
            ItemDisplayContext displayContext = rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            firstPersonRenderer.renderItem(mc.player, notInOffhand ? heldItem : offhandItem, displayContext, !rightHand, poseStack, bufferSource, packedLight);
            if (!notInOffhand) {
                ItemRenderer itemRenderer = mc.getItemRenderer();
                ClientHooks.handleCameraTransforms(ms, itemRenderer.getModel(offhandItem, null, null, 0), displayContext, !rightHand);
                ms.translate(flip * -0.05, 0.15, -1.2);
                ms.translate(0, 0, -animation * 2.25);
                if (blockItem && itemRenderer.getModel(heldItem, null, null, 0).isGui3d()) {
                    transformStack.rotateYDegrees(flip * 45);
                    ms.translate(flip * 0.15, -0.15, -0.05);
                    ms.scale(1.25f, 1.25f, 1.25f);
                }

                firstPersonRenderer.renderItem(mc.player, heldItem, displayContext, !rightHand, poseStack, bufferSource, packedLight);
            }

            ms.popPose();
        }
        ms.popPose();
        event.setCanceled(true);
    }
}
