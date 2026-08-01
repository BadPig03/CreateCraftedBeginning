package net.ty.createcraftedbeginning.api.enginehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightEngineHandlers {
    /**
     * Registers the built-in airtight engine handlers.
     */
    public static void register() {
        AirtightEngineHandlerUtils.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), 1);
        AirtightEngineHandlerUtils.register(CCBGases.PRESSURIZED_NATURAL_AIR.get().getResourceLocation(), 8);

        AirtightEngineHandlerUtils.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), 1);
        AirtightEngineHandlerUtils.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get().getResourceLocation(), 12);

        AirtightEngineHandlerUtils.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), 2);
        AirtightEngineHandlerUtils.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get().getResourceLocation(), 16);

        AirtightEngineHandlerUtils.register(CCBGases.MOIST_AIR.get().getResourceLocation(), 1);
        AirtightEngineHandlerUtils.register(CCBGases.SPORE_AIR.get().getResourceLocation(), 1);
        AirtightEngineHandlerUtils.register(CCBGases.SCULK_AIR.get().getResourceLocation(), 1);

        AirtightEngineHandlerUtils.register(CCBGases.STEAM.get().getResourceLocation(), 2);

        AirtightEngineHandlerUtils.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), 16);
    }
}
