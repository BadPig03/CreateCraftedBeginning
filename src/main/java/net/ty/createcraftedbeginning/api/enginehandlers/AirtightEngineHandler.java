package net.ty.createcraftedbeginning.api.enginehandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface AirtightEngineHandler {
    int MAX_LEVEL = 8;

    SimpleRegistry<Gas, AirtightEngineHandler> REGISTRY = SimpleRegistry.create();

    /**
     * Returns the amount of effective supply contributed by each unit of gas.
     *
     * @return the gas work factor
     */
    double getWorkFactor();

    /**
     * Returns the highest airtight engine level this gas can sustain.
     *
     * @return the maximum engine level
     */
    default int getMaxLevel() {
        return 8;
    }
}
