package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalInteraction;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile.Span;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Vertex;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BeamPathReplay {
    private BeamPathReplay() {}

    public static @Nullable Evaluation evaluate(BeamState source, List<OpticalHit> prefix, double range, BeamFootprint footprint, double flux) {
        Evaluation endpoint = propagate(source, prefix, range, footprint, flux, null, false);
        if (endpoint == null) {
            return null;
        }

        return new Evaluation(translate(endpoint.state(), source.frame().origin()), endpoint.remainingRange());
    }

    static @Nullable Path replay(BeamState source, List<OpticalHit> prefix, double range, BeamFootprint footprint, double flux, boolean contacts) {
        List<Segment> segments = new ArrayList<>();
        Evaluation endpoint = propagate(source, prefix, range, footprint, flux, segments, contacts);
        if (endpoint == null) {
            return null;
        }

        BeamState state = endpoint.state();
        double remaining = endpoint.remainingRange();
        return new Path(List.copyOf(segments), translate(state, source.frame().origin()), remaining, focus(state, remaining));
    }

    static @Nullable Focus focus(BeamState state, double range) {
        BeamProfile profile = state.profile();
        List<Span> spans = profile.spans(range);
        Span span = spans.getLast();
        double start = span.from();
        BeamProfile tail = profile.advance(start);
        double slope = span.radiusSlope();
        if (slope <= 0) {
            return null;
        }

        double distance = start - tail.radius() / slope;
        BeamFrame frame = state.frame();
        BeamFootprint footprint = state.footprint();
        double radius = profile.radius();
        Vec3 origin = frame.origin().add(frame.direction().scale(distance)).subtract(frame.transverseAxis().scale(radius * footprint.horizontalOffset())).subtract(frame.getSecondTransverseAxis().scale(radius * footprint.verticalOffset()));
        return new Focus(origin, distance, slope, start);
    }

    private static @Nullable Evaluation propagate(BeamState source, List<OpticalHit> prefix, double range, BeamFootprint footprint, double flux, @Nullable List<Segment> segments, boolean contacts) {
        BeamFrame frame = source.frame();
        Vec3 origin = frame.origin();
        BeamProfile profile = source.profile();
        BeamFootprint sourceFootprint = source.footprint();
        double radius = profile.radius();
        Vec3 transverse = frame.transverseAxis();
        Vec3 offset = transverse.scale(radius * (footprint.horizontalOffset() - sourceFootprint.horizontalOffset())).add(frame.getSecondTransverseAxis().scale(radius * (footprint.verticalOffset() - sourceFootprint.verticalOffset())));
        BeamState state = new BeamState(new BeamFrame(offset, frame.direction(), transverse), profile, flux, footprint);
        for (OpticalHit expected : prefix) {
            OpticalSurface expectedSurface = expected.surface();
            if (expectedSurface.aperture() == null) {
                return null;
            }

            BeamFootprint current = state.footprint();
            OpticalTarget expectedTarget = expected.target();
            BlockHitResult expectedLocalHit = expected.localHit();
            Vec3 expectedNormal = expected.localPlaneNormal();
            OpticalHit plane = new OpticalHit(expectedTarget, expectedLocalHit, expected.position().subtract(origin), expectedSurface, expectedNormal);
            Vertex vertex = BeamContact.intersect(state, state.profile().spans(range), plane, current.horizontalOffset(), current.verticalOffset());
            if (vertex == null) {
                return null;
            }

            double length = vertex.distance();
            if (length <= OpticalWorldQuery.TRACE_OFFSET) {
                return null;
            }

            BeamState incident = state.advance(length);
            Vec3 point = incident.frame().origin();
            OpticalHit hit = new OpticalHit(expectedTarget, new BlockHitResult(point, expectedLocalHit.getDirection(), expectedTarget.blockPos(), false), point, expectedSurface, expectedNormal);
            OpticalSurface surface = hit.surface();
            OpticalInteraction interaction = surface.interactor().interact(incident, hit);
            BeamState outgoing = interaction.outgoingBeam();
            if (outgoing == null || interaction.absorbedFluxLm() != 0 || outgoing.fluxLm() != state.fluxLm()) {
                return null;
            }

            if (segments != null) {
                BeamContact contact = null;
                if (contacts) {
                    contact = BeamContact.create(state, range, hit);
                    if (contact == null) {
                        return null;
                    }
                }
                Vec3 position = point.add(origin);
                OpticalTarget target = hit.target();
                OpticalHit worldHit = new OpticalHit(target, new BlockHitResult(position, hit.localHit().getDirection(), target.blockPos(), false), position, surface, hit.localPlaneNormal());
                segments.add(new Segment(segments.size() - 1, translate(state, origin), translate(incident, origin), length, range, worldHit, 0, contact));
            }
            state = outgoing;
            range -= length;
        }
        return new Evaluation(state, range);
    }

    private static BeamState translate(BeamState state, Vec3 offset) {
        BeamFrame frame = state.frame();
        return new BeamState(new BeamFrame(frame.origin().add(offset), frame.direction(), frame.transverseAxis()), state.profile(), state.fluxLm(), state.footprint());
    }

    record Path(List<Segment> segments, BeamState state, double remainingRange, @Nullable Focus focus) {}

    public record Evaluation(BeamState state, double remainingRange) {}

    record Focus(Vec3 origin, double distance, double slope, double start) {}
}
