package net.ty.createcraftedbeginning.api.turbinehandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightTurbineHandler {
    int MAX_LEVEL = 16;

    SimpleRegistry<Gas, AirtightTurbineHandler> REGISTRY = SimpleRegistry.create();

    /**
     * Returns the highest operating level this gas can unlock in a Tesla Turbine.
     *
     * @return the maximum turbine level
     */
    int getMaxLevel();
}
