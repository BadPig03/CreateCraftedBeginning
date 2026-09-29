package net.ty.createcraftedbeginning.content.airtights.handlers.release.natural;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HighPressureEnergizedNaturalAirEffectHandler extends NaturalAirEffectHandler {
    @Override
    public void apply(GasReleaseContext context) {
        applyEffects(context, 20);
    }
}
