package net.ty.createcraftedbeginning.api.gas.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ArmHandlerUtils {
    private ArmHandlerUtils() {
    }

    public static AirtightExtendArmHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    public static AirtightExtendArmHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightExtendArmHandler armHandler = AirtightExtendArmHandler.REGISTRY.get(gasType);
        if (armHandler == null) {
            return new DefaultArmHandler();
        }
        return armHandler;
    }
}
