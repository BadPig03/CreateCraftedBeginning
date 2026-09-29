package net.ty.createcraftedbeginning.gas.release;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasReleaseResult(GasReleaseContext context, boolean feedbackDue, boolean effectDue) {
    @Internal
    public boolean released() {
        return context.gasAmount() > 0;
    }
}
