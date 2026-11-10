package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile.Span;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Patch;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Vertex;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalAperture;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPathReplay.Focus;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPathReplay.Path;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record BeamLinearTail(Path path, double tailStart, int verticalSign) {
    private static final double EPSILON = 1.0E-9;

    static @Nullable BeamLinearTail create(BeamState source, List<Segment> prefix, double range, BeamVolumeQuery volume) {
        List<OpticalHit> hits = new ArrayList<>();
        for (Segment segment : prefix) {
            OpticalHit hit = segment.hit();
            if (hit == null || segment.absorbedFluxLm() != 0) {
                return null;
            }

            hits.add(hit);
        }
        BeamFootprint footprint = source.footprint();
        double flux = source.fluxLm();
        Path center = BeamPathReplay.replay(source, hits, range, footprint, flux, false);
        if (center == null) {
            return null;
        }

        int verticalSign = 1;
        List<Segment> centerSegments = center.segments();
        for (int index = 0; index < prefix.size(); index++) {
            Segment segment = centerSegments.get(index);
            OpticalHit hit = segment.hit();
            if (hit == null) {
                return null;
            }

            OpticalTarget target = hit.target();
            if (target.inSubLevel() || target.contraptionId() != null) {
                return null;
            }

            OpticalSurface surface = hit.surface();
            OpticalAperture aperture = surface.aperture();
            BeamState start = segment.start();
            double remainingRange = segment.remainingRange();
            BeamContact contact = BeamContact.create(start, remainingRange, hit);
            if (aperture == null || contact == null) {
                return null;
            }

            double halfSize = aperture.halfSize();
            for (Patch patch : contact.patches()) {
                for (Vec3 offset : patch.vertices()) {
                    Vec3 point = hit.position().add(offset).subtract(aperture.center());
                    if (Math.abs(point.dot(aperture.horizontal())) > halfSize + EPSILON || Math.abs(point.dot(aperture.vertical())) > halfSize + EPSILON) {
                        return null;
                    }
                }
            }
            BeamState outgoing = center.state();
            if (index + 1 < prefix.size()) {
                outgoing = centerSegments.get(index + 1).start();
            }
            Vec3 normal = surface.normal();
            Vec3 direction = start.frame().direction();
            double incidence = direction.dot(normal);
            BeamFrame outgoingFrame = outgoing.frame();
            Vec3 outgoingDirection = outgoingFrame.direction();
            if (incidence * outgoingDirection.dot(normal) < 0) {
                Vec3 reflected = direction.subtract(normal.scale(2 * incidence));
                if (reflected.distanceToSqr(outgoingDirection) > EPSILON * EPSILON || !segment.incident().profile().equals(outgoing.profile())) {
                    return null;
                }

                verticalSign = -verticalSign;
            }
            else if (Math.abs(incidence) < 1 - EPSILON) {
                return null;
            }
            BeamProfile startProfile = start.profile();
            if (Math.abs(incidence) >= 1 - EPSILON || startProfile.widthSlope() == 0) {
                continue;
            }

            Focus incoming = BeamPathReplay.focus(start, remainingRange);
            if (incoming == null) {
                return null;
            }

            List<Span> spans = startProfile.spans(remainingRange);
            for (Point point : start.footprint().domain().vertices()) {
                Vertex vertex = BeamContact.intersect(start, spans, hit, point.u(), point.v());
                if (vertex == null || vertex.distance() < incoming.start() - EPSILON) {
                    return null;
                }
            }
        }
        Focus expected = center.focus();
        if (expected == null) {
            return null;
        }

        List<Path> paths = new ArrayList<>();
        List<Focus> foci = new ArrayList<>();
        paths.add(center);
        foci.add(expected);
        double expectedDistance = expected.distance();
        double startDistance = expected.start() - expectedDistance;
        double horizontalScale = footprint.horizontalScale();
        double verticalScale = footprint.verticalScale();
        for (int corner = 0; corner < 4; corner++) {
            double u = footprint.horizontalOffset() + ((corner & 1) == 0 ? -1 : 1) * horizontalScale;
            double v = footprint.verticalOffset() + ((corner & 2) == 0 ? -1 : 1) * verticalScale;
            BeamFootprint sample = new BeamFootprint(horizontalScale, verticalScale, u, v);
            Path path = BeamPathReplay.replay(source, hits, range, sample, flux, false);
            if (path == null) {
                return null;
            }

            Focus actual = path.focus();
            if (actual == null) {
                return null;
            }

            double actualDistance = actual.distance();
            if (actual.origin().distanceToSqr(expected.origin()) > EPSILON * EPSILON || Math.abs(actual.slope() - expected.slope()) > EPSILON || Math.abs(path.remainingRange() - actualDistance - (center.remainingRange() - expectedDistance)) > EPSILON) {
                return null;
            }

            paths.add(path);
            foci.add(actual);
            startDistance = Math.max(startDistance, actual.start() - actualDistance);
        }
        List<BeamPropagation> checked = new ArrayList<>();
        for (int index = 0; index < paths.size(); index++) {
            Path path = paths.get(index);
            List<Segment> segments = new ArrayList<>(path.segments());
            double length = Math.max(0, startDistance + foci.get(index).distance());
            double remainingRange = path.remainingRange();
            if (length > remainingRange) {
                return null;
            }

            BeamState state = path.state();
            segments.add(new Segment(segments.size() - 1, state, state.advance(length), length, remainingRange, null, 0));
            checked.add(new BeamPropagation(segments.getFirst().start(), segments, List.of()));
        }
        if (volume.hasUnseenPrefixObstacle(checked.getFirst(), checked.subList(1, checked.size()))) {
            return null;
        }

        return new BeamLinearTail(center, Math.max(0, startDistance + expectedDistance), verticalSign);
    }

}
