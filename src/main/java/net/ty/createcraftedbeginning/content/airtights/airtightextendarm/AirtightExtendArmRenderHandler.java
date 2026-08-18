package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import net.ty.createcraftedbeginning.platform.access.ItemInHandRendererAccess;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class AirtightExtendArmRenderHandler {
    public static final AirtightExtendArmRenderHandler INSTANCE = new AirtightExtendArmRenderHandler();

    private float handAnimation;
    private float lastHandAnimation;
    private PartialModel pose = CCBPartialModels.AIRTIGHT_EXTEND_ARM_PUNCHING;

    private AirtightExtendArmRenderHandler() {
    }

    private static void renderPlayerArm(RenderHandEvent event, EntityRenderDispatcher renderDispatcher, LocalPlayer player, boolean rightHand, float flip) {
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        PoseTransformStack transformStack = TransformStack.of(poseStack);
        transformStack.rotateYDegrees(flip * 75);
        poseStack.translate(flip * -1, 3.6, 3.5);
        transformStack.rotateZDegrees(flip * 120).rotateXDegrees(200).rotateYDegrees(flip * -135);
        poseStack.translate(flip * 5.6, 0, 0);
        transformStack.rotateYDegrees(flip * 40);
        poseStack.translate(flip * 0.05, -0.3, -0.3);

        if (renderDispatcher.getRenderer(player) instanceof PlayerRenderer playerRenderer) {
            if (rightHand) {
                playerRenderer.renderRightHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
            }
            else {
                playerRenderer.renderLeftHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
            }
        }

        poseStack.popPose();
    }

    public void tick() {
        lastHandAnimation = handAnimation;
        handAnimation *= Mth.clamp(handAnimation, 0.8f, 0.99f);
        updatePose();
    }

    public void registerListeners(IEventBus bus) {
        bus.addListener(EventPriority.LOWEST, this::onRenderPlayerHand);
    }

    float getAnimation(float partialTicks) {
        return Mth.lerp(partialTicks, lastHandAnimation, handAnimation);
    }

    PartialModel getPose() {
        return pose;
    }

    private void updatePose() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(minecraft.getEntityRenderDispatcher().getItemInHandRenderer() instanceof ItemInHandRendererAccess accessor)) {
            return;
        }

        pose = CCBPartialModels.AIRTIGHT_EXTEND_ARM_PUNCHING;
        if (!accessor.getOffHandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM)) {
            return;
        }

        ItemStack mainHandItem = accessor.getMainHandItem();
        if (mainHandItem.isEmpty() || !(mainHandItem.getItem() instanceof BlockItem) || !minecraft.getItemRenderer().getModel(mainHandItem, null, null, 0).isGui3d()) {
            return;
        }

        pose = CCBPartialModels.AIRTIGHT_EXTEND_ARM_HOLDING;
    }

    private void onRenderPlayerHand(RenderHandEvent event) {
        if (event.isCanceled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(minecraft.getEntityRenderDispatcher().getItemInHandRenderer() instanceof ItemInHandRendererAccess accessor)) {
            return;
        }

        ItemStack offhandItem = accessor.getOffHandItem();
        ItemStack heldItem = event.getItemStack();
        boolean armInOffhand = offhandItem.is(CCBItems.AIRTIGHT_EXTEND_ARM);
        if (!armInOffhand && !heldItem.is(CCBItems.AIRTIGHT_EXTEND_ARM)) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            event.setCanceled(true);
            return;
        }

        renderMainHand(event, minecraft, player, offhandItem, armInOffhand);
        event.setCanceled(true);
    }

    private void renderMainHand(RenderHandEvent event, Minecraft minecraft, LocalPlayer player, ItemStack offhandItem, boolean armInOffhand) {
        boolean rightHand = event.getHand() == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
        float flip = rightHand ? 1 : -1;
        float swingProgress = event.getSwingProgress();
        ItemStack heldItem = event.getItemStack();
        boolean isBlockItem = heldItem.getItem() instanceof BlockItem;
        float equipProgress = isBlockItem ? 0 : event.getEquipProgress() / 4;

        if (1 - swingProgress > handAnimation && swingProgress > 0) {
            handAnimation = 0.95f;
        }

        float animation = getAnimation(AnimationTickHolder.getPartialTicks());
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();
        EntityRenderDispatcher renderDispatcher = minecraft.getEntityRenderDispatcher();

        poseStack.pushPose();
        poseStack.translate(flip * 0.54, -0.4 - 0.6 * equipProgress, -0.42);
        renderPlayerArm(event, renderDispatcher, player, rightHand, flip);

        poseStack.pushPose();
        poseStack.translate(flip * -0.1, 0, -0.3);

        ItemInHandRenderer firstPersonRenderer = renderDispatcher.getItemInHandRenderer();
        ItemDisplayContext displayContext = rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        ItemStack arm = armInOffhand ? offhandItem : heldItem;
        firstPersonRenderer.renderItem(player, arm, displayContext, !rightHand, poseStack, bufferSource, packedLight);

        if (armInOffhand) {
            ItemRenderer itemRenderer = minecraft.getItemRenderer();
            ClientHooks.handleCameraTransforms(poseStack, itemRenderer.getModel(offhandItem, null, null, 0), displayContext, !rightHand);
            poseStack.translate(flip * -0.05, 0.15, -1.2);
            poseStack.translate(0, 0, -animation * 2.25);
            if (isBlockItem && itemRenderer.getModel(heldItem, null, null, 0).isGui3d()) {
                TransformStack.of(poseStack).rotateYDegrees(flip * 45);
                poseStack.translate(flip * 0.15, -0.15, -0.05);
                poseStack.scale(1.25f, 1.25f, 1.25f);
            }

            firstPersonRenderer.renderItem(player, heldItem, displayContext, !rightHand, poseStack, bufferSource, packedLight);
        }

        poseStack.popPose();
        poseStack.popPose();
    }
}
