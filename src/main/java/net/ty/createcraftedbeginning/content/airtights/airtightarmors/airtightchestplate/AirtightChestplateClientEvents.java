package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.CreativeFlightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ElytraUpgrade;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
public final class AirtightChestplateClientEvents {
    private static final double ELYTRA_TRAIL_SAMPLE_SPACING = 0.35;
    private static final int ELYTRA_TRAIL_MAX_SAMPLES_PER_TICK = 8;
    private static final double ELYTRA_NOZZLE_BACK_OFFSET = 0.42;
    private static final double ELYTRA_NOZZLE_SIDE_OFFSET = 0.22;
    private static final double ELYTRA_EXHAUST_SPEED = 0.18;

    private AirtightChestplateClientEvents() {
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !ElytraUpgrade.shouldInterceptBoostInput(player)) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.getHand() != InteractionHand.MAIN_HAND || !ElytraUpgrade.canRequestBoost(player)) {
            return;
        }

        if (ElytraUpgrade.tryStartClientBoostPulse(player)) {
            spawnElytraBoostTrail(player, player.level(), false);
            minecraft.getSoundManager().play(new EntityBoundSoundInstance(CCBSoundEvents.AIRTIGHT_JETPACK_LAUNCH.getMainEvent(), SoundSource.PLAYERS, 1, 1, player, player.getRandom().nextLong()));
        }
        CatnipServices.NETWORK.sendToServer(AirtightChestplateElytraBoostPacket.INSTANCE);
    }

    @SubscribeEvent
    public static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (!level.isClientSide) {
            return;
        }

        if (ElytraUpgrade.tickBoostPulse(player)) {
            spawnElytraBoostTrail(player, level, true);
        }
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) || !CCBConfig.client().particles.showChestplateJetpackParticles.get() || player.isCreative() || player.isSpectator()) {
            return;
        }

        if (!player.getAbilities().flying || !CreativeFlightUpgrade.INSTANCE.canApply(player) || player.getEffect(CCBMobEffects.JETPACK_FLIGHT) == null) {
            return;
        }

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        float bodyYawRadians = -player.yBodyRot * Mth.DEG_TO_RAD;
        float bodyYawSin = Mth.sin(bodyYawRadians);
        float bodyYawCos = Mth.cos(bodyYawRadians);
        double particleYOffset = player.getEyeHeight() * 0.4;
        level.addParticle(CCBParticleTypes.AIRTIGHT_JETPACK.getParticleOptions(), playerX + -0.48 * bodyYawSin - bodyYawCos * 0.24, playerY + particleYOffset, playerZ + -0.48 * bodyYawCos + bodyYawSin * 0.24, 0, -0.24, 0);
        level.addParticle(CCBParticleTypes.AIRTIGHT_JETPACK.getParticleOptions(), playerX + -0.48 * bodyYawSin + bodyYawCos * 0.24, playerY + particleYOffset, playerZ + -0.48 * bodyYawCos - bodyYawSin * 0.24, 0, -0.24, 0);
    }

    @SubscribeEvent
    public static void onClientLoggingOut(LoggingOut event) {
        ElytraUpgrade.clearClientBoostPulses();
    }

    private static void spawnElytraBoostTrail(Player player, Level level, boolean interpolateMotion) {
        if (!CCBConfig.client().particles.showChestplateJetpackParticles.get()) {
            return;
        }

        Vec3 look = player.getLookAngle();
        double lookLengthSqr = look.lengthSqr();
        if (!Double.isFinite(lookLengthSqr) || lookLengthSqr <= 1.0E-12) {
            return;
        }

        Vec3 forward = look.scale(1.0 / Math.sqrt(lookLengthSqr));
        Vec3 right = new Vec3(forward.z, 0, -forward.x);
        double rightLengthSqr = right.lengthSqr();
        if (rightLengthSqr <= 1.0E-12) {
            float yawRadians = -player.getYRot() * Mth.DEG_TO_RAD;
            right = new Vec3(Mth.cos(yawRadians), 0, Mth.sin(yawRadians));
        }
        else {
            right = right.scale(1.0 / Math.sqrt(rightLengthSqr));
        }

        Vec3 currentPosition = player.position();
        Vec3 previousPosition = interpolateMotion ? new Vec3(player.xo, player.yo, player.zo) : currentPosition;
        double traveledDistance = currentPosition.distanceTo(previousPosition);
        if (!Double.isFinite(traveledDistance)) {
            traveledDistance = 0;
        }
        int sampleCount = Mth.clamp(Mth.ceil(traveledDistance / ELYTRA_TRAIL_SAMPLE_SPACING), 1, ELYTRA_TRAIL_MAX_SAMPLES_PER_TICK);
        Vec3 nozzleOffset = forward.scale(-ELYTRA_NOZZLE_BACK_OFFSET).add(0, player.getBbHeight() * 0.55, 0);
        Vec3 sideOffset = right.scale(ELYTRA_NOZZLE_SIDE_OFFSET);
        Vec3 exhaustVelocity = forward.scale(-ELYTRA_EXHAUST_SPEED);
        for (int i = 1; i <= sampleCount; i++) {
            double progress = (double) i / sampleCount;
            Vec3 trailPosition = previousPosition.lerp(currentPosition, progress).add(nozzleOffset);
            Vec3 subtracted = trailPosition.subtract(sideOffset);
            level.addParticle(CCBParticleTypes.AIRTIGHT_JETPACK.getParticleOptions(), subtracted.x, subtracted.y, subtracted.z, exhaustVelocity.x, exhaustVelocity.y, exhaustVelocity.z);
            Vec3 added = trailPosition.add(sideOffset);
            level.addParticle(CCBParticleTypes.AIRTIGHT_JETPACK.getParticleOptions(), added.x, added.y, added.z, exhaustVelocity.x, exhaustVelocity.y, exhaustVelocity.z);
        }
    }
}
