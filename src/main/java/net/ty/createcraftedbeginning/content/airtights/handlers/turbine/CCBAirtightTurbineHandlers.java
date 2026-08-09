package net.ty.createcraftedbeginning.content.airtights.handlers.turbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlerUtils;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightTurbineHandlers {
    public static void register() {
        AirtightTurbineHandlerUtils.register(CCBGases.STEAM.get().getResourceLocation(), 4);
        AirtightTurbineHandlerUtils.register(CCBGases.PRESSURIZED_STEAM.get().getResourceLocation(), 8);

        AirtightTurbineHandlerUtils.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), 1);
        AirtightTurbineHandlerUtils.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), 2);
        AirtightTurbineHandlerUtils.register(CCBGases.PRESSURIZED_NATURAL_AIR.get().getResourceLocation(), 4);
        AirtightTurbineHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get().getResourceLocation(), 8);

        AirtightTurbineHandlerUtils.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), 1);
        AirtightTurbineHandlerUtils.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), 3);
        AirtightTurbineHandlerUtils.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get().getResourceLocation(), 6);
        AirtightTurbineHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), 12);

        AirtightTurbineHandlerUtils.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), 2);
        AirtightTurbineHandlerUtils.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), 4);
        AirtightTurbineHandlerUtils.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get().getResourceLocation(), 8);
        AirtightTurbineHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), 16);

        AirtightTurbineHandlerUtils.register(CCBGases.MOIST_AIR.get().getResourceLocation(), 1);
        AirtightTurbineHandlerUtils.register(CCBGases.SPORE_AIR.get().getResourceLocation(), 1);
        AirtightTurbineHandlerUtils.register(CCBGases.SCULK_AIR.get().getResourceLocation(), 1);

        AirtightTurbineHandlerUtils.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), 16);
    }
}
