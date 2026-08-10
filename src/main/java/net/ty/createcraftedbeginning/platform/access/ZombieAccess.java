package net.ty.createcraftedbeginning.platform.access;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ZombieAccess {
    boolean ccb$convertsInWater();

    void ccb$startUnderWaterConversion(int time);
}
