package net.ty.createcraftedbeginning.content.airtights.handlers.engine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlers;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightEngineHandlers {
    public static void register() {
        AirtightEngineHandlers.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1, 4);
        AirtightEngineHandlers.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 10, 4);

        AirtightEngineHandlers.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1.5F, 6);
        AirtightEngineHandlers.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 15, 6);

        AirtightEngineHandlers.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 2, 8);
        AirtightEngineHandlers.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 20, 8);

        AirtightEngineHandlers.register(CCBGases.MOIST_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1, 4);
        AirtightEngineHandlers.register(CCBGases.MOIST_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 10, 4);

        AirtightEngineHandlers.register(CCBGases.SPORE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1, 4);
        AirtightEngineHandlers.register(CCBGases.SPORE_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 10, 4);

        AirtightEngineHandlers.register(CCBGases.SCULK_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1, 4);
        AirtightEngineHandlers.register(CCBGases.SCULK_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 1, 4);

        AirtightEngineHandlers.register(CCBGases.STEAM.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 2, 8);
        AirtightEngineHandlers.register(CCBGases.STEAM.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 20, 8);

        AirtightEngineHandlers.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 32, 8);
    }
}
