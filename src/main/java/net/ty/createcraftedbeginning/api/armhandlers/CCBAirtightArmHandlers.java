package net.ty.createcraftedbeginning.api.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightArmHandlers {
    /**
     * Registers the built-in airtight arm handlers.
     */
    public static void register() {
        AirtightArmHandlerUtils.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), 1, 2, 2, 0.5f);
        AirtightArmHandlerUtils.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), 0.8f, 4, 4, 1);
        AirtightArmHandlerUtils.register(CCBGases.PRESSURIZED_NATURAL_AIR.get().getResourceLocation(), 0.65f, 4, 4, 1);
        AirtightArmHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get().getResourceLocation(), 0.5f, 8, 8, 2);

        AirtightArmHandlerUtils.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), 0.75f, 2, 2, 0.5f);
        AirtightArmHandlerUtils.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), 0.6f, 4, 4, 1);
        AirtightArmHandlerUtils.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get().getResourceLocation(), 0.4875f, 4, 4, 1);
        AirtightArmHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), 0.15f, 8, 8, 2);

        AirtightArmHandlerUtils.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), 0.5f, 2, 2, 0.5f);
        AirtightArmHandlerUtils.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), 0.4f, 4, 4, 1);
        AirtightArmHandlerUtils.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get().getResourceLocation(), 0.325f, 4, 4, 1);
        AirtightArmHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), 0.1f, 8, 8, 2);

        AirtightArmHandlerUtils.register(CCBGases.MOIST_AIR.get().getResourceLocation(), 1, 2, 2, 0.5f);
        AirtightArmHandlerUtils.register(CCBGases.SPORE_AIR.get().getResourceLocation(), 1, 2, 2, 0.5f);
        AirtightArmHandlerUtils.register(CCBGases.SCULK_AIR.get().getResourceLocation(), 1, 2, 2, 0.5f);

        AirtightArmHandlerUtils.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), 0, 64, 64, 5);
    }
}
