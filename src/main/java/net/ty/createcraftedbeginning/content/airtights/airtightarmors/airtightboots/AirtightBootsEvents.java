package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.EnvironmentalDamageProtectionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.FallProtectionUpgrade;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public class AirtightBootsEvents {
    @SubscribeEvent
    public static void onPlayerIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        DamageSource source = event.getSource();
        boolean isEnvironmentalDamage = source.is(DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES) || source.is(DamageTypes.SWEET_BERRY_BUSH);
        if (isEnvironmentalDamage && EnvironmentalDamageProtectionUpgrade.INSTANCE.canApply(player)) {
            event.setCanceled(true);
            return;
        }

        if (!source.is(DamageTypeTags.IS_FALL) || !FallProtectionUpgrade.INSTANCE.canApply(player)) {
            return;
        }

        event.setCanceled(true);
    }
}
