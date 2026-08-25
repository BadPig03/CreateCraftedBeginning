package net.ty.createcraftedbeginning.platform;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.ty.createcraftedbeginning.platform.access.OverworldConversionAccess;
import net.ty.createcraftedbeginning.platform.access.ZombieAccess;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class EntityConversionBridge {
    private EntityConversionBridge() {
    }

    public static void advanceOverworldConversion(LivingEntity entity, int amount) {
        if (!(entity instanceof OverworldConversionAccess conversionAccess)) {
            return;
        }

        conversionAccess.ccb$setTimeInOverworld(conversionAccess.ccb$getTimeInOverworld() + amount);
    }

    public static void tryStartUnderwaterConversion(Zombie zombie, int conversionTime) {
        if (zombie.isUnderWaterConverting() || !(zombie instanceof ZombieAccess zombieAccess) || !zombieAccess.ccb$convertsInWater()) {
            return;
        }

        zombieAccess.ccb$startUnderWaterConversion(conversionTime);
    }
}
