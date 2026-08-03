package net.ty.createcraftedbeginning.api.turbinehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DefaultTurbineHandler implements AirtightTurbineHandler {
    public static final DefaultTurbineHandler INSTANCE = new DefaultTurbineHandler();

    private DefaultTurbineHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxLevel() {
        return 0;
    }
}
