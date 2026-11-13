package net.ty.createcraftedbeginning.content.opticalpower.laser;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record BeamPropagation(BeamState source, List<Segment> segments, List<Termination> terminations, OcclusionWork occlusionWork) {
    public BeamPropagation(BeamState source, List<Segment> segments, List<Termination> terminations) {
        this(source, segments, terminations, new OcclusionWork(0, 0, 0));
    }

    public BeamPropagation {
        Objects.requireNonNull(source, "Beam propagation source must not be null.");
        Objects.requireNonNull(occlusionWork, "Beam propagation must include occlusion work statistics.");
        segments = List.copyOf(segments);
        terminations = List.copyOf(terminations);
    }

    public double getAbsorbedFluxLm() {
        double power = 0;
        for (Segment segment : segments) {
            power += segment.absorbedFluxLm();
        }
        return power;
    }

    public double getRemainingFluxLm() {
        double power = 0;
        for (Termination termination : terminations) {
            power += termination.state().fluxLm();
        }
        return power;
    }

    public record Segment(int parentIndex, BeamState start, BeamState incident, double length, double remainingRange, @Nullable OpticalHit hit, double absorbedFluxLm, @Nullable BeamContact contact, @Nullable Boundary boundary, @Nullable BeamTerminal terminal) {
        public Segment(int parentIndex, BeamState start, BeamState incident, double length, double remainingRange, @Nullable OpticalHit hit, double absorbedFluxLm) {
            this(parentIndex, start, incident, length, remainingRange, hit, absorbedFluxLm, null, null);
        }

        public Segment(int parentIndex, BeamState start, BeamState incident, double length, double remainingRange, @Nullable OpticalHit hit, double absorbedFluxLm, @Nullable BeamContact contact) {
            this(parentIndex, start, incident, length, remainingRange, hit, absorbedFluxLm, contact, null);
        }

        public Segment(int parentIndex, BeamState start, BeamState incident, double length, double remainingRange, @Nullable OpticalHit hit, double absorbedFluxLm, @Nullable BeamContact contact, @Nullable Boundary boundary) {
            this(parentIndex, start, incident, length, remainingRange, hit, absorbedFluxLm, contact, boundary, null);
        }

        public Segment {
            if (!Double.isFinite(absorbedFluxLm) || absorbedFluxLm < 0) {
                throw new IllegalArgumentException("Absorbed segment power must be finite and non-negative; got " + absorbedFluxLm + " lm.");
            }

            absorbedFluxLm = Math.max(0, absorbedFluxLm);
        }
    }

    public record Termination(int parentIndex, BeamState state, Reason reason) {
    }

    public record Boundary(double firstContactDistance, Vec3 firstContactPoint, OpticalPlane cutoffPlane, boolean opticalInteraction) {}

    public record OcclusionWork(int beamQueries, int candidateBlocks, int shapeTests) {}

    public enum Reason {
        ABSORBED,
        RANGE_EXHAUSTED,
        UNLOADED,
        BELOW_MIN_POWER,
        REPEATED_STATE,
        NO_PROGRESS,
        DEPTH_LIMIT,
        SEGMENT_LIMIT,
        BEAM_LIMIT,
        OCCLUSION_LIMIT
    }
}
