package net.ty.createcraftedbeginning.content.airtights.handlers.release.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HighPressureUltrawarmAirEffectHandler extends UltrawarmAirEffectHandler {
    @Override
    public void apply(GasReleaseContext context) {
        applyEffects(context, 10);
    }
}
