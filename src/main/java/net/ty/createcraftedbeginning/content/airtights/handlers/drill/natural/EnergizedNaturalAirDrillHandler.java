package net.ty.createcraftedbeginning.content.airtights.handlers.drill.natural;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedNaturalAirDrillHandler extends NaturalAirDrillHandler {
    @Override
    public int getDamageAddition() {
        return 1;
    }

    @Override
    public double getConsumptionMultiplier() {
        return super.getConsumptionMultiplier() * 0.75;
    }
}
