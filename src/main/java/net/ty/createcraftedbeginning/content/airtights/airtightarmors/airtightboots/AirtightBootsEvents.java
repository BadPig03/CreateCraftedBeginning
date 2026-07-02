package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.BootsResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.EnvironmentalDamageProtectionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.FallProtectionUpgrade;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightBootsEvents {

    @SubscribeEvent
    public static void onPlayerIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        DamageSource source = event.getSource();
        if ((source.is(DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES) || source.is(DamageTypes.SWEET_BERRY_BUSH)) && EnvironmentalDamageProtectionUpgrade.INSTANCE.canApply(player)) {
            event.setCanceled(true);
        }
        else if (source.is(DamageTypeTags.IS_FALL) && FallProtectionUpgrade.INSTANCE.canApply(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerPreTakeDamage(Pre event) {
        if (!(event.getEntity() instanceof Player player) || event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            return;
        }

        BootsResistanceUpgrade.INSTANCE.canApply(player, event.getOriginalDamage());
    }

    @SubscribeEvent
    public static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        AirtightBootsUpgradeRegistry.forEach(upgrade -> {
            if (!upgrade.canApply(player)) {
                return;
            }

            upgrade.applyEffect(player);
        });
    }
}
