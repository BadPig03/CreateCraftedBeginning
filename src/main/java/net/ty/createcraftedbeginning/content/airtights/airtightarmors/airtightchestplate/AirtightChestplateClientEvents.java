package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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

        if (ElytraUpgrade.applyClientSpeedBoost(player)) {
            if (CCBConfig.client().particles.showChestplateJetpackParticles.get()) {
                player.level().addParticle(ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY(), player.getZ(), 0, 0, 0);
            }
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
}
