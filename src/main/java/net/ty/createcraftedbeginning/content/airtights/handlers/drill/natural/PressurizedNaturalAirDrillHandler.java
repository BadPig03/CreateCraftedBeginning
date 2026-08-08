package net.ty.createcraftedbeginning.content.airtights.handlers.drill.natural;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedNaturalAirDrillHandler extends NaturalAirDrillHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public float getConsumptionMultiplier() {
        return 0.65f;
    }
}
