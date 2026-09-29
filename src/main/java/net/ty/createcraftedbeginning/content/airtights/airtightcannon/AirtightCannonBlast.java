package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCannonBlast {
    private static final double MIN_RAY_OFFSET_LENGTH_SQR = 1.0E-8;
    private static final double RAY_START_OFFSET = 1.0E-4;

    private AirtightCannonBlast() {
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

    public static void applyEffects(List<LivingEntity> entities, Supplier<MobEffectInstance> instance) {
        for (LivingEntity entity : entities) {
            entity.addEffect(instance.get());
        }
    }

    private static List<LivingEntity> getNearbyEntities(Level level, Vec3 pos, float radius, Entity source, @Nullable Entity owner) {
        float searchRadius = radius * 2;
        float searchRadiusSqr = Mth.square(searchRadius);
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

    @FunctionalInterface
    public interface BonusDamageFunction {
        float getDamage(LivingEntity entity);
    }
}
