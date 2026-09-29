package net.ty.createcraftedbeginning.content.airtights.handlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.airtights.handlers.arm.CCBAirtightArmHandlers;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.CCBAirtightArmorsHandlers;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.CCBAirtightCannonHandlers;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.CCBAirtightDrillHandlers;
import net.ty.createcraftedbeginning.content.airtights.handlers.engine.CCBAirtightEngineHandlers;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.CCBGasReleaseHandlers;
import net.ty.createcraftedbeginning.content.airtights.handlers.thermoregulator.CCBAirtightThermoregulatorHandlers;
import net.ty.createcraftedbeginning.content.airtights.handlers.turbine.CCBAirtightTurbineHandlers;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBBuiltInAirtightHandlers {
    private CCBBuiltInAirtightHandlers() {
    }

    public static void register() {
        CCBAirtightArmHandlers.register();
        CCBAirtightArmorsHandlers.register();
        CCBAirtightCannonHandlers.register();
        CCBAirtightDrillHandlers.register();
        CCBAirtightEngineHandlers.register();
        CCBAirtightThermoregulatorHandlers.register();
        CCBAirtightTurbineHandlers.register();
        CCBGasReleaseHandlers.register();
    }
}
