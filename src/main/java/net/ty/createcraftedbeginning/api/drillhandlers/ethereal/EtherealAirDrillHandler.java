package net.ty.createcraftedbeginning.api.drillhandlers.ethereal;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EtherealAirDrillHandler implements AirtightDrillHandler {
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
        return 0.8f;
    }
}
