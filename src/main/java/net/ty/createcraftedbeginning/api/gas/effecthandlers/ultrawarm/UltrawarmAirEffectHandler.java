package net.ty.createcraftedbeginning.api.gas.effecthandlers.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.AirtightPipeEffectHandler;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UltrawarmAirEffectHandler implements AirtightPipeEffectHandler {
    @Override
    public void apply(Level level, BlockPos pos, Direction direction, Gas gasType) {
        applyEffects(level, pos, direction, gasType.getInflation(), gasType.getTint(), 1);
    }

    protected void applyEffects(Level level, BlockPos pos, Direction direction, float inflation, int color, int scale) {
        showOutline(level, pos, direction, inflation, color);
        AABB area = new AABB(pos.relative(direction)).inflate(inflation);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity entity : entities) {
            if (entity instanceof AbstractPiglin || entity instanceof Hoglin) {
                MobEffectInstance effect = entity.getEffect(CCBMobEffects.ZOMBIFICATION_IMMUNITY);
                if (effect != null && effect.getDuration() > 1) {
                    continue;
                }

                entity.addEffect(new MobEffectInstance(CCBMobEffects.ZOMBIFICATION_IMMUNITY, 20 * scale, 0, true, true), null);
            }
            else if (entity instanceof SnowGolem golem) {
                golem.hurt(level.damageSources().onFire(), golem.getHealth());
            }
        }
    }
}
