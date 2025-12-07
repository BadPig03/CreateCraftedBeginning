package net.ty.createcraftedbeginning.api.gas;

import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import java.util.function.Consumer;

public class SmartGasTank extends GasTank {
    private final Consumer<GasStack> updateCallback;

    public SmartGasTank(long capacity, Consumer<GasStack> updateCallback) {
        super(capacity);
        this.updateCallback = updateCallback;
    }

    @Override
    public void setGasStack(GasStack stack) {
        super.setGasStack(stack);
        updateCallback.accept(stack);
    }

    @Override
    protected void onContentsChanged() {
        super.onContentsChanged();
        updateCallback.accept(getGasStack());
    }
}
