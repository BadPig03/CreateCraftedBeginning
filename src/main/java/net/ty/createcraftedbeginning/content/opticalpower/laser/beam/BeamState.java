package net.ty.createcraftedbeginning.content.opticalpower.laser.beam;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record BeamState(BeamFrame frame, BeamProfile profile, double fluxLm, BeamFootprint footprint) {
    public BeamState(BeamFrame frame, BeamProfile profile, double fluxLm) {
        this(frame, profile, fluxLm, BeamFootprint.FULL);
    }

    public BeamState {
        Objects.requireNonNull(frame, "Beam state frame must not be null.");
        Objects.requireNonNull(profile, "Beam state profile must not be null.");
        if (!Double.isFinite(fluxLm) || fluxLm < 0) {
            throw new IllegalArgumentException("Beam power must be finite and non-negative; got " + fluxLm + " lm.");
        }

        fluxLm = Math.max(0, fluxLm);
        Objects.requireNonNull(footprint, "Beam state footprint must not be null.");
    }

    public BeamState advance(double distance) {
        BeamProfile advanced = profile.advance(distance);
        BeamFrame moved = frame.advance(distance);
        double delta = advanced.radius() - profile.radius();
        Vec3 transverse = frame.transverseAxis();
        Vec3 origin = moved.origin().add(transverse.scale(delta * footprint.horizontalOffset())).add(frame.getSecondTransverseAxis().scale(delta * footprint.verticalOffset()));
        return new BeamState(new BeamFrame(origin, frame.direction(), transverse), advanced, fluxLm, footprint);
    }
}
