package net.ty.createcraftedbeginning.api.gas.effecthandlers.natural;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasOpenPipeEffectHandler;
import net.ty.createcraftedbeginning.mixin.accessor.AbstractPiglinAccessor;
import net.ty.createcraftedbeginning.mixin.accessor.HoglinAccessor;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NaturalAirEffectHandler implements GasOpenPipeEffectHandler {
    @Override
    public void apply(Level level, BlockPos pos, Direction direction, @NotNull Gas gasType) {
        applyEffects(level, pos, direction, gasType.getInflation(), gasType.getTint(), 1);
    }

    protected void applyEffects(@NotNull Level level, BlockPos pos, Direction direction, float inflation, int color, int scale) {
        showOutline(level, pos, direction, inflation, color);
        AABB area = new AABB(pos.relative(direction)).inflate(inflation);
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

            if (entity instanceof AbstractPiglinAccessor piglinAccessor) {
                piglinAccessor.setTimeInOverworld(piglinAccessor.getTimeInOverworld() + scale);
            }
            else if (entity instanceof HoglinAccessor hoglinAccessor) {
                hoglinAccessor.setTimeInOverworld(hoglinAccessor.getTimeInOverworld() + scale);
            }
        }
    }
}
