package net.ty.createcraftedbeginning.api.gas;

import java.util.function.Consumer;

public class SmartGasTank extends GasTank {
    private final Consumer<GasStack> updateCallback;

    public SmartGasTank(long capacity, Consumer<GasStack> updateCallback) {
		super(capacity);
		this.updateCallback = updateCallback;
	}

	@Override
	protected void onContentsChanged() {
		super.onContentsChanged();
		updateCallback.accept(getGas());
	}

	@Override
	public void setGas(GasStack stack) {
		super.setGas(stack);
		updateCallback.accept(stack);
	}
}
