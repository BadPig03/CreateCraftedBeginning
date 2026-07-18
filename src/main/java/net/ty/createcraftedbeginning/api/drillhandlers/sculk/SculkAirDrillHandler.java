package net.ty.createcraftedbeginning.api.drillhandlers.sculk;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SculkAirDrillHandler implements AirtightDrillHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public int getDamageAddition() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getConsumptionMultiplier() {
        return 1;
    }
}
