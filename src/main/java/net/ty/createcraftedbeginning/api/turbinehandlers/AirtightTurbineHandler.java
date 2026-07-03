package net.ty.createcraftedbeginning.api.turbinehandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightTurbineHandler {
    SimpleRegistry<Gas, AirtightTurbineHandler> REGISTRY = SimpleRegistry.create();

    int getEfficiency();
}
