package net.ty.createcraftedbeginning.content.airtights.handlers.drill.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedUltrawarmAirDrillHandler extends UltrawarmAirDrillHandler {
    @Override
    public int getDamageAddition() {
        return 2;
    }

    @Override
    public double getConsumptionMultiplier() {
        return super.getConsumptionMultiplier() * 0.75;
    }
}
