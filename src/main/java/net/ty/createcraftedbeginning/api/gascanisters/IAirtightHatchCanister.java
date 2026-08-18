package net.ty.createcraftedbeginning.api.gascanisters;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IAirtightHatchCanister extends IGasCanisterContainer {
    HatchCanisterType getAirtightHatchType();

    GasStack getAirtightHatchContents();

    long getAirtightHatchCapacity(GasStack contents);
    
    boolean setAirtightHatchContents(GasStack contents);

    enum HatchCanisterType {
        NORMAL,
        CREATIVE
    }
}
