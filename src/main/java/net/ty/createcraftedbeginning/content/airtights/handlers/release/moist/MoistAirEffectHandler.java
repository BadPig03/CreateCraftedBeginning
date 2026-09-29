package net.ty.createcraftedbeginning.content.airtights.handlers.release.moist;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandler;
import net.ty.createcraftedbeginning.platform.EntityConversionBridge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MoistAirEffectHandler implements GasReleaseHandler {
    @Override
    public void apply(GasReleaseContext context) {
        applyEffects(context, 1);
    }

    protected void applyEffects(GasReleaseContext context, int scale) {
        Level level = context.level();
        AABB area = context.effectBounds();
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity entity : entities) {
            if (entity.isOnFire()) {
                entity.extinguishFire();
            }
            if (entity.isSensitiveToWater()) {
                entity.hurt(level.damageSources().drown(), scale);
            }
            if (entity instanceof WaterAnimal || entity instanceof Axolotl) {
                entity.setAirSupply(entity.getMaxAirSupply());
            }
            if (!(entity instanceof Zombie zombie)) {
                continue;
            }

            EntityConversionBridge.tryStartUnderwaterConversion(zombie, 300);
        }
    }
}
