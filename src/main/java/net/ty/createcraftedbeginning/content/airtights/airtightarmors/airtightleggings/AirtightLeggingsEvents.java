package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
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
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.CrammingProtectionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.ProjectileDeflectionUpgrade;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightLeggingsEvents {
    @SubscribeEvent
    public static void onProjectileImpact(@NotNull ProjectileImpactEvent event) {
        if (event.getRayTraceResult().getType() != Type.ENTITY || !(event.getRayTraceResult() instanceof EntityHitResult entityResult)) {
            return;
        }

        Entity hitEntity = entityResult.getEntity();
        if (!(hitEntity instanceof Player player) || !ProjectileDeflectionUpgrade.canDeflect(player)) {
            return;
        }

        Projectile projectile = event.getProjectile();
        projectile.deflect(ProjectileDeflection.MOMENTUM_DEFLECT, player, projectile.getOwner(), true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInvalidateCrammingDamage(@NotNull LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypes.CRAMMING)) {
            return;
        }

        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.is(CCBItems.AIRTIGHT_LEGGINGS) || !CrammingProtectionUpgrade.INSTANCE.isEnabled(leggings) || !AirtightArmorsUtils.canInvalidateDamage(player, 0, () -> !player.level().isClientSide)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(@NotNull Post event) {
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
