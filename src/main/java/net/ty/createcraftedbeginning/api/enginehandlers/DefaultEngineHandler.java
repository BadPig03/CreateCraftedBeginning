package net.ty.createcraftedbeginning.api.enginehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DefaultEngineHandler implements AirtightEngineHandler {
    public static final DefaultEngineHandler INSTANCE = new DefaultEngineHandler();

    private DefaultEngineHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getWorkFactor() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxLevel() {
        return 0;
    }
}
