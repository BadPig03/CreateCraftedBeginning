package net.ty.createcraftedbeginning.content.airtights.handlers.release.potion;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandler;
import net.ty.createcraftedbeginning.content.airtights.potiongas.PotionGas;
import net.ty.createcraftedbeginning.foundation.NbtValues;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PotionGasEffectHandler implements GasReleaseHandler {
    private static final int REFRESH_THRESHOLD = 40;
    private static final String COMPOUND_KEY_INSTANT_COOLDOWNS = "createcraftedbeginning:instant_potion_gas_cooldowns";

    private static void applyInstantEffects(Level level, List<LivingEntity> targets, MobEffectInstance source) {
        Holder<MobEffect> effectHolder = source.getEffect();
        MobEffect effect = effectHolder.value();
        String effectId = effectHolder.getRegisteredName();
        long gameTime = level.getGameTime();
        targets.sort(Comparator.comparingInt(LivingEntity::getId));
        int applied = 0;
        for (LivingEntity target : targets) {
            CompoundTag entityData = target.getPersistentData();
            CompoundTag cooldowns = NbtValues.getCompoundOrEmpty(entityData, COMPOUND_KEY_INSTANT_COOLDOWNS);
            if (gameTime < NbtValues.getLongOrDefault(cooldowns, effectId, Long.MIN_VALUE)) {
                continue;
            }

            cooldowns.getAllKeys().removeIf(key -> cooldowns.getLong(key) <= gameTime);
            cooldowns.putLong(effectId, gameTime + PotionGas.INSTANT_COOLDOWN_TICKS);
            entityData.put(COMPOUND_KEY_INSTANT_COOLDOWNS, cooldowns);
            effect.applyInstantenousEffect(null, null, target, source.getAmplifier(), 1.0);
            applied++;
            if (applied < PotionGas.MAX_TARGETS) {
                continue;
            }

            break;
        }
    }

    @Override
    public long getEffectInterval(GasStack gas) {
        MobEffectInstance effect = PotionGas.findReleaseEffect(gas);
        if (effect != null && effect.getEffect().value().isInstantenous()) {
            return PotionGas.INSTANT_GAS_PER_APPLICATION;
        }

        return PotionGas.GAS_PER_APPLICATION;
    }

    @Override
    public int getEffectCooldown(GasStack gas) {
        MobEffectInstance effect = PotionGas.findReleaseEffect(gas);
        if (effect != null && effect.getEffect().value().isInstantenous()) {
            return PotionGas.INSTANT_COOLDOWN_TICKS;
        }

        return 1;
    }

    @Override
    public void apply(GasReleaseContext context) {
        Level level = context.level();
        if (level.isClientSide) {
            return;
        }

        MobEffectInstance source = PotionGas.findReleaseEffect(context.gas());
        if (source == null) {
            return;
        }

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, context.effectBounds(), entity -> entity.isAlive() && !entity.isSpectator() && entity.isAffectedByPotions());
        if (source.getEffect().value().isInstantenous()) {
            applyInstantEffects(level, targets, source);
            return;
        }

        targets.sort(Comparator.comparingInt((LivingEntity entity) -> {
            MobEffectInstance current = entity.getEffect(source.getEffect());
            if (current == null) {
                return 0;
            }

            return current.getDuration();
        }).thenComparingInt(LivingEntity::getId));
        int applied = 0;
        for (LivingEntity target : targets) {
            MobEffectInstance current = target.getEffect(source.getEffect());
            if (current != null && (current.getAmplifier() > source.getAmplifier() || current.getAmplifier() == source.getAmplifier() && (current.isInfiniteDuration() || current.getDuration() > REFRESH_THRESHOLD))) {
                continue;
            }

            if (!target.addEffect(new MobEffectInstance(source.getEffect(), PotionGas.EFFECT_DURATION, source.getAmplifier()))) {
                continue;
            }

            applied++;
            if (applied < PotionGas.MAX_TARGETS) {
                continue;
            }

            break;
        }
    }
}
