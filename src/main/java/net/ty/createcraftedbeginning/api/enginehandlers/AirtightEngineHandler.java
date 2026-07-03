package net.ty.createcraftedbeginning.api.enginehandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface AirtightEngineHandler {
    SimpleRegistry<Gas, AirtightEngineHandler> REGISTRY = SimpleRegistry.create();

    int getEfficiency();
}
