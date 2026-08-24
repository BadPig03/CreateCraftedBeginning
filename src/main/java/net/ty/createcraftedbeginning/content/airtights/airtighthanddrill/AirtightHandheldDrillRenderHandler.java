package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class AirtightHandheldDrillRenderHandler {
    public static final AirtightHandheldDrillRenderHandler INSTANCE = new AirtightHandheldDrillRenderHandler();

    private static final float MAX_ANIMATION = 1.2f;
    private static final float MIN_ANIMATION = 0.001f;
    private static final float ACCELERATION = (float) Math.pow(MAX_ANIMATION / MIN_ANIMATION, 0.1);
    private static final float DECELERATION = 1 / ACCELERATION;

    private float handAnimation;
    private float lastHandAnimation;
    private boolean accelerate;
    private boolean decelerate;

    private AirtightHandheldDrillRenderHandler() {
    }

    private static void onRenderPlayerHand(RenderHandEvent event) {
        ItemStack drillStack = event.getItemStack();
        if (!drillStack.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        boolean isRightHand = event.getHand() == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
        float handSign = isRightHand ? 1 : -1;
        ItemDisplayContext displayContext = isRightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

        poseStack.pushPose();
        poseStack.translate(handSign * 0.56, -0.52 + event.getEquipProgress() * -0.6, -0.72);
        minecraft.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, drillStack, displayContext, !isRightHand, poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();

        event.setCanceled(true);
    }

    public void tick() {
        lastHandAnimation = handAnimation;
        if (accelerate) {
            handAnimation *= ACCELERATION;
            if (handAnimation > MAX_ANIMATION) {
                handAnimation = MAX_ANIMATION;
                accelerate = false;
            }
        }
        if (!decelerate) {
            return;
        }

        handAnimation *= DECELERATION;
        if (handAnimation >= MIN_ANIMATION) {
            return;
        }

        handAnimation = 0;
        decelerate = false;
    }

    public void registerListeners(IEventBus bus) {
        bus.addListener(AirtightHandheldDrillRenderHandler::onRenderPlayerHand);
    }

    void start() {
        if (handAnimation < MIN_ANIMATION) {
            handAnimation = MIN_ANIMATION;
        }
        accelerate = true;
        decelerate = false;
    }

    void stop() {
        accelerate = false;
        decelerate = true;
    }

    float getAnimation(float partialTicks) {
        return Mth.lerp(partialTicks, lastHandAnimation, handAnimation);
    }

    boolean hasHandAnimation() {
        return handAnimation > 0;
    }
}
