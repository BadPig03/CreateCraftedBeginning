package net.ty.createcraftedbeginning.api.gas.effecthandlers.moist;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasOpenPipeEffectHandler;
import net.ty.createcraftedbeginning.mixin.accessor.ZombieAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MoistAirEffectHandler implements GasOpenPipeEffectHandler {
    @Override
    public void apply(Level level, BlockPos pos, Direction direction, @NotNull Gas gasType) {
        applyEffects(level, pos, direction, gasType.getInflation(), gasType.getTint());
    }

    protected void applyEffects(@NotNull Level level, BlockPos pos, Direction direction, float inflation, int color) {
        showOutline(level, pos, direction, inflation, color);
        AABB area = new AABB(pos.relative(direction)).inflate(inflation);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity entity : entities) {
            if (entity.isOnFire()) {
                entity.extinguishFire();
            }
            if (entity.isSensitiveToWater()) {
                entity.hurt(level.damageSources().drown(), 1);
            }
            if (entity instanceof WaterAnimal || entity instanceof Axolotl) {
                entity.setAirSupply(entity.getMaxAirSupply());
            }
            if (entity instanceof Zombie zombie && !zombie.isUnderWaterConverting() && entity instanceof ZombieAccessor zombieAccessor && zombieAccessor.ccb$convertsInWater()) {
                zombieAccessor.ccb$startUnderWaterConversion(300);
            }
        }
    }
}
