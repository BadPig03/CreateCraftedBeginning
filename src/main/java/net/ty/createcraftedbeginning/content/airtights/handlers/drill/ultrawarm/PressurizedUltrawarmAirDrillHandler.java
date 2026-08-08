package net.ty.createcraftedbeginning.content.airtights.handlers.drill.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedUltrawarmAirDrillHandler extends UltrawarmAirDrillHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public float getConsumptionMultiplier() {
        return 0.51f;
    }
}
