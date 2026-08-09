package net.ty.createcraftedbeginning.content.airtights.handlers.drainage.ethereal;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEnergizedEtherealAirEffectHandler extends EtherealAirEffectHandler {
    @Override
    public float getInflation() {
        return 2;
    }
}
