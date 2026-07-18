package net.ty.createcraftedbeginning.api.drillhandlers.ethereal;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEnergizedEtherealAirDrillHandler extends EtherealAirDrillHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public int getDamageAddition() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getConsumptionMultiplier() {
        return 0.1f;
    }
}
