package net.ty.createcraftedbeginning.content.airtights.handlers.release.moist;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HighPressureMoistAirEffectHandler extends MoistAirEffectHandler {
    @Override
    public void apply(GasReleaseContext context) {
        applyEffects(context, 10);
    }
}
