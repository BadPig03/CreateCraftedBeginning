package net.ty.createcraftedbeginning.api.thermoregulatorhandlers;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.api.registry.SimpleRegistry.Provider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.contents.BlazeBurnerThermoregulatorHandler;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.contents.BreezeCoolerThermoregulatorHandler;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.contents.PassiveBoilerHeatersThermoregulatorHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightThermoregulatorHandlerEvent;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightThermoregulatorHandlers {
    public static void register() {
        AirtightThermoregulatorHandlerEvent.add(AllBlocks.BLAZE_BURNER.get(), new BlazeBurnerThermoregulatorHandler());
        AirtightThermoregulatorHandlerEvent.add(CCBBlocks.BREEZE_COOLER_BLOCK.get(), new BreezeCoolerThermoregulatorHandler());

        AirtightThermoregulatorHandler.REGISTRY.registerProvider(Provider.forBlockTag(AllBlockTags.PASSIVE_BOILER_HEATERS.tag, new PassiveBoilerHeatersThermoregulatorHandler()));
    }
}
