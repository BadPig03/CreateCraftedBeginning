package net.ty.createcraftedbeginning.gas.storage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasTankState(GasTankLimits limits, GasStack contents) {
    public GasTankState {
        contents = contents.copy();
    }

    @Override
    public GasStack contents() {
        return contents.copy();
    }
}
