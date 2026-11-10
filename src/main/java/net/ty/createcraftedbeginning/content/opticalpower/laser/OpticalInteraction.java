package net.ty.createcraftedbeginning.content.opticalpower.laser;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record OpticalInteraction(@Nullable BeamState outgoingBeam, double absorbedFluxLm) {
    public OpticalInteraction {
        if (!Double.isFinite(absorbedFluxLm) || absorbedFluxLm < 0) {
            throw new IllegalArgumentException("Absorbed optical power must be finite and non-negative; got " + absorbedFluxLm + " lm.");
        }

        absorbedFluxLm = Math.max(0, absorbedFluxLm);
    }
}
