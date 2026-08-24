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
    static void onEffectExpired(Expired event) {
        if (!isZombificationImmunity(event.getEffectInstance())) {
            return;
        }

        setZombificationImmunity(event.getEntity(), false);
    }

    @SubscribeEvent
    static void onEffectRemove(Remove event) {
        if (!isZombificationImmunity(event.getEffectInstance())) {
            return;
        }

        setZombificationImmunity(event.getEntity(), false);
    }

    private static boolean isZombificationImmunity(@Nullable MobEffectInstance effectInstance) {
        return effectInstance != null && effectInstance.is(CCBMobEffects.ZOMBIFICATION_IMMUNITY);
    }

    private static void setZombificationImmunity(LivingEntity livingEntity, boolean isImmune) {
        if (livingEntity instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(isImmune);
        }
        else if (livingEntity instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(isImmune);
        }
    }

    @Override
    public void onEffectStarted(LivingEntity livingEntity, int amplifier) {
        setZombificationImmunity(livingEntity, true);
    }
}
