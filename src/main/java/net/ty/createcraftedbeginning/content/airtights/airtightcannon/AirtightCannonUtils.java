package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlerUtils;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.projectile.WeatherFlareProjectileEntity;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCannonUtils {
    private static final int EFFICIENT_USE_TIME = 15;
    private static final int SHOT_COOLDOWN = 15;
    private static final float MIN_CHARGED_RATIO = 0.33333334f;
    private static final float POWER_MULTIPLIER_PER_LEVEL = 0.125f;
    private static final double MIN_RAY_OFFSET_LENGTH_SQR = 1.0E-8;
    private static final double RAY_START_OFFSET = 1.0E-4;

    private AirtightCannonUtils() {
    }

    public static ExplosionDamageCalculator createDamageCalculator(AirtightCannonShotContext context) {
        return new SimpleExplosionDamageCalculator(true, false, Optional.of(context.knockbackMultiplier()), BuiltInRegistries.BLOCK.getTag(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity())) {
            @Override
            public float getKnockbackMultiplier(Entity entity) {
                if (context.isFriendlyTarget(entity)) {
                    return 0;
                }
                return super.getKnockbackMultiplier(entity);
            }
        };
    }

    public static List<LivingEntity> getNearbyEntities(Level level, Vec3 pos, float radius, AirtightCannonShotContext context) {
        return getNearbyEntities(level, pos, radius, context.projectile(), context.owner());
    }

    public static void applyBonusDamage(List<LivingEntity> entities, DamageSource damageSource, float bonusDamage) {
        applyBonusDamage(entities, damageSource, entity -> bonusDamage);
    }

    public static void applyBonusDamage(LivingEntity entity, DamageSource damageSource, float bonusDamage) {
        entity.hurt(damageSource, bonusDamage);
    }

    public static void applyBonusDamage(List<LivingEntity> entities, DamageSource damageSource, BonusDamageFunction damageFunction) {
        for (LivingEntity entity : entities) {
            entity.hurt(damageSource, damageFunction.getDamage(entity));
        }
    }

    static Optional<Float> getChargedRatio(ItemStack cannon, int timeCharged) {
        int efficientUseTime = getEfficientUseTime(cannon);
        int minimumUseTime = Math.max(Mth.ceil(efficientUseTime * MIN_CHARGED_RATIO), 1);
        if (timeCharged < minimumUseTime) {
            return Optional.empty();
        }
        return Optional.of(CCBMathUtils.clampNonNegative((float) timeCharged / efficientUseTime, 2));
    }

    static int getEfficientUseTime(ItemStack cannon) {
        int quickChargeLevel = getEnchantmentLevel(cannon, Enchantments.QUICK_CHARGE);
        return Math.max(EFFICIENT_USE_TIME - quickChargeLevel * 3, 1);
    }

    static void fireFlares(Level level, Player player, ItemStack flareStack, float chargedRatio) {
        InteractionHand hand = player.getUsedItemHand();
        ItemStack cannon = player.getItemInHand(hand);
        if (consumeShotFuel(player, chargedRatio, 1).isEmpty()) {
            return;
        }

        int infinityLevel = getEnchantmentLevel(cannon, Enchantments.INFINITY);
        Vec3 lookDirection = player.getLookAngle().normalize();
        Vec3 barrelPos = player.getEyePosition().add(lookDirection.scale(0.75));
        Vec3 flareMotion = lookDirection.scale(chargedRatio);

        WeatherFlareProjectileEntity flareProjectile = new WeatherFlareProjectileEntity(level, flareStack.getItem(), barrelPos.y);
        flareProjectile.setPos(barrelPos);
        flareProjectile.setOwner(player);
        flareProjectile.setDeltaMovement(flareMotion);
        flareProjectile.setCopied(infinityLevel > 0);
        level.addFreshEntity(flareProjectile);
        if (!player.isCreative() && infinityLevel == 0) {
            flareStack.shrink(1);
        }

        finishShot(player, cannon, hand, barrelPos, lookDirection);
    }

    static void spawnWindCharges(Level level, Player player, float chargedRatio) {
        InteractionHand hand = player.getUsedItemHand();
        ItemStack cannon = player.getItemInHand(hand);
        int windChargeCount = getWindChargeCount(cannon);
        Optional<ShotFuel> fuel = consumeShotFuel(player, chargedRatio, windChargeCount);
        if (fuel.isEmpty()) {
            return;
        }

        ShotFuel selectedFuel = fuel.get();
        int punchLevel = getEnchantmentLevel(cannon, Enchantments.PUNCH);
        int powerLevel = getEnchantmentLevel(cannon, Enchantments.POWER);
        boolean hasFlame = getEnchantmentLevel(cannon, Enchantments.FLAME) > 0;
        float powerMultiplier = 1 + powerLevel * POWER_MULTIPLIER_PER_LEVEL;
        float effectMultiplier = chargedRatio * powerMultiplier;
        float knockbackMultiplier = 0.1f + punchLevel * 0.25f;

        Vec3 lookDirection = player.getLookAngle().normalize();
        Vec3 barrelPos = player.getEyePosition().add(lookDirection.scale(0.75));
        Vec3 baseMotion = lookDirection.scale(2);
        RandomSource random = level.getRandom();
        Vec3 spreadBase = windChargeCount > 1 ? VecHelper.rotate(new Vec3(0, 0.1, 0), 360 * random.nextFloat(), Axis.Z) : Vec3.ZERO;
        float spreadStepDegrees = 360.0f / windChargeCount;
        Holder<Gas> gasHolder = selectedFuel.gasType().getHolder();
        for (int projectileIndex = 0; projectileIndex < windChargeCount; projectileIndex++) {
            Vec3 projectileMotion = baseMotion;
            if (windChargeCount > 1) {
                float spreadJitterDegrees = 45 * (random.nextFloat() - 0.5f);
                Vec3 spreadOffset = VecHelper.rotate(spreadBase, projectileIndex * spreadStepDegrees + spreadJitterDegrees, Axis.Z);
                projectileMotion = projectileMotion.add(VecHelper.lookAt(spreadOffset, baseMotion));
            }

            AirtightCannonWindChargeProjectileEntity windCharge = new AirtightCannonWindChargeProjectileEntity(level, gasHolder, projectileMotion);
            windCharge.setPos(barrelPos);
            windCharge.setOwner(player);
            windCharge.setDeltaMovement(projectileMotion);
            windCharge.setMultiplier(effectMultiplier);
            windCharge.setKnockback(knockbackMultiplier);
            windCharge.setFlame(hasFlame);
            level.addFreshEntity(windCharge);
        }

        finishShot(player, cannon, hand, barrelPos, lookDirection);
    }

    private static int getEnchantmentLevel(ItemStack cannon, ResourceKey<Enchantment> enchantment) {
        if (!cannon.is(CCBItems.AIRTIGHT_CANNON)) {
            return 0;
        }
        return cannon.getTagEnchantments().entrySet().stream().filter(entry -> entry.getKey().is(enchantment)).findFirst().map(Entry::getValue).orElse(0);
    }

    private static List<LivingEntity> getNearbyEntities(Level level, Vec3 pos, float radius, Entity source, @Nullable Entity owner) {
        float searchRadius = radius * 2;
        float searchRadiusSqr = searchRadius * searchRadius;
        AABB searchArea = new AABB(pos, pos).inflate(searchRadius, searchRadius, searchRadius);
        return level.getEntitiesOfClass(LivingEntity.class, searchArea, entity -> entity.getBoundingBox().getCenter().distanceToSqr(pos) <= searchRadiusSqr && !AirtightCannonShotContext.isProtectedTarget(owner, entity) && hasLineOfSight(level, pos, entity, source));
    }

    private static boolean hasLineOfSight(Level level, Vec3 sourcePos, LivingEntity target, Entity source) {
        return isRayClear(level, sourcePos, target.getBoundingBox().getCenter(), source) || isRayClear(level, sourcePos, target.getEyePosition(), source);
    }

    private static boolean isRayClear(Level level, Vec3 sourcePos, Vec3 targetPos, Entity source) {
        Vec3 rayOffset = targetPos.subtract(sourcePos);
        Vec3 rayStart = rayOffset.lengthSqr() > MIN_RAY_OFFSET_LENGTH_SQR ? sourcePos.add(rayOffset.normalize().scale(RAY_START_OFFSET)) : sourcePos;
        ClipContext clipContext = new ClipContext(rayStart, targetPos, Block.COLLIDER, Fluid.NONE, source);
        return level.clip(clipContext).getType() == Type.MISS;
    }

    private static double getRawGasConsumption(float chargedRatio, int projectileCount) {
        float chargeConsumptionMultiplier = chargedRatio >= 1 ? Mth.square(chargedRatio) : Mth.sqrt(chargedRatio);
        int perShotConsumption = CCBConfig.server().equipments.perShotConsumption.get();
        return perShotConsumption * (double) projectileCount * chargeConsumptionMultiplier;
    }

    private static int getWindChargeCount(ItemStack cannon) {
        return 2 * getEnchantmentLevel(cannon, Enchantments.MULTISHOT) + 1;
    }

    private static Optional<ShotFuel> resolveShotFuel(Player player, float chargedRatio, int projectileCount) {
        Gas selectedGas = CanisterContainerClients.getStoredGasType(player);
        if (selectedGas.isEmpty()) {
            return Optional.empty();
        }

        double rawGasConsumption = getRawGasConsumption(chargedRatio, projectileCount);
        AirtightCannonHandler cannonHandler = AirtightCannonHandlerUtils.of(selectedGas);
        long amount = GasConsumptions.roundUp(rawGasConsumption * cannonHandler.getGasConsumptionMultiplier());
        if (amount < 0) {
            return Optional.empty();
        }
        return Optional.of(new ShotFuel(selectedGas, amount));
    }

    private static Optional<ShotFuel> consumeShotFuel(Player player, float chargedRatio, int projectileCount) {
        Optional<ShotFuel> fuel = resolveShotFuel(player, chargedRatio, projectileCount);
        if (fuel.isEmpty()) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.no_gas");
            return Optional.empty();
        }

        ShotFuel selectedFuel = fuel.get();
        if (CanisterContainerConsumers.interactContainer(player, selectedFuel.gasType(), selectedFuel.amount(), () -> true, false)) {
            return fuel;
        }

        GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", Component.translatable(selectedFuel.gasType().getTranslationKey()));
        return Optional.empty();
    }

    private static void finishShot(Player player, ItemStack cannon, InteractionHand hand, Vec3 barrelPos, Vec3 lookDirection) {
        ShootableGadgetItemMethods.applyCooldown(player, cannon, hand, stack -> stack.getItem() instanceof AirtightCannonItem, SHOT_COOLDOWN);
        ShootableGadgetItemMethods.sendPackets(player, isSelf -> new AirtightCannonPacket(barrelPos, lookDirection, ItemStack.EMPTY, hand, 1, isSelf));
    }

    @FunctionalInterface
    public interface BonusDamageFunction {
        float getDamage(LivingEntity entity);
    }

    private record ShotFuel(Gas gasType, long amount) {}
}
