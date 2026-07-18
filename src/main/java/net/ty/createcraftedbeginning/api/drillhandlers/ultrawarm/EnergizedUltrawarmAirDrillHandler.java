package net.ty.createcraftedbeginning.api.drillhandlers.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedUltrawarmAirDrillHandler extends UltrawarmAirDrillHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public int getDamageAddition() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getConsumptionMultiplier() {
        return 0.72f;
    }
}
