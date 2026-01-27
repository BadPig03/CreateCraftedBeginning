package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.mojang.blaze3d.vertex.PoseStack;
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
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class AirtightHandheldDrillRenderHandler {
    private static final float MAX_ANIMATION = 1.0f;
    private static final float MIN_ANIMATION = 0.001f;
    private static final float ACCELERATION = (float) Math.pow(MAX_ANIMATION / MIN_ANIMATION, 0.1f);
    private static final float DECELERATION = 1 / ACCELERATION;

    protected float handAnimation;
    protected float lastHandAnimation;
    protected boolean accelerate;
    protected boolean decelerate;

    public void tick() {
        lastHandAnimation = handAnimation;
        if (accelerate) {
            handAnimation *= ACCELERATION;
            if (handAnimation > MAX_ANIMATION) {
                handAnimation = MAX_ANIMATION;
                accelerate = false;
            }
        }
        if (decelerate) {
            handAnimation *= DECELERATION;
            if (handAnimation < MIN_ANIMATION) {
                handAnimation = 0;
                decelerate = false;
            }
        }
    }

    public void start() {
        if (handAnimation < MIN_ANIMATION) {
            handAnimation = MIN_ANIMATION;
        }
        accelerate = true;
        decelerate = false;
    }

    public void stop() {
        accelerate = false;
        decelerate = true;
    }

    public float getAnimation(float partialTicks) {
        return Mth.lerp(partialTicks, lastHandAnimation, handAnimation);
    }

    public boolean hasHandAnimation(float threshold) {
        return handAnimation > threshold;
    }

    public void registerListeners(@NotNull IEventBus bus) {
        bus.addListener(this::onRenderPlayerHand);
    }

    protected void onRenderPlayerHand(@NotNull RenderHandEvent event) {
        ItemStack drill = event.getItemStack();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        PoseStack ms = event.getPoseStack();
        boolean rightHand = event.getHand() == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
        float flip = rightHand ? 1 : -1;

        ms.pushPose();
        ms.translate(flip * 0.56f, -0.52f + event.getEquipProgress() * -0.6f, -0.72f);
        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, drill, rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !rightHand, ms, event.getMultiBufferSource(), event.getPackedLight());
        ms.popPose();

        event.setCanceled(true);
    }
}
