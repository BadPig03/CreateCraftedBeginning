package net.ty.createcraftedbeginning.api.gas.drillhandlers.ethereal;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEtherealAirDrillHandler extends EtherealAirDrillHandler {
    @Override
    public float getConsumptionMultiplier() {
        return 0.37f;
    }
}
