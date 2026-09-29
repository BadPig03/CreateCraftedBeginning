package net.ty.createcraftedbeginning.content.airtights.handlers.arm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlers;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightArmHandlers {
    public static void register() {
        AirtightArmHandlers.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1, 2, 2, 0.5F);
        AirtightArmHandlers.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 0.75F, 4, 4, 1);

        AirtightArmHandlers.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 0.8F, 3, 3, 0.75F);
        AirtightArmHandlers.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 0.6F, 6, 6, 1.5F);

        AirtightArmHandlers.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 0.6F, 4, 4, 1);
        AirtightArmHandlers.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 0.45F, 8, 8, 2);

        AirtightArmHandlers.register(CCBGases.MOIST_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1, 2, 2, 0.5F);
        AirtightArmHandlers.register(CCBGases.SPORE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1, 2, 2, 0.5F);
        AirtightArmHandlers.register(CCBGases.SCULK_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 1, 2, 2, 0.5F);

        AirtightArmHandlers.register(CCBGases.STEAM.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 0.9F, 2.5F, 2.5F, 0.75F);

        AirtightArmHandlers.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, 0, 64, 64, 5);
    }
}
