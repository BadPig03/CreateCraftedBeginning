package net.ty.createcraftedbeginning.api.drillhandlers.moist;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MoistAirDrillHandler implements AirtightDrillHandler {
    @Override
    public int getDamageAddition() {
        return 0;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 1;
    }
}
