package net.ty.createcraftedbeginning.content.mobeffects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import org.jetbrains.annotations.NotNull;

public class ZombificationEffect extends MobEffect {
    public ZombificationEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity livingEntity, int amplifier) {
        if (livingEntity.hasEffect(CCBMobEffects.ZOMBIFICATION_IMMUNITY) || !(livingEntity instanceof AbstractPiglin) && !(livingEntity instanceof Hoglin)) {
            return true;
        }

        livingEntity.setTicksFrozen(livingEntity.getTicksFrozen() + 7);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
