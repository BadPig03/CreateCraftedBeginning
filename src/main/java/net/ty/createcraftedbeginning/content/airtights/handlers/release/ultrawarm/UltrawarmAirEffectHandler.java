package net.ty.createcraftedbeginning.content.airtights.handlers.release.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandler;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UltrawarmAirEffectHandler implements GasReleaseHandler {
    @Override
    public void apply(GasReleaseContext context) {
        applyEffects(context, 1);
    }

    protected void applyEffects(GasReleaseContext context, int scale) {
        Level level = context.level();
        AABB area = context.effectBounds();
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity entity : entities) {
            if (entity instanceof AbstractPiglin || entity instanceof Hoglin) {
                MobEffectInstance effect = entity.getEffect(CCBMobEffects.ZOMBIFICATION_IMMUNITY);
                if (effect != null && effect.getDuration() > 1) {
                    continue;
                }

                entity.addEffect(new MobEffectInstance(CCBMobEffects.ZOMBIFICATION_IMMUNITY, 20 * scale, 0, true, true), null);
                continue;
            }

            if (!(entity instanceof SnowGolem golem)) {
                continue;
            }

            golem.hurt(level.damageSources().onFire(), golem.getHealth());
        }
    }
}
