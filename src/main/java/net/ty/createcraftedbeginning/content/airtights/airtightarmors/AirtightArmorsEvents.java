package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.GlobalAirtightUpgradesConsumptionManager;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightArmorsEvents {
    @SubscribeEvent
    public static void onAirtightArmorFireImmune(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !AirtightArmorsUtils.isEntireArmoredUp(player)) {
            return;
        }

        DamageSource damageSource = event.getSource();
        if (damageSource.type().effects() != DamageEffects.BURNING || !(damageSource.is(DamageTypes.LAVA) || damageSource.is(DamageTypes.IN_FIRE) || damageSource.is(DamageTypes.ON_FIRE))) {
            return;
        }

        if (player.getRemainingFireTicks() > 0) {
            player.setRemainingFireTicks(0);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        GlobalAirtightUpgradesConsumptionManager.tick(player);
        AirtightArmorsUtils.applyResistance(player);

        if (level.getGameTime() % 20 != 0 || !AirtightArmorsUtils.isEntireArmoredUp(player)) {
            return;
        }

        CCBAdvancements.SEALED_TO_PERFECTION.awardTo(player);
        if (!AirtightHelmetUpgradeRegistry.allUpgradesActive(player) || !AirtightChestplateUpgradeRegistry.allUpgradesActive(player) || !AirtightLeggingsUpgradeRegistry.allUpgradesActive(player) || !AirtightBootsUpgradeRegistry.allUpgradesActive(player)) {
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

        GlobalAirtightUpgradesConsumptionManager.syncToClient(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        GlobalAirtightUpgradesConsumptionManager.clear(player);
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
