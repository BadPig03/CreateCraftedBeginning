package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.CrammingProtectionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.LeggingsResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.ProjectileDeflectionUpgrade;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightLeggingsEvents {
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityResult) || entityResult.getType() != Type.ENTITY || !(entityResult.getEntity() instanceof Player player)) {
            return;
        }

        Projectile projectile = event.getProjectile();
        if (!ProjectileDeflectionUpgrade.INSTANCE.canApply(player, projectile.getDeltaMovement())) {
            return;
        }

        projectile.deflect(ProjectileDeflection.MOMENTUM_DEFLECT, player, projectile.getOwner(), true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypes.CRAMMING) || !CrammingProtectionUpgrade.INSTANCE.canApply(player)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerPreTakeDamage(Pre event) {
        if (!(event.getEntity() instanceof Player player) || event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            return;
        }

        LeggingsResistanceUpgrade.INSTANCE.canApply(player, event.getOriginalDamage());
    }

    @SubscribeEvent
    public static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.is(CCBItems.AIRTIGHT_LEGGINGS) || player.level().isClientSide) {
            return;
        }

        AirtightLeggingsUpgradeRegistry.forEach(upgrade -> {
            if (!upgrade.canApply(player)) {
                return;
            }

            upgrade.applyEffect(player);
        });
    }
}
