package net.ty.createcraftedbeginning.content.airtights.handlers.drill.creative;

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
    public double getConsumptionMultiplier() {
        return 0;
    }
}
