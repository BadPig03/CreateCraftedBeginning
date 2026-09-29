package net.ty.createcraftedbeginning.api.enginehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface AirtightEngineHandler {
    int MAX_LEVEL = 8;

    double getWorkFactor();

    default int getMaxLevel() {
        return 8;
    }
}
