package net.ty.createcraftedbeginning.api.gas.pressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A single-gas transfer candidate's shared inventory snapshot, keyed by physical
 * compartment identity. Only empty or matching-gas compartments change contents.
 * Supplied contents and returned stacks are copied. Related compartments outside
 * the network are captured on first read; prediction runs synchronously without
 * modifying storage or invoking callbacks that can change it.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressurePrediction {
    private final Map<Object, GasStack> contents = new IdentityHashMap<>();

    public GasPressurePrediction(Map<Object, GasStack> contents) {
        contents.forEach((identity, gas) -> this.contents.put(identity, gas.copy()));
    }

    public GasStack getGas(GasPressureCompartment compartment) {
        return contents.computeIfAbsent(compartment.getCompartmentIdentity(), identity -> compartment.getGasStack().copy()).copy();
    }
}
