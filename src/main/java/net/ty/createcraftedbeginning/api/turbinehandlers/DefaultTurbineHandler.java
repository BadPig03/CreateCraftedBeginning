package net.ty.createcraftedbeginning.api.turbinehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DefaultTurbineHandler implements AirtightTurbineHandler {
    @Override
    public int getEfficiency() {
        return 0;
    }
}
