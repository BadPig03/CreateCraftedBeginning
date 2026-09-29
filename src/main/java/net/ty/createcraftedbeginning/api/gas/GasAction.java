package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GasAction {
    EXECUTE(FluidAction.EXECUTE),
    SIMULATE(FluidAction.SIMULATE);

    private final FluidAction fluidAction;

    GasAction(FluidAction fluidAction) {
        this.fluidAction = fluidAction;
    }

    public static GasAction get(boolean execute) {
        if (!execute) {
            return SIMULATE;
        }

        return EXECUTE;
    }

    @SuppressWarnings("unused")
    public static GasAction fromFluidAction(FluidAction action) {
        if (action != FluidAction.EXECUTE) {
            return SIMULATE;
        }

        return EXECUTE;
    }

    public boolean execute() {
        return this == EXECUTE;
    }

    public boolean simulate() {
        return this == SIMULATE;
    }

    @SuppressWarnings("unused")
    public FluidAction toFluidAction() {
        return fluidAction;
    }
}
