package net.ty.createcraftedbeginning.gas.storage;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SmartGasTank extends GasTank {
    private final Runnable updateCallback;

    public SmartGasTank(long volume, long maxPressurePa, Runnable updateCallback) {
        super(volume, maxPressurePa);
        this.updateCallback = updateCallback;
    }

    @Override
    protected void onStateChanged() {
        updateCallback.run();
    }
}
