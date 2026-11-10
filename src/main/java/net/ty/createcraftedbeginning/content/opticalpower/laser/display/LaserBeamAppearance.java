package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LaserBeamAppearance(double red, double green, double blue, double opacity) {
    private static final double REFERENCE_FLUX_LM = 1;

    public LaserBeamAppearance {
        if (!Double.isFinite(red) || red < 0 || red > 1 || !Double.isFinite(green) || green < 0 || green > 1 || !Double.isFinite(blue) || blue < 0 || blue > 1 || !Double.isFinite(opacity) || opacity < 0 || opacity > 1) {
            throw new IllegalArgumentException("Beam appearance channels must be finite and in [0, 1]; got red=" + red + ", green=" + green + ", blue=" + blue + ", opacity=" + opacity + '.');
        }
    }

    public static LaserBeamAppearance fromBeam(BeamState beam) {
        double fluxLm = beam.fluxLm();
        if (fluxLm == 0) {
            return new LaserBeamAppearance(0, 0, 0, 0);
        }

        double brightness = Math.min(1, Math.sqrt(fluxLm / (REFERENCE_FLUX_LM * beam.footprint().areaFraction())));
        return new LaserBeamAppearance(1, 1, 1, brightness);
    }
}
