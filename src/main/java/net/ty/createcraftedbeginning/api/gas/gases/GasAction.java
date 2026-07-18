package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public enum GasAction {
    EXECUTE(FluidAction.EXECUTE),
    SIMULATE(FluidAction.SIMULATE);

    private final FluidAction fluidAction;

    GasAction(FluidAction fluidAction) {
        this.fluidAction = fluidAction;
    }

    /**
     * Computes and returns the get result.
     *
     * @param execute whether execute is enabled
     * @return this instance
     */
    public static GasAction get(boolean execute) {
        return execute ? EXECUTE : SIMULATE;
    }

    /**
     * Creates a value from the supplied fluid action representation.
     *
     * @param action the action that determines whether the operation is simulated or executed
     * @return the converted value
     */
    public static GasAction fromFluidAction(FluidAction action) {
        return action == FluidAction.EXECUTE ? EXECUTE : SIMULATE;
    }

    /**
     * Executes this operation.
     *
     * @return {@code true} if the condition is satisfied; otherwise {@code false}
     */
    public boolean execute() {
        return this == EXECUTE;
    }

    /**
     * Simulates the configured operation without applying changes.
     *
     * @return {@code true} if the condition is satisfied; otherwise {@code false}
     */
    public boolean simulate() {
        return this == SIMULATE;
    }

    /**
     * Converts this value to a fluid action representation.
     *
     * @return the converted value
     */
    public FluidAction toFluidAction() {
        return fluidAction;
    }
}
