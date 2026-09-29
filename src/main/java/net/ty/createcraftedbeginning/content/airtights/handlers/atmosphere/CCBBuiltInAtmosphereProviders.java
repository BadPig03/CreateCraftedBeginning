package net.ty.createcraftedbeginning.content.airtights.handlers.atmosphere;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereProviderRegistry;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBBuiltInAtmosphereProviders {
    private CCBBuiltInAtmosphereProviders() {
    }

    public static void register() {
        AtmosphereProviderRegistry.register(CCBAPI.asResource("default"), Integer.MIN_VALUE, new DefaultAtmosphereProvider());
    }
}
