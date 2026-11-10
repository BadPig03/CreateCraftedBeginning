package net.ty.createcraftedbeginning.gas.visual;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GasStorageReadout {
    AMOUNT_ONLY(false, false),
    PRESSURE_ONLY(false, true),
    FULL(true, true);

    private final boolean capacity;
    private final boolean pressure;

    GasStorageReadout(boolean capacity, boolean pressure) {
        this.capacity = capacity;
        this.pressure = pressure;
    }

    public boolean capacity() {
        return capacity;
    }

    public boolean pressure() {
        return pressure;
    }
}
