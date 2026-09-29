package net.ty.createcraftedbeginning.content.airtights.handlers.turbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlers;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightTurbineHandlers {
    public static void register() {
        AirtightTurbineHandlers.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1);
        AirtightTurbineHandlers.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 4);
        AirtightTurbineHandlers.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 2);
        AirtightTurbineHandlers.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 8);

        AirtightTurbineHandlers.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1.5F);
        AirtightTurbineHandlers.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 6);
        AirtightTurbineHandlers.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 3);
        AirtightTurbineHandlers.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 12);

        AirtightTurbineHandlers.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 2);
        AirtightTurbineHandlers.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 8);
        AirtightTurbineHandlers.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 4);
        AirtightTurbineHandlers.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 16);

        AirtightTurbineHandlers.register(CCBGases.MOIST_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1);
        AirtightTurbineHandlers.register(CCBGases.MOIST_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 4);

        AirtightTurbineHandlers.register(CCBGases.SPORE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1);
        AirtightTurbineHandlers.register(CCBGases.SPORE_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 4);

        AirtightTurbineHandlers.register(CCBGases.SCULK_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1);
        AirtightTurbineHandlers.register(CCBGases.SCULK_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 4);

        AirtightTurbineHandlers.register(CCBGases.STEAM.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 4);
        AirtightTurbineHandlers.register(CCBGases.STEAM.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, 12);

        AirtightTurbineHandlers.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 16);
    }
}
