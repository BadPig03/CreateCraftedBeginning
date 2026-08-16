package net.ty.createcraftedbeginning.client.contents;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickEmpty;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateElytraBoostPacket;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.CreativeFlightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ElytraUpgrade;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
public final class AirtightChestplateClientEvents {
    private AirtightChestplateClientEvents() {
    }

    @SubscribeEvent
    public static void onRightClickEmpty(RightClickEmpty event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) {
            return;
        }

        if (!ElytraUpgrade.applyClientSpeedBoost(player)) {
            return;
        }

        CatnipServices.NETWORK.sendToServer(AirtightChestplateElytraBoostPacket.INSTANCE);
    }

    @SubscribeEvent
    public static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return;
        }

        spawnCreativeFlightParticles(player, player.level());
    }

    private static void spawnCreativeFlightParticles(Player player, Level level) {
        if (!CCBConfig.client().enableChestplateJetpackParticles.get() || player.isCreative() || player.isSpectator()) {
            return;
        }

        if (!player.getAbilities().flying || !CreativeFlightUpgrade.INSTANCE.canApply(player) || player.getEffect(CCBMobEffects.JETPACK_FLIGHT) == null) {
            return;
        }

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double angle = Math.toRadians(-player.yBodyRot);
        double yOffset = player.getEyeHeight() * 0.4;
        level.addParticle(CCBParticleTypes.AIRTIGHT_JETPACK.getParticleOptions(), x + -0.48 * Math.sin(angle) - Math.cos(angle) * 0.24, y + yOffset, z + -0.48 * Math.cos(angle) + Math.sin(angle) * 0.24, 0, -0.24, 0);
        level.addParticle(CCBParticleTypes.AIRTIGHT_JETPACK.getParticleOptions(), x + -0.48 * Math.sin(angle) + Math.cos(angle) * 0.24, y + yOffset, z + -0.48 * Math.cos(angle) - Math.sin(angle) * 0.24, 0, -0.24, 0);
    }
}
