package net.ty.createcraftedbeginning.content.mobeffects;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SteamScaldEffect extends MobEffect {
    private static final int INTERVAL_TICKS = 25;
    private static final float MIN_REMAINING_HEALTH = 1;

    public SteamScaldEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (livingEntity.level().isClientSide || !livingEntity.isAlive() || livingEntity.tickCount % INTERVAL_TICKS != 0) {
            return true;
        }

        float health = livingEntity.getHealth();
        if (health <= MIN_REMAINING_HEALTH) {
            return true;
        }

        float damage = Math.min(amplifier / 2.0F + 1, health - MIN_REMAINING_HEALTH);
        livingEntity.hurt(livingEntity.damageSources().inFire(), damage);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
