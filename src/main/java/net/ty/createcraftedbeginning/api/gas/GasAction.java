package net.ty.createcraftedbeginning.api.gas;

import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

@SuppressWarnings("unused")
public enum GasAction {
    EXECUTE(FluidAction.EXECUTE),
    SIMULATE(FluidAction.SIMULATE);

    private final FluidAction fluidAction;

    GasAction(FluidAction fluidAction) {
        this.fluidAction = fluidAction;
    }

    public static GasAction get(boolean execute) {
        return execute ? EXECUTE : SIMULATE;
    }

    public static GasAction fromFluidAction(FluidAction action) {
        return action == FluidAction.EXECUTE ? EXECUTE : SIMULATE;
    }

    public boolean execute() {
        return this == EXECUTE;
    }

    public boolean simulate() {
        return this == SIMULATE;
    }

    public FluidAction toFluidAction() {
        return fluidAction;
    }
}
