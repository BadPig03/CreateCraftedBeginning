package net.ty.createcraftedbeginning.api.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightArmHandlerEvent;
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightArmHandlers {
    public static void register() {
        AirtightArmHandlerEvent.add(CCBGases.NATURAL_AIR.get(), 1, 2, 2, 0.5f);
        AirtightArmHandlerEvent.add(CCBGases.ENERGIZED_NATURAL_AIR.get(), 0.8f, 4, 4, 1);
        AirtightArmHandlerEvent.add(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 0.65f, 4, 4, 1);
        AirtightArmHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 0.5f, 8, 8, 2);

        AirtightArmHandlerEvent.add(CCBGases.ULTRAWARM_AIR.get(), 0.75f, 2, 2, 0.5f);
        AirtightArmHandlerEvent.add(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 0.6f, 4, 4, 1);
        AirtightArmHandlerEvent.add(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 0.4875f, 4, 4, 1);
        AirtightArmHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 0.15f, 8, 8, 2);

        AirtightArmHandlerEvent.add(CCBGases.ETHEREAL_AIR.get(), 0.5f, 2, 2, 0.5f);
        AirtightArmHandlerEvent.add(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 0.64f, 4, 4, 1);
        AirtightArmHandlerEvent.add(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), 0.325f, 4, 4, 1);
        AirtightArmHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 0.1f, 8, 8, 2);

        AirtightArmHandlerEvent.add(CCBGases.MOIST_AIR.get(), 1, 2, 2, 0.5f);
        AirtightArmHandlerEvent.add(CCBGases.SPORE_AIR.get(), 1, 2, 2, 0.5f);
        AirtightArmHandlerEvent.add(CCBGases.SCULK_AIR.get(), 1, 2, 2, 0.5f);

        AirtightArmHandlerEvent.add(CCBGases.CREATIVE_AIR.get(), 0, 64, 64, 5);
    }
}
