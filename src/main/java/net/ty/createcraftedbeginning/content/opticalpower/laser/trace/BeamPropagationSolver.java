package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Reason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Termination;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalInteraction;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.PropagationBudget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPathReplay.Path;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BeamPropagationSolver {
    private static final double MIN_REMAINING_RANGE = 2.0E-4;
    private static final double POSITION_TOLERANCE_SQR = 1.0E-10;
    private static final double DIRECTION_TOLERANCE_SQR = 1.0E-12;
    private static final double POWER_RELATIVE_TOLERANCE = 1.0E-12;

    private BeamPropagationSolver() {
    }

    static BeamPropagation solve(BeamState source, PropagationBudget budget, Query query, List<BeamState> regions) {
        BeamQuery beamQuery = (beam, range) -> query.findNext(beam.frame(), range);
        Trace trace = new Trace(source, budget, regions);
        while (!trace.isComplete()) {
            trace.advance(beamQuery);
        }
        return trace.result();
    }

    @FunctionalInterface
    interface Query {
        @Nullable OpticalQueryResult findNext(BeamFrame frame, double range);
    }

    @FunctionalInterface
    interface BeamQuery {
        @Nullable OpticalQueryResult findNext(BeamState beam, double range);
    }

    static final class Trace {
        private final BeamState source;
        private final PropagationBudget budget;
        private final ArrayDeque<PendingBeam> pending = new ArrayDeque<>();
        private final List<Segment> segments = new ArrayList<>();
        private final List<Termination> terminations = new ArrayList<>();
        private final int maxBeams;
        private int admittedBeams;

        Trace(BeamState source, PropagationBudget budget, List<BeamState> regions) {
            Objects.requireNonNull(source, "Beam trace source must not be null.");
            Objects.requireNonNull(budget, "Beam trace propagation budget must not be null.");
            this.source = source;
            this.budget = budget;
            double initialPower = 0;
            for (BeamState region : regions) {
                initialPower += region.fluxLm();
                pending.add(new PendingBeam(region, budget.range(), -1, 0, null));
            }
            double sourceFluxLm = source.fluxLm();
            validatePower(sourceFluxLm, initialPower, "Initial beam regions");
            maxBeams = budget.maxBeams();
            if (regions.size() > maxBeams) {
                throw new IllegalArgumentException("Initial beam regions exceed beam limit " + maxBeams + "; got " + regions.size() + '.');
            }

            admittedBeams = regions.size();
        }

        Trace(BeamState source, PropagationBudget budget, Path prefix) {
            this.source = source;
            this.budget = budget;
            maxBeams = budget.maxBeams();
            segments.addAll(prefix.segments());
            Visit history = null;
            for (Segment segment : segments) {
                OpticalHit hit = segment.hit();
                if (hit == null) {
                    continue;
                }

                history = new Visit(hit.target(), segment.incident(), history);
            }
            int depth = (int) segments.stream().filter(segment -> segment.hit() != null).count();
            admittedBeams = depth + 1;
            pending.add(new PendingBeam(prefix.state(), prefix.remainingRange(), segments.size() - 1, depth, history));
        }

        private static void validatePower(double incoming, double accounted, String context) {
            double tolerance = incoming * POWER_RELATIVE_TOLERANCE;
            if (!Double.isFinite(accounted) || Math.abs(incoming - accounted) > tolerance) {
                throw new IllegalStateException(context + " must conserve power; got input=" + incoming + ", accounted=" + accounted + " lm.");
            }
        }

        private static void validateInteraction(BeamState incident, OpticalHit hit, OpticalInteraction interaction) {
            double accountedPower = interaction.absorbedFluxLm();
            BeamState output = interaction.outgoingBeam();
            if (output != null) {
                Vec3 origin = output.frame().origin();
                Vec3 hitPosition = hit.position();
                if (origin.distanceToSqr(hitPosition) > POSITION_TOLERANCE_SQR) {
                    throw new IllegalStateException("Outgoing beam must start at contact " + hitPosition + "; got " + origin + '.');
                }

                accountedPower += output.fluxLm();
            }
            double incomingPower = incident.fluxLm();
            double tolerance = incomingPower * POWER_RELATIVE_TOLERANCE;
            if (!Double.isFinite(accountedPower) || Math.abs(incomingPower - accountedPower) > tolerance) {
                throw new IllegalStateException("Optical interaction at " + hit.target() + " must conserve power; got input=" + incomingPower + ", accounted=" + accountedPower + " lm.");
            }
        }

        boolean isComplete() {
            return pending.isEmpty();
        }

        List<Segment> segments() {
            return segments;
        }

        List<Termination> terminations() {
            return terminations;
        }

        BeamPropagation result() {
            if (!isComplete()) {
                throw new IllegalStateException("Cannot publish an incomplete optical trace.");
            }

            BeamPropagation result = new BeamPropagation(source, segments, terminations);
            validatePower(source.fluxLm(), result.getAbsorbedFluxLm() + result.getRemainingFluxLm(), "Propagation");
            return result;
        }

        Path checkpoint() {
            PendingBeam beam = pending.getFirst();
            return new Path(List.copyOf(segments), beam.state(), beam.range(), null);
        }

        void advance(BeamQuery query) {
            advance(query, Double.POSITIVE_INFINITY);
        }

        void advance(BeamQuery query, double stopAfter) {
            PendingBeam current = pending.removeFirst();
            BeamState beam = current.state();
            double range = current.range();
            int depth = current.depth();
            int parentIndex = current.parentIndex();
            Reason stopped = null;
            if (beam.fluxLm() < budget.minFluxLm()) {
                stopped = Reason.BELOW_MIN_POWER;
            }
            else if (range <= MIN_REMAINING_RANGE) {
                stopped = Reason.RANGE_EXHAUSTED;
            }
            else if (depth >= budget.maxDepth()) {
                stopped = Reason.DEPTH_LIMIT;
            }
            else if (segments.size() >= budget.maxSegments()) {
                stopped = Reason.SEGMENT_LIMIT;
            }
            if (stopped != null) {
                terminations.add(new Termination(parentIndex, beam, stopped));
                return;
            }

            double queryRange = Math.min(range, stopAfter);
            OpticalQueryResult queryResult = query.findNext(beam, queryRange);
            if (queryResult == null) {
                terminations.add(new Termination(parentIndex, beam, Reason.UNLOADED));
                return;
            }

            double length = queryResult.length();
            if (length > queryRange) {
                throw new IllegalStateException("Optical query exceeded remaining range " + queryRange + "; got " + length + " blocks.");
            }

            BeamState incident = beam.advance(length);
            BeamFrame incidentFrame = incident.frame();
            OpticalHit hit = queryResult.hit();
            Vec3 incidentOrigin = incidentFrame.origin();
            OpticalInteraction interaction = new OpticalInteraction(null, 0);
            if (hit != null) {
                Vec3 hitPosition = hit.position();
                if (incidentOrigin.distanceToSqr(hitPosition) > POSITION_TOLERANCE_SQR) {
                    throw new IllegalStateException("Optical contact must lie at the propagated endpoint; got " + hitPosition + " for " + incidentOrigin + '.');
                }

                interaction = hit.surface().interactor().interact(incident, hit);
                validateInteraction(incident, hit, interaction);
            }

            int segmentIndex = segments.size();
            segments.add(new Segment(parentIndex, beam, incident, length, range, hit, interaction.absorbedFluxLm()));
            if (hit == null) {
                if (length == queryRange && queryRange < range) {
                    pending.addFirst(new PendingBeam(incident, range - length, segmentIndex, depth, current.history()));
                    return;
                }

                Reason reason = Reason.RANGE_EXHAUSTED;
                if (length < range) {
                    reason = Reason.UNLOADED;
                }
                terminations.add(new Termination(segmentIndex, incident, reason));
                return;
            }

            BeamState outgoing = interaction.outgoingBeam();
            if (outgoing == null) {
                terminations.add(new Termination(segmentIndex, new BeamState(incidentFrame, incident.profile(), 0, incident.footprint()), Reason.ABSORBED));
                return;
            }

            Reason interrupted = null;
            OpticalTarget target = hit.target();
            BeamProfile profile = incident.profile();
            BeamFootprint footprint = incident.footprint();
            double fluxLm = incident.fluxLm();
            Visit previousHistory = current.history();
            for (Visit previous = previousHistory; previous != null; previous = previous.parent()) {
                BeamState prior = previous.incident();
                BeamFrame priorFrame = prior.frame();
                if (!previous.target().equals(target) || priorFrame.origin().distanceToSqr(incidentOrigin) > POSITION_TOLERANCE_SQR || priorFrame.direction().distanceToSqr(incidentFrame.direction()) > DIRECTION_TOLERANCE_SQR || priorFrame.transverseAxis().distanceToSqr(incidentFrame.transverseAxis()) > DIRECTION_TOLERANCE_SQR || !prior.profile().equals(profile) || !prior.footprint().equals(footprint) || prior.fluxLm() != fluxLm) {
                    continue;
                }

                interrupted = Reason.REPEATED_STATE;
                break;
            }
            if (interrupted == null && length <= OpticalWorldQuery.TRACE_OFFSET) {
                interrupted = Reason.NO_PROGRESS;
            }

            Visit history = new Visit(target, incident, previousHistory);
            if (interrupted == null && admittedBeams >= maxBeams) {
                interrupted = Reason.BEAM_LIMIT;
            }
            if (interrupted != null) {
                terminations.add(new Termination(segmentIndex, outgoing, interrupted));
                return;
            }

            pending.addLast(new PendingBeam(outgoing, range - length, segmentIndex, depth + 1, history));
            admittedBeams++;
        }
    }

    private record PendingBeam(BeamState state, double range, int parentIndex, int depth, @Nullable Visit history) {}

    private record Visit(OpticalTarget target, BeamState incident, @Nullable Visit parent) {}
}
