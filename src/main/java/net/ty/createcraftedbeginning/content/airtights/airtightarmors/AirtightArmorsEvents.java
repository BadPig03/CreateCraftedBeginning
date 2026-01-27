package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickEmpty;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightArmorsEvents {
    @SubscribeEvent
    public static void onHelmetApplyMobEffects(@NotNull Applicable event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!AirtightArmorsUtils.canPreventMobEffects(player, event.getEffectInstance())) {
            return;
        }

        event.setResult(Result.DO_NOT_APPLY);
    }

    @SubscribeEvent
    public static void onHelmetBreatheUnderwater(@NotNull LivingBreatheEvent event) {
        if (event.canBreathe() || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!AirtightArmorsUtils.canBreatheUnderwater(player)) {
            return;
        }

        event.setConsumeAirAmount(0);
    }

    @SubscribeEvent
    public static void onChestplateCreativeFlight(@NotNull Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) {
            AirtightArmorsUtils.spawnFlightParticles(player, level);
            return;
        }

        AirtightArmorsUtils.refreshChestplateModifiers(player);
        AirtightArmorsUtils.refreshBootsModifiers(player);
        if (CCBAdvancements.SKY_IS_NOT_THE_LIMIT.isAlreadyAwardedTo(player) || player.getEyePosition().y <= level.getMaxBuildHeight()) {
            return;
        }

        CCBAdvancements.SKY_IS_NOT_THE_LIMIT.awardTo(player);
    }

    @SubscribeEvent
    public static void onChestplateInvalidateWallDamage(@NotNull LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!event.getSource().is(DamageTypes.FLY_INTO_WALL)) {
            return;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return;
        }
        if (!AirtightArmorsUtils.canInvalidateDamage(player, event.getAmount(), () -> !player.level().isClientSide)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onChestplateBoostElytra(@NotNull RightClickEmpty event) {
        InteractionHand hand = event.getHand();
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        ItemStack heldItem = player.getMainHandItem();
        if (!heldItem.isEmpty()) {
            return;
        }

        float multiplier = AirtightArmorsUtils.getBoostMultiplier(player);
        if (multiplier == 0) {
            return;
        }

        AirtightArmorsUtils.boostElytra(player, multiplier);
    }

    @SubscribeEvent
    public static void onLeggingsDeflectProjectile(@NotNull ProjectileImpactEvent event) {
        if (event.getRayTraceResult().getType() != Type.ENTITY || !(event.getRayTraceResult() instanceof EntityHitResult entityResult)) {
            return;
        }

        Entity hitEntity = entityResult.getEntity();
        if (!(hitEntity instanceof Player player)) {
            return;
        }
        if (!AirtightArmorsUtils.canDeflectProjectile(player)) {
            return;
        }

        Projectile projectile = event.getProjectile();
        projectile.deflect(ProjectileDeflection.MOMENTUM_DEFLECT, player, projectile.getOwner(), true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeggingsInvalidateCrammingDamage(@NotNull LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypes.CRAMMING)) {
            return;
        }

        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return;
        }

        Level level = player.level();
        if (!AirtightArmorsUtils.canInvalidateDamage(player, event.getAmount(), () -> !level.isClientSide && level.getGameTime() % 10 == 5)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBootsInvalidateFallDamage(@NotNull LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypes.FALL)) {
            return;
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(CCBItems.AIRTIGHT_BOOTS)) {
            return;
        }
        if (!AirtightArmorsUtils.canInvalidateDamage(player, event.getAmount(), () -> !player.level().isClientSide)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBootsInvalidateEnvironmentalDamage(@NotNull LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        DamageSource damageSource = event.getSource();
        if (!damageSource.is(DamageTypes.FREEZE)) {
            return;
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(CCBItems.AIRTIGHT_BOOTS)) {
            return;
        }
        if (!AirtightArmorsUtils.canInvalidateDamage(player, event.getAmount(), () -> !player.level().isClientSide)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBootsInvalidateEnvironmentalDamagePeriodic(@NotNull LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        DamageSource damageSource = event.getSource();
        if (!damageSource.is(DamageTypes.HOT_FLOOR) && !damageSource.is(DamageTypes.CAMPFIRE) && !damageSource.is(DamageTypes.SWEET_BERRY_BUSH) && !damageSource.is(DamageTypes.CACTUS)) {
            return;
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(CCBItems.AIRTIGHT_BOOTS)) {
            return;
        }

        Level level = player.level();
        if (!AirtightArmorsUtils.canInvalidateDamage(player, event.getAmount(), () -> !level.isClientSide && level.getGameTime() % 10 == 5)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAirtightArmorFireImmune(@NotNull LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !AirtightArmorsUtils.isEntireArmoredUp(player)) {
            return;
        }

        DamageSource damageSource = event.getSource();
        if (damageSource.type().effects() != DamageEffects.BURNING) {
            return;
        }
        if (!damageSource.is(DamageTypes.LAVA) && !damageSource.is(DamageTypes.IN_FIRE) && !damageSource.is(DamageTypes.ON_FIRE)) {
            return;
        }

        if (player.getRemainingFireTicks() > 0) {
            player.setRemainingFireTicks(0);
        }
        event.setCanceled(true);
    }
}
