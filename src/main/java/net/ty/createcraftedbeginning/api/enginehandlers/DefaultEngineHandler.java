package net.ty.createcraftedbeginning.api.enginehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DefaultEngineHandler implements AirtightEngineHandler {
    @Override
    public int getEfficiency() {
        return 0;
    }
}
