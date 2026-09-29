package net.ty.createcraftedbeginning.api.gas.handler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasStorageHandler extends GasHandler {
    GasPressureCompartment getPressureCompartment(int tank);

    default long getTankVolume(int tank) {
        return getPressureCompartment(tank).getVolume();
    }

    default long getTankPressurePa(int tank) {
        return getPressureCompartment(tank).getPressurePa();
    }

    default long getTankMaxPressurePa(int tank) {
        return getPressureCompartment(tank).getMaxPressurePa();
    }

    default long getTankMaxAmount(int tank) {
        return getPressureCompartment(tank).getMaxAmount();
    }

    default PressureModel getTankPressureModel(int tank) {
        return getPressureCompartment(tank).getPressureModel();
    }
}
