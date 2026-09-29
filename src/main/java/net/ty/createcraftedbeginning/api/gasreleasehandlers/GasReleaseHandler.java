package net.ty.createcraftedbeginning.api.gasreleasehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasReleaseHandler {
    default boolean shouldShowOutline() {
        return true;
    }

    default void apply(GasReleaseContext context) {
    }
}
