package net.ty.createcraftedbeginning.content.airtights.handlers.thermoregulator;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.api.registry.SimpleRegistry.Provider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandlerUtils;
import net.ty.createcraftedbeginning.content.airtights.handlers.thermoregulator.contents.BlazeBurnerThermoregulatorHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.thermoregulator.contents.BreezeCoolerThermoregulatorHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.thermoregulator.contents.PassiveBoilerHeatersThermoregulatorHandler;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightThermoregulatorHandlers {
    public static void register() {
        AirtightThermoregulatorHandlerUtils.register(AllBlocks.BLAZE_BURNER.get(), new BlazeBurnerThermoregulatorHandler());
        AirtightThermoregulatorHandlerUtils.register(CCBBlocks.BREEZE_COOLER_BLOCK.get(), new BreezeCoolerThermoregulatorHandler());

        AirtightThermoregulatorHandler.REGISTRY.registerProvider(Provider.forBlockTag(AllBlockTags.PASSIVE_BOILER_HEATERS.tag, new PassiveBoilerHeatersThermoregulatorHandler()));
    }
}
