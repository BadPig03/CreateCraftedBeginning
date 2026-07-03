package net.ty.createcraftedbeginning.api.turbinehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightTurbineHandlerEvent;
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightTurbineHandlers {
    public static void register() {
        AirtightTurbineHandlerEvent.add(CCBGases.NATURAL_AIR.get(), 1);
        AirtightTurbineHandlerEvent.add(CCBGases.ENERGIZED_NATURAL_AIR.get(), 2);
        AirtightTurbineHandlerEvent.add(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 4);
        AirtightTurbineHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 8);

        AirtightTurbineHandlerEvent.add(CCBGases.ULTRAWARM_AIR.get(), 1);
        AirtightTurbineHandlerEvent.add(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 3);
        AirtightTurbineHandlerEvent.add(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 6);
        AirtightTurbineHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 12);

        AirtightTurbineHandlerEvent.add(CCBGases.ETHEREAL_AIR.get(), 2);
        AirtightTurbineHandlerEvent.add(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 4);
        AirtightTurbineHandlerEvent.add(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), 8);
        AirtightTurbineHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 16);

        AirtightTurbineHandlerEvent.add(CCBGases.MOIST_AIR.get(), 1);
        AirtightTurbineHandlerEvent.add(CCBGases.SPORE_AIR.get(), 1);
        AirtightTurbineHandlerEvent.add(CCBGases.SCULK_AIR.get(), 1);

        AirtightTurbineHandlerEvent.add(CCBGases.CREATIVE_AIR.get(), 16);
    }
}
