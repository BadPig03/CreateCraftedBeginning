package net.ty.createcraftedbeginning.api.drainagehandlers.natural;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandler;
import net.ty.createcraftedbeginning.mixin.accessor.AbstractPiglinAccessor;
import net.ty.createcraftedbeginning.mixin.accessor.HoglinAccessor;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class NaturalAirEffectHandler implements AirtightDrainageHandler {
    @Override
    public float getInflation() {
        return 1;
    }

    @Override
    public void apply(Level level, BlockPos pos, Direction direction, Gas gasType) {
        applyEffects(level, pos, direction, gasType.getTint(), 1);
    }

    protected void applyEffects(Level level, BlockPos pos, Direction direction, int tint, int scale) {
        showOutline(level, pos, direction, getInflation(), tint);
        AABB area = new AABB(pos.relative(direction)).inflate(getInflation());
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
