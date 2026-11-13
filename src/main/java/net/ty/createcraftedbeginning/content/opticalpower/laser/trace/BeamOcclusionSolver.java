package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.OcclusionWork;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Reason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Termination;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalInteraction;
import net.ty.createcraftedbeginning.content.opticalpower.laser.PropagationBudget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamFirstHitQuery.Result;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BeamOcclusionSolver {
    private static final double MIN_PROGRESS = 1.0E-4;
    private static final double FLUX_RELATIVE_TOLERANCE = 1.0E-10;

    private BeamOcclusionSolver() {
    }

    public static BeamPropagation solve(Level level, BeamState source, PropagationBudget budget, float partialTick) {
        return solve(level, source, budget, partialTick, false);
    }

    public static BeamPropagation solve(Level level, BeamState source, PropagationBudget budget, float partialTick, boolean rendering) {
        Objects.requireNonNull(level, "Beam occlusion solving requires a non-null level.");
        return solve(level, source, budget, new LaserCollisionContext(partialTick), rendering);
    }

    public static BeamPropagation solve(Level level, BeamState source, PropagationBudget budget, LaserCollisionContext collision, boolean rendering) {
        float partialTick = collision.getPartialTick();
        if (!Float.isFinite(partialTick) || partialTick < 0 || partialTick > 1) {
            throw new IllegalArgumentException("Occlusion partial tick must be finite and in [0, 1]; got " + partialTick + '.');
        }

        return solve(source, budget, new BeamFirstHitQuery(level, collision, rendering, budget));
    }

    static BeamPropagation solve(BeamState source, PropagationBudget budget, Query query) {
        List<Segment> segments = new ArrayList<>();
        List<Termination> terminations = new ArrayList<>();
        List<OpticalHit> visited = new ArrayList<>();
        List<BeamState> states = new ArrayList<>();
        BeamState beam = source;
        OpticalHit previous = null;
        double range = budget.range();
        for (int depth = 0; terminations.isEmpty(); depth++) {
            int parent = segments.size() - 1;
            Reason stopped = null;
            if (beam.fluxLm() < budget.minFluxLm()) {
                stopped = Reason.BELOW_MIN_POWER;
            }
            else if (range <= 0) {
                stopped = Reason.RANGE_EXHAUSTED;
            }
            else if (depth >= budget.maxDepth()) {
                stopped = Reason.DEPTH_LIMIT;
            }
            else if (segments.size() >= budget.maxSegments()) {
                stopped = Reason.SEGMENT_LIMIT;
            }
            else if (depth >= budget.maxBeams()) {
                stopped = Reason.BEAM_LIMIT;
            }
            if (stopped != null) {
                terminations.add(new Termination(parent, beam, stopped));
                continue;
            }

            Result event = query.findNext(beam, range, previous);
            double length = event.distance();
            if (!Double.isFinite(length) || length < 0 || length > range) {
                throw new IllegalStateException("Beam query distance must be finite and within the remaining range; got " + length + " of " + range + " blocks.");
            }

            if (event.reason() == Reason.OCCLUSION_LIMIT) {
                terminations.add(new Termination(parent, beam, Reason.OCCLUSION_LIMIT));
                continue;
            }

            BeamState incident = beam.advance(length);
            BeamFrame incidentFrame = incident.frame();
            OpticalHit hit = event.hit();
            if (hit == null) {
                if (length > 0) {
                    segments.add(new Segment(parent, beam, incident, length, range, null, 0, null, event.boundary()));
                    parent++;
                }
                terminations.add(new Termination(parent, incident, Objects.requireNonNull(event.reason(), "A beam query without a hit must include its stopping reason.")));
                continue;
            }

            if (Math.abs(hit.position().subtract(beam.frame().origin()).dot(beam.frame().direction()) - length) > 1.0E-5) {
                throw new IllegalStateException("Beam query contact must lie at its reported distance.");
            }

            OpticalInteraction interaction = hit.surface().interactor().interact(incident, hit);
            BeamState outgoing = interaction.outgoingBeam();
            double accounted = interaction.absorbedFluxLm();
            if (outgoing != null) {
                accounted += outgoing.fluxLm();
                if (outgoing.frame().origin().distanceToSqr(hit.position()) > 1.0E-10) {
                    throw new IllegalStateException("Outgoing whole beam must start at optical contact " + hit.position() + '.');
                }
            }
            if (!Double.isFinite(accounted) || Math.abs(accounted - beam.fluxLm()) > beam.fluxLm() * FLUX_RELATIVE_TOLERANCE) {
                throw new IllegalStateException("Whole-beam interaction must conserve flux at " + hit.target() + '.');
            }

            BeamContact contact = BeamContact.create(beam, range, hit);
            BeamTerminal terminal = event.terminal();
            if (outgoing == null && terminal == null) {
                terminal = BeamTerminal.EMPTY;
            }
            int index = segments.size();
            segments.add(new Segment(parent, beam, incident, length, range, hit, interaction.absorbedFluxLm(), contact, event.boundary(), terminal));
            if (outgoing == null) {
                BeamState empty = new BeamState(incidentFrame, incident.profile(), 0, incident.footprint());
                terminations.add(new Termination(index, empty, Reason.ABSORBED));
                continue;
            }

            for (int visit = 0; visit < visited.size(); visit++) {
                BeamState prior = states.get(visit);
                BeamFrame priorFrame = prior.frame();
                if (!visited.get(visit).target().equals(hit.target()) || priorFrame.origin().distanceToSqr(incidentFrame.origin()) > 1.0E-10 || priorFrame.direction().distanceToSqr(incidentFrame.direction()) > 1.0E-12 || priorFrame.transverseAxis().distanceToSqr(incidentFrame.transverseAxis()) > 1.0E-12 || !prior.profile().equals(incident.profile()) || !prior.footprint().equals(incident.footprint()) || prior.fluxLm() != incident.fluxLm()) {
                    continue;
                }

                stopped = Reason.REPEATED_STATE;
                break;
            }
            if (stopped == null && length <= MIN_PROGRESS) {
                stopped = Reason.NO_PROGRESS;
            }
            if (stopped != null) {
                terminations.add(new Termination(index, outgoing, stopped));
                continue;
            }

            visited.add(hit);
            states.add(incident);
            previous = hit;
            beam = outgoing;
            range -= length;
        }
        BeamPropagation result = new BeamPropagation(source, segments, terminations, query.work());
        double accounted = result.getAbsorbedFluxLm() + result.getRemainingFluxLm();
        if (!Double.isFinite(accounted) || Math.abs(accounted - source.fluxLm()) > source.fluxLm() * FLUX_RELATIVE_TOLERANCE) {
            throw new IllegalStateException("Whole-beam propagation must conserve flux; got input=" + source.fluxLm() + ", accounted=" + accounted + " lm.");
        }

        return result;
    }

    interface Query {
        Result findNext(BeamState beam, double range, @Nullable OpticalHit previous);

        OcclusionWork work();
    }
}
