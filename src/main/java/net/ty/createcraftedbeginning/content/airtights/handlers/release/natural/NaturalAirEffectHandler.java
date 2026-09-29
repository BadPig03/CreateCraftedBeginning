package net.ty.createcraftedbeginning.content.airtights.handlers.release.natural;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandler;
import net.ty.createcraftedbeginning.platform.EntityConversionBridge;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class NaturalAirEffectHandler implements GasReleaseHandler {
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
                if (entity.hasEffect(CCBMobEffects.ZOMBIFICATION_IMMUNITY)) {
                    continue;
                }

                MobEffectInstance effect = entity.getEffect(CCBMobEffects.ZOMBIFICATION);
                if (effect != null && effect.getDuration() > 1) {
                    continue;
                }

                entity.addEffect(new MobEffectInstance(CCBMobEffects.ZOMBIFICATION, 20, 0, true, true), null);
            }

            EntityConversionBridge.advanceOverworldConversion(entity, scale);
        }
    }
}
