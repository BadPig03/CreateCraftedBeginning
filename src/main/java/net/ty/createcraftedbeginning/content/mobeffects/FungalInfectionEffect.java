package net.ty.createcraftedbeginning.content.mobeffects;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.MushroomCow.MushroomType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBEntityFlags;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public final class FungalInfectionEffect extends MobEffect {
    private static final int INTERVAL_TICKS = 80;
    private static final int SPREAD_DURATION = 600;

    public FungalInfectionEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onEffectApplicable(Applicable event) {
        if (!event.getEffectInstance().is(CCBMobEffects.FUNGAL_INFECTION) || !isImmune(event.getEntity())) {
            return;
        }

        event.setResult(Result.DO_NOT_APPLY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(event.getSource() instanceof FungalBurstDamageSource source) || target.level().isClientSide || !target.isAlive() || isImmune(target) || Explosion.getSeenPercent(source.center, target) <= 0) {
            return;
        }

        int amplifier = source.amplifier;
        target.addEffect(new MobEffectInstance(CCBMobEffects.FUNGAL_INFECTION, SPREAD_DURATION * (amplifier + 1), amplifier), source.getEntity());
    }

    private static boolean isImmune(LivingEntity livingEntity) {
        return CCBEntityFlags.IMMUNE_TO_FUNGAL_INFECTION.matches(livingEntity);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (isImmune(livingEntity)) {
            return false;
        }

        if (!livingEntity.level().isClientSide && livingEntity.isAlive()) {
            livingEntity.hurt(livingEntity.damageSources().starve(), amplifier + 1);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % INTERVAL_TICKS == 0;
    }

    @Override
    public void onEffectStarted(LivingEntity livingEntity, int amplifier) {
        if (livingEntity.level().isClientSide || !livingEntity.isAlive() || isImmune(livingEntity) || livingEntity.getType() != EntityType.COW || !(livingEntity instanceof Cow cow)) {
            return;
        }

        MushroomCow mushroomCow = cow.convertTo(EntityType.MOOSHROOM, true);
        if (mushroomCow == null) {
            return;
        }

        mushroomCow.setVariant(cow.getRandom().nextBoolean() ? MushroomType.RED : MushroomType.BROWN);
        mushroomCow.setAge(cow.getAge());
        mushroomCow.setHealth(cow.getHealth());
        mushroomCow.setNoGravity(cow.isNoGravity());
        mushroomCow.setDeltaMovement(cow.getDeltaMovement());
    }

    @Override
    public void onMobRemoved(LivingEntity livingEntity, int amplifier, RemovalReason reason) {
        if (reason != RemovalReason.KILLED || isImmune(livingEntity) || !(livingEntity.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = livingEntity.position().add(0, livingEntity.getBbHeight() / 2, 0);
        level.sendParticles(ParticleTypes.MYCELIUM, center.x, center.y, center.z, 60, 1, 1, 1, 0.1);
        level.explode(livingEntity, new FungalBurstDamageSource(livingEntity, center, amplifier), null, center.x, center.y, center.z, 2, false, ExplosionInteraction.NONE);
    }

    private static final class FungalBurstDamageSource extends DamageSource {
        private final Vec3 center;
        private final int amplifier;

        private FungalBurstDamageSource(LivingEntity source, Vec3 center, int amplifier) {
            super(source.damageSources().explosion(source, source).typeHolder(), source);
            this.center = center;
            this.amplifier = amplifier;
        }
    }
}
