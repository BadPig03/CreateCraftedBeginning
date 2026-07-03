package net.ty.createcraftedbeginning.api.enginehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightEngineHandlerEvent;
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightEngineHandlers {
    public static void register() {
        AirtightEngineHandlerEvent.add(CCBGases.NATURAL_AIR.get(), 1);
        AirtightEngineHandlerEvent.add(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 8);

        AirtightEngineHandlerEvent.add(CCBGases.ULTRAWARM_AIR.get(), 1);
        AirtightEngineHandlerEvent.add(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 12);

        AirtightEngineHandlerEvent.add(CCBGases.ETHEREAL_AIR.get(), 2);
        AirtightEngineHandlerEvent.add(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), 16);

        AirtightEngineHandlerEvent.add(CCBGases.MOIST_AIR.get(), 1);
        AirtightEngineHandlerEvent.add(CCBGases.SPORE_AIR.get(), 1);
        AirtightEngineHandlerEvent.add(CCBGases.SCULK_AIR.get(), 1);

        AirtightEngineHandlerEvent.add(CCBGases.CREATIVE_AIR.get(), 16);
    }
}
