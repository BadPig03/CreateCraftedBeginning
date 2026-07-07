package net.ty.createcraftedbeginning.api.drillhandlers.creative;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeDrillHandler implements AirtightDrillHandler {
    @Override
    public int getDamageAddition() {
        return Integer.MAX_VALUE - 1;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 0;
    }
}
