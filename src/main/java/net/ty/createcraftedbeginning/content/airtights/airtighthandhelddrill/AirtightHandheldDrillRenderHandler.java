package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
@OnlyIn(Dist.CLIENT)
public final class AirtightHandheldDrillRenderHandler {
    public static final AirtightHandheldDrillRenderHandler INSTANCE = new AirtightHandheldDrillRenderHandler();

    private final AirtightHandheldDrillAnimationState animation = new AirtightHandheldDrillAnimationState();

    @Nullable
    private LocalPlayer animatedPlayer;
    @Nullable
    private ClientLevel animatedLevel;
    private ItemStack animatedDrill = ItemStack.EMPTY;
    private int animatedSlot = -1;
    private boolean paused;

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

    public void registerListeners(IEventBus bus) {
        bus.addListener(AirtightHandheldDrillRenderHandler::onRenderPlayerHand);
    }

    public float getRotationAngle(ItemStack drill, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player != animatedPlayer || minecraft.level != animatedLevel || player.getMainHandItem() != animatedDrill || player.getInventory().selected != animatedSlot) {
            return 0;
        }

        if (drill != animatedDrill && drill != player.getOffhandItem()) {
            return 0;
        }

        return animation.getRotationAngle(partialTicks);
    }

    void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || !player.isAlive() || player.isSpectator()) {
            reset();
            return;
        }

        ItemStack drill = player.getMainHandItem();
        int selectedSlot = player.getInventory().selected;
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            reset();
            return;
        }

        if (animatedPlayer != player || animatedLevel != level || animatedDrill != drill || animatedSlot != selectedSlot) {
            reset();
            animatedPlayer = player;
            animatedLevel = level;
            animatedDrill = drill;
            animatedSlot = selectedSlot;
        }
        if (minecraft.isPaused()) {
            if (!paused) {
                animation.freeze(AnimationTickHolder.getPartialTicks());
                paused = true;
            }
            return;
        }

        paused = false;
        boolean canInteract = minecraft.screen == null && minecraft.getOverlay() == null && minecraft.isWindowActive();
        HitResult hitResult = minecraft.hitResult;
        boolean usingDrill = canInteract && player.isUsingItem() && player.getUseItem().is(CCBItems.AIRTIGHT_HANDHELD_DRILL);
        boolean targetingBlock = canInteract && minecraft.options.keyAttack.isDown() && hitResult != null && hitResult.getType() == Type.BLOCK;
        animation.tick(usingDrill || targetingBlock);
    }

    void reset() {
        animation.reset();
        animatedPlayer = null;
        animatedLevel = null;
        animatedDrill = ItemStack.EMPTY;
        animatedSlot = -1;
        paused = false;
    }
}
