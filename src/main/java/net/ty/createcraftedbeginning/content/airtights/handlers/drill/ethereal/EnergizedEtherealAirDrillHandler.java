package net.ty.createcraftedbeginning.content.airtights.handlers.drill.ethereal;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedEtherealAirDrillHandler extends EtherealAirDrillHandler {
    @Override
    public int getDamageAddition() {
        return 3;
    }

    @Override
    public float getConsumptionMultiplier() {
        return super.getConsumptionMultiplier() * 0.75F;
    }
}
