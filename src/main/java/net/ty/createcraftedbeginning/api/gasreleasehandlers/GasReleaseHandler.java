package net.ty.createcraftedbeginning.api.gasreleasehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasReleaseHandler {
    /** Returns the positive gas amount required for one release-effect attempt for this stack. */
    default long getEffectInterval(GasStack gas) {
        return 1000;
    }

    /**
     * Returns a positive minimum interval in ticks between continuous release-effect attempts.
     * Values above one opt into a persistent outlet cooldown shared across gas and pressure changes.
     * Rejected attempts spend their gas threshold without queuing a later attempt.
     * A value of one uses only the existing once-per-tick limit.
     */
    default int getEffectCooldown(GasStack gas) {
        return 1;
    }

    default boolean shouldShowOutline() {
        return true;
    }

    default void apply(GasReleaseContext context) {
    }
}
