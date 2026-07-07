package net.ty.createcraftedbeginning.api.drillhandlers.sculk;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SculkAirDrillHandler implements AirtightDrillHandler {
    @Override
    public int getDamageAddition() {
        return 0;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 1;
    }
}
