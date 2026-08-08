package net.ty.createcraftedbeginning.content.airtights.handlers.engine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlerUtils;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightEngineHandlers {
    private static final int FULL_LEVEL = 8;
    private static final int SPECIAL_AIR_MAX_LEVEL = 4;

    /**
     * Registers the built-in airtight engine handlers.
     */
    public static void register() {
        AirtightEngineHandlerUtils.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), 1, FULL_LEVEL);
        AirtightEngineHandlerUtils.register(CCBGases.PRESSURIZED_NATURAL_AIR.get().getResourceLocation(), 10, FULL_LEVEL);

        AirtightEngineHandlerUtils.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), 1.5, FULL_LEVEL);
        AirtightEngineHandlerUtils.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get().getResourceLocation(), 15, FULL_LEVEL);

        AirtightEngineHandlerUtils.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), 2, FULL_LEVEL);
        AirtightEngineHandlerUtils.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get().getResourceLocation(), 20, FULL_LEVEL);

        AirtightEngineHandlerUtils.register(CCBGases.MOIST_AIR.get().getResourceLocation(), 1, SPECIAL_AIR_MAX_LEVEL);
        AirtightEngineHandlerUtils.register(CCBGases.SPORE_AIR.get().getResourceLocation(), 1, SPECIAL_AIR_MAX_LEVEL);
        AirtightEngineHandlerUtils.register(CCBGases.SCULK_AIR.get().getResourceLocation(), 1, SPECIAL_AIR_MAX_LEVEL);

        AirtightEngineHandlerUtils.register(CCBGases.STEAM.get().getResourceLocation(), 2, FULL_LEVEL);
        AirtightEngineHandlerUtils.register(CCBGases.PRESSURIZED_STEAM.get().getResourceLocation(), 20, FULL_LEVEL);

        AirtightEngineHandlerUtils.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), 32, FULL_LEVEL);
    }
}
