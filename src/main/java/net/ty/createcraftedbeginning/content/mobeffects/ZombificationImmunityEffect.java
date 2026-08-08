package net.ty.createcraftedbeginning.content.mobeffects;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public class ZombificationImmunityEffect extends MobEffect {
    public ZombificationImmunityEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @SubscribeEvent
    public static void onEffectExpired(Expired event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (!isZombificationImmunity(effect)) {
            return;
        }

        setZombificationImmunity(event.getEntity(), false);
    }

    @SubscribeEvent
    public static void onEffectRemove(Remove event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (!isZombificationImmunity(effect)) {
            return;
        }

        setZombificationImmunity(event.getEntity(), false);
    }

    private static boolean isZombificationImmunity(@Nullable MobEffectInstance effect) {
        return effect != null && effect.is(CCBMobEffects.ZOMBIFICATION_IMMUNITY);
    }

    private static void setZombificationImmunity(LivingEntity entity, boolean immune) {
        if (entity instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(immune);
        }
        else if (entity instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(immune);
        }
    }

    @Override
    public void onEffectStarted(LivingEntity livingEntity, int amplifier) {
        setZombificationImmunity(livingEntity, true);
    }
}
