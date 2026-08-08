package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.GlobalAirtightUpgradesConsumptionManager;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public class AirtightArmorsEvents {
    @SubscribeEvent
    public static void onAirtightArmorFireImmune(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !AirtightArmorsUtils.isEntireArmoredUp(player)) {
            return;
        }

        DamageSource source = event.getSource();
        boolean isBurning = source.type().effects() == DamageEffects.BURNING;
        boolean isFireDamage = source.is(DamageTypes.LAVA) || source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE);
        if (!isBurning || !isFireDamage) {
            return;
        }

        if (player.getRemainingFireTicks() > 0) {
            player.setRemainingFireTicks(0);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerPreTakeDamage(Pre event) {
        if (!(event.getEntity() instanceof Player player) || event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            return;
        }

        float reducedDamage = AirtightArmorsUtils.applyPaidResistance(player, event.getOriginalDamage(), event.getNewDamage());
        event.setNewDamage(reducedDamage);
    }

    @SubscribeEvent
    public static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        GlobalAirtightUpgradesConsumptionManager.tick(player);
        if (level.getGameTime() % 20 != 0 || !AirtightArmorsUtils.isEntireArmoredUp(player)) {
            return;
        }

        CCBAdvancements.SEALED_TO_PERFECTION.awardTo(player);
        boolean allUpgradesEnabled = AirtightHelmetUpgradeRegistry.allUpgradesEnabled(player) && AirtightChestplateUpgradeRegistry.allUpgradesEnabled(player) && AirtightLeggingsUpgradeRegistry.allUpgradesEnabled(player) && AirtightBootsUpgradeRegistry.allUpgradesEnabled(player);
        if (!allUpgradesEnabled) {
            return;
        }

        CCBAdvancements.PHANTOM_DIVER.awardTo(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        GlobalAirtightUpgradesConsumptionManager.forceSyncToClient(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        GlobalAirtightUpgradesConsumptionManager.clearTracking(player);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        GlobalAirtightUpgradesConsumptionManager.clear(player);
        GlobalAirtightUpgradesConsumptionManager.syncToClient(player);
    }
}
