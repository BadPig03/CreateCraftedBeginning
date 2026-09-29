package net.ty.createcraftedbeginning.api.canister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightHatchCanister extends GasCanisterContainer {
    HatchCanisterType getAirtightHatchType();

    GasStack getAirtightHatchContents();

    boolean setAirtightHatchContents(GasStack contents);

    enum HatchCanisterType {
        NORMAL,
        CREATIVE
    }
}
