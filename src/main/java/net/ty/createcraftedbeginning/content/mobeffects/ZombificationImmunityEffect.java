package net.ty.createcraftedbeginning.content.mobeffects;

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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class ZombificationImmunityEffect extends MobEffect {
    public ZombificationImmunityEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void onEffectStarted(@NotNull LivingEntity livingEntity, int amplifier) {
        if (livingEntity instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(true);
        }
        else if (livingEntity instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(@NotNull Expired event) {
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance == null || !effectInstance.is(CCBMobEffects.ZOMBIFICATION_IMMUNITY)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (entity instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(false);
        }
        else if (entity instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(false);
        }
    }

    @SubscribeEvent
    public static void onEffectRemove(@NotNull Remove event) {
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance == null || !effectInstance.is(CCBMobEffects.ZOMBIFICATION_IMMUNITY)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (entity instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(false);
        }
        else if (entity instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(false);
        }
    }
}
