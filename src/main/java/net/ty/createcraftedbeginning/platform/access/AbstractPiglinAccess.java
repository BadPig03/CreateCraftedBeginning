package net.ty.createcraftedbeginning.platform.access;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AbstractPiglinAccess {
    int ccb$getTimeInOverworld();

    void ccb$setTimeInOverworld(int timeInOverworld);
}
