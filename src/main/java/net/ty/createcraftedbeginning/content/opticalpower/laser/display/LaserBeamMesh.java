package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Reason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Termination;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayProfile.Contact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayProfile.Terminal;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderSnapshot;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamMesh {
    private static final double DIRECTION_TOLERANCE_SQR = 1.0E-12;
    private static final double GRAZING_COSINE = 0.05;

    private LaserBeamMesh() {
    }

    public static @Unmodifiable List<DrawSection> create(BeamPropagation propagation) {
        return create(propagation, 1);
    }

    public static @Unmodifiable List<DrawSection> create(BeamPropagation propagation, double layerScale) {
        if (!Double.isFinite(layerScale) || layerScale <= 0 || layerScale> 1) {
            throw new IllegalArgumentException("Beam layer scale must be finite and in (0, 1]; got " + layerScale + '.');
        }

        List<Segment> segments = propagation.segments();
        if (segments.stream().map(Segment::start).anyMatch(start -> start.profile().widthSlope() != 0 || start.footprint().region() != null)) {
            return createVarying(propagation, layerScale);
        }

        int count = segments.size();
        int[] successors = new int[count];
        Arrays.fill(successors, -1);
        double[] distances = new double[count];
        boolean[] absorbed = new boolean[count];
        for (int index = 0; index < count; index++) {
            Segment segment = segments.get(index);
            int parent = segment.parentIndex();
            if (parent < 0) {
                continue;
            }

            if (parent >= index) {
                throw new IllegalArgumentException("Render segment parent must precede segment " + index + "; got " + parent + '.');
            }

            if (successors[parent] >= 0) {
                throw new IllegalArgumentException("Render segment must have at most one successor; got another child for " + parent + '.');
            }

            successors[parent] = index;
            distances[index] = distances[parent] + segments.get(parent).length();
        }
        for (Termination termination : propagation.terminations()) {
            int parent = termination.parentIndex();
            if (parent < 0) {
                continue;
            }

            Reason reason = termination.reason();
            if (reason != Reason.ABSORBED) {
                continue;
            }

            absorbed[parent] = true;
        }
        boolean[] visited = new boolean[count];
        Section[] resolved = new Section[count];
        List<DrawSection> result = new ArrayList<>();
        for (int root = 0; root < count; root++) {
            if (visited[root]) {
                continue;
            }

            List<Segment> path = new ArrayList<>();
            int index = root;
            while (true) {
                visited[index] = true;
                Segment segment = segments.get(index);
                BeamState start = segment.start();
                BeamFrame incomingFrame = start.frame();
                Vec3 incoming = incomingFrame.direction();
                OpticalHit terminalHit = segment.hit();
                path.add(segment);
                int child = successors[index];
                if (child < 0 || terminalHit == null) {
                    break;
                }

                Segment output = segments.get(child);
                BeamState outputStart = output.start();
                Vec3 normal = terminalHit.surface().normal();
                BeamFrame outgoingFrame = outputStart.frame();
                Vec3 reflected = incoming.subtract(normal.scale(2 * incoming.dot(normal)));
                Vec3 transverse = incomingFrame.transverseAxis();
                Vec3 reflectedAxis = transverse.subtract(normal.scale(2 * transverse.dot(normal)));
                if (reflected.distanceToSqr(outgoingFrame.direction()) > DIRECTION_TOLERANCE_SQR || reflectedAxis.distanceToSqr(outgoingFrame.transverseAxis()) > DIRECTION_TOLERANCE_SQR || !segment.incident().profile().equals(outputStart.profile()) || !start.footprint().equals(outputStart.footprint())) {
                    break;
                }

                index = child;
            }

            Segment first = segments.get(root);
            BeamState firstStart = first.start();
            BeamFrame frame = firstStart.frame();
            double width = firstStart.profile().radius();
            int parentIndex = first.parentIndex();
            Vec3 axis = frame.transverseAxis();
            Vec3 second = frame.getSecondTransverseAxis();
            BeamFootprint footprint = firstStart.footprint();
            Vec3 centerOffset = Vec3.ZERO;
            if (parentIndex < 0) {
                centerOffset = frame.origin().subtract(propagation.source().frame().origin());
            }
            double centerX = centerOffset.dot(axis);
            double centerY = centerOffset.dot(second);
            double horizontalScale = footprint.horizontalScale();
            double left = Math.max(centerX - width * horizontalScale, -width * layerScale);
            double right = Math.min(centerX + width * horizontalScale, width * layerScale);
            double verticalScale = footprint.verticalScale();
            double bottom = Math.max(centerY - width * verticalScale, -width * layerScale);
            double top = Math.min(centerY + width * verticalScale, width * layerScale);
            if (right <= left || top <= bottom) {
                continue;
            }

            SamplingDomain domain = new SamplingDomain((left / width + 1) * 0.5, (bottom / width + 1) * 0.5, (right / width + 1) * 0.5, (top / width + 1) * 0.5);
            Vec3 lowerLeft = axis.scale(left - centerX).add(second.scale(bottom - centerY));
            Vec3 lowerRight = axis.scale(right - centerX).add(second.scale(bottom - centerY));
            Vec3 upperRight = axis.scale(right - centerX).add(second.scale(top - centerY));
            Vec3 upperLeft = axis.scale(left - centerX).add(second.scale(top - centerY));
            Vec3[] offsets = {lowerLeft, lowerRight, upperRight, upperLeft};
            List<Section> geometry = LaserBeamGeometry.createSections(path, offsets, parentIndex < 0, 2 * width, part -> {
                OpticalHit hit = part.hit();
                if (hit == null) {
                    return null;
                }

                OpticalSurface surface = hit.surface();
                return new OpticalPlane(hit.position(), surface.normal());
            });
            int segmentIndex = root;
            for (int partIndex = 0; partIndex < path.size(); partIndex++, segmentIndex = successors[segmentIndex]) {
                Segment segment = segments.get(segmentIndex);
                Section section = geometry.get(partIndex);
                List<Vec3> starts = section.starts();
                if (partIndex == 0 && parentIndex >= 0) {
                    Section previous = resolved[parentIndex];
                    if (previous != null) {
                        starts = previous.ends();
                    }
                }
                BeamState start = segment.start();
                BeamFrame beamFrame = start.frame();
                Vec3 beamDirection = beamFrame.direction();
                double remainingRange = segment.remainingRange();
                LaserBeamFade fade = new LaserBeamFade(beamFrame.origin(), beamDirection, remainingRange, Math.min(LaserBeamGeometry.TERMINAL_FADE_LENGTH, distances[segmentIndex] + remainingRange));
                List<Vec3> ends = section.ends();
                section = new Section(starts, ends, ends.stream().anyMatch(point -> fade.at(point) < 1));
                resolved[segmentIndex] = section;
                OpticalHit hit = segment.hit();
                boolean grazingTerminal = absorbed[segmentIndex] && successors[segmentIndex] < 0 && hit != null && Math.abs(beamDirection.dot(hit.surface().normal())) < GRAZING_COSINE;
                result.add(new DrawSection(section, LaserBeamAppearance.fromBeam(start), segmentIndex, distances[segmentIndex], fade, domain, grazingTerminal, LaserBeamBrightness.fromBeam(start)));
            }
        }
        return List.copyOf(result);
    }

    public static @Unmodifiable List<DrawSection> create(LaserRenderSnapshot snapshot) {
        return create(snapshot, 1);
    }

    public static @Unmodifiable List<DrawSection> create(LaserRenderSnapshot snapshot, double layerScale) {
        return create(snapshot.propagation(), layerScale).stream().map(section -> {
            Section geometry = section.geometry();
            List<Vec3> starts = geometry.starts().stream().map(snapshot::toLocalPosition).toList();
            List<Vec3> ends = geometry.ends().stream().map(snapshot::toLocalPosition).toList();
            LaserBeamFade fade = section.fade();
            if (fade != null) {
                fade = new LaserBeamFade(snapshot.toLocalPosition(fade.origin()), snapshot.toLocalDirection(fade.direction()), fade.remainingRange(), fade.length());
            }
            LaserBeamBrightness brightness = section.brightness();
            if (brightness != null) {
                BeamProfile profile = brightness.profile();
                if (profile.widthSlope() != 0) {
                    brightness = new LaserBeamBrightness(snapshot.toLocalPosition(brightness.origin()), snapshot.toLocalDirection(brightness.direction()), profile);
                }
            }
            return new DrawSection(new Section(starts, ends, geometry.fadesAtEnd(), geometry.variableWidth()), section.appearance(), section.segmentIndex(), section.pathDistance(), fade, section.samplingDomain(), section.grazingTerminal(), brightness);
        }).toList();
    }

    static List<DrawSection> split(DrawSection draw) {
        Section geometry = draw.geometry();
        if (geometry.variableWidth()) {
            return List.of(draw);
        }

        boolean grazingTerminal = draw.grazingTerminal();
        List<Section> parts = geometry.createParts(grazingTerminal);
        double referenceWidth = draw.getReferenceWidth();
        boolean hasValidPart = false;
        boolean hasInvalidPart = false;
        for (Section part : parts) {
            if (part.isRenderable(referenceWidth)) {
                hasValidPart = true;
                continue;
            }

            hasInvalidPart = true;
        }
        if (hasInvalidPart && (!hasValidPart || geometry.isRenderable(referenceWidth))) {
            return List.of(draw);
        }

        List<DrawSection> result = new ArrayList<>();
        List<Vec3> starts = geometry.starts();
        Vec3 start = starts.getFirst().lerp(starts.get(2), 0.5);
        List<Vec3> ends = geometry.ends();
        Vec3 end = ends.getFirst().lerp(ends.get(2), 0.5);
        if (hasInvalidPart) {
            Vec3 direction = end.subtract(start).normalize();
            List<Section> aligned = new ArrayList<>();
            List<Vec3> boundary = starts;
            for (int index = 0; index < parts.size(); index++) {
                Section part = parts.get(index);
                List<Vec3> next = part.ends();
                if (index + 1 < parts.size()) {
                    Vec3 center = next.getFirst().lerp(next.get(2), 0.5);
                    List<Vec3> projected = new ArrayList<>();
                    for (Vec3 point : next) {
                        projected.add(point.subtract(direction.scale(point.subtract(center).dot(direction))));
                    }
                    next = List.copyOf(projected);
                }
                aligned.add(new Section(boundary, next, part.fadesAtEnd()));
                boundary = next;
            }
            Section terminal = aligned.getLast();
            if (grazingTerminal && !terminal.isRenderable(referenceWidth)) {
                List<Vec3> flatEnd = new ArrayList<>();
                for (Vec3 point : terminal.ends()) {
                    flatEnd.add(point.subtract(direction.scale(point.subtract(end).dot(direction))));
                }
                Section simplified = new Section(terminal.starts(), flatEnd, terminal.fadesAtEnd());
                if (simplified.isRenderable(referenceWidth)) {
                    aligned.set(aligned.size() - 1, simplified);
                }
            }
            parts = List.copyOf(aligned);
        }
        for (Section part : parts) {
            List<Vec3> partStarts = part.starts();
            Vec3 partStart = partStarts.getFirst().lerp(partStarts.get(2), 0.5);
            List<Vec3> partEnds = part.ends();
            LaserBeamFade fade = draw.fade();
            boolean fades = fade != null && partEnds.stream().anyMatch(point -> fade.at(point) < 1);
            Section section = new Section(partStarts, partEnds, fades);
            result.add(new DrawSection(section, draw.appearance(), draw.segmentIndex(), draw.pathDistance() + partStart.distanceTo(start), fade, draw.samplingDomain(), false, draw.brightness(), draw.mapped()));
        }
        return List.copyOf(result);
    }

    private static @Unmodifiable List<Vec3> varyingSection(LaserBeamDisplayProfile display, double distance, double[] horizontal, double[] vertical, List<Contact> contacts) {
        List<Vec3> vertices = new ArrayList<>(4);
        for (int corner = 0; corner < horizontal.length; corner++) {
            if (!contacts.isEmpty()) {
                Contact contact = contacts.get(corner);
                if (distance >= contact.distance()) {
                    vertices.add(contact.position());
                    continue;
                }
            }

            vertices.add(display.point(distance, horizontal[corner], vertical[corner]));
        }
        return List.copyOf(vertices);
    }

    private static @Unmodifiable List<DrawSection> createVarying(BeamPropagation propagation, double layerScale) {
        List<Segment> segments = propagation.segments();
        double[] pathDistances = new double[segments.size()];
        List<List<Vec3>> terminalSections = new ArrayList<>();
        List<List<Point>> boundaries = new ArrayList<>();
        Terminal[] terminals = new Terminal[segments.size()];
        List<DrawSection> result = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            Segment segment = segments.get(index);
            BeamState beam = segment.start();
            BeamFrame frame = beam.frame();
            Vec3 beamDirection = frame.direction();
            double remainingRange = segment.remainingRange();
            BeamFootprint footprint = beam.footprint();
            double offsetX = footprint.horizontalOffset();
            double offsetY = footprint.verticalOffset();
            double scaleX = footprint.horizontalScale();
            double scaleY = footprint.verticalScale();
            BeamRegion region = footprint.domain();
            List<Point> boundary = region.vertices();
            double left = Math.max(-layerScale, boundary.stream().mapToDouble(Point::u).min().orElse(offsetX - scaleX));
            double right = Math.min(layerScale, boundary.stream().mapToDouble(Point::u).max().orElse(offsetX + scaleX));
            double bottom = Math.max(-layerScale, boundary.stream().mapToDouble(Point::v).min().orElse(offsetY - scaleY));
            double top = Math.min(layerScale, boundary.stream().mapToDouble(Point::v).max().orElse(offsetY + scaleY));
            terminalSections.add(List.of());
            boundaries.add(List.of());
            if (right <= left || top <= bottom) {
                continue;
            }

            double length = segment.length();
            OpticalHit terminalHit = segment.hit();
            BeamRegion clipped = region.clip(-1, 0, -layerScale).clip(1, 0, -layerScale).clip(0, -1, -layerScale).clip(0, 1, -layerScale);
            List<Point> clippedBoundary = clipped.vertices();
            if (clippedBoundary.size() < 3) {
                continue;
            }

            BeamProfile profile = beam.profile();
            LaserBeamDisplayProfile display = LaserBeamDisplayProfile.fromBeam(beam);
            BeamRegion sourceRegion = footprint.region();
            int parent = segment.parentIndex();
            boolean sharedMirrorBoundary = false;
            boolean sharedTransmissionBoundary = false;
            if (parent >= 0 && sourceRegion != null) {
                Segment predecessor = segments.get(parent);
                OpticalHit hit = predecessor.hit();
                BeamState previousBeam = predecessor.start();
                BeamRegion previousRegion = previousBeam.footprint().region();
                List<Point> previousBoundary = boundaries.get(parent);
                if (hit != null && previousRegion != null && !previousBoundary.isEmpty()) {
                    Vec3 normal = hit.surface().normal();
                    BeamFrame incoming = previousBeam.frame();
                    Vec3 direction = incoming.direction();
                    Vec3 axis = incoming.transverseAxis();
                    Vec3 transverse = frame.transverseAxis();
                    BeamRegion reflected = new BeamRegion(previousRegion.vertices().reversed().stream().map(point -> new Point(point.u(), -point.v())).toList());
                    sharedMirrorBoundary = sourceRegion.equals(reflected) && predecessor.incident().profile().equals(profile) && direction.subtract(normal.scale(2 * direction.dot(normal))).distanceToSqr(beamDirection) <= DIRECTION_TOLERANCE_SQR && axis.subtract(normal.scale(2 * axis.dot(normal))).distanceToSqr(transverse) <= DIRECTION_TOLERANCE_SQR;
                    sharedTransmissionBoundary = sourceRegion.equals(previousRegion) && direction.distanceToSqr(beamDirection) <= DIRECTION_TOLERANCE_SQR && axis.distanceToSqr(transverse) <= DIRECTION_TOLERANCE_SQR;
                    if (sharedTransmissionBoundary) {
                        clippedBoundary = previousBoundary;
                    }
                    if (sharedMirrorBoundary) {
                        clippedBoundary = previousBoundary.stream().map(point -> new Point(point.u(), -point.v())).toList();
                    }
                }
            }
            boundaries.set(index, clippedBoundary);
            double[] horizontal = clippedBoundary.stream().mapToDouble(Point::u).toArray();
            double[] vertical = clippedBoundary.stream().mapToDouble(Point::v).toArray();
            List<Contact> contacts = new ArrayList<>();
            double terminalDistance = length;
            if (terminalHit != null) {
                Terminal terminal = display.createTerminal(beam, remainingRange, terminalHit, clippedBoundary);
                terminals[index] = terminal;
                for (int corner = 0; corner < horizontal.length; corner++) {
                    Contact contact = terminal.intersect(horizontal[corner], vertical[corner]);
                    if (contact == null) {
                        throw new IllegalStateException("Accepted beam has no terminal boundary intersection at segment " + index + '.');
                    }

                    contacts.add(contact);
                    terminalDistance = Math.max(terminalDistance, contact.distance());
                }
            }
            SamplingDomain domain = new SamplingDomain((left + 1) / 2, (bottom + 1) / 2, (right + 1) / 2, (top + 1) / 2, sourceRegion);
            List<Double> samples = display.sampleDistances(terminalDistance);
            List<Vec3> starts = varyingSection(display, 0, horizontal, vertical, List.of());
            if (parent >= 0) {
                pathDistances[index] = pathDistances[parent] + segments.get(parent).length();
                List<Vec3> previous = terminalSections.get(parent);
                Segment predecessor = segments.get(parent);
                OpticalHit contact = predecessor.hit();
                if (sharedMirrorBoundary || sharedTransmissionBoundary) {
                    starts = previous;
                }
                else if (sourceRegion != null && contact != null) {
                    Vec3 normal = contact.surface().normal();
                    boolean reflected = predecessor.incident().frame().direction().dot(normal) * beamDirection.dot(normal) < 0;
                    Terminal terminal = terminals[parent];
                    if (terminal == null) {
                        throw new IllegalStateException("Accepted beam has no shared display terminal at segment " + index + '.');
                    }

                    List<Vec3> projected = new ArrayList<>();
                    for (Point point : clippedBoundary) {
                        Contact vertex = terminal.intersect(point.u(), reflected ? -point.v() : point.v());
                        if (vertex == null) {
                            throw new IllegalStateException("Accepted beam has no shared boundary intersection at segment " + index + '.');
                        }

                        projected.add(vertex.position());
                    }
                    starts = List.copyOf(projected);
                }
                else if (previous.size() == horizontal.length) {
                    starts = previous;
                    if (contact != null) {
                        Vec3 normal = contact.surface().normal();
                        if (predecessor.incident().frame().direction().dot(normal) * beamDirection.dot(normal) < 0) {
                            starts = previous.reversed();
                        }
                    }
                }
            }
            LaserBeamFade fade = new LaserBeamFade(frame.origin(), beamDirection, remainingRange, Math.min(LaserBeamGeometry.TERMINAL_FADE_LENGTH, pathDistances[index] + remainingRange));
            LaserBeamAppearance appearance = LaserBeamAppearance.fromBeam(beam);
            LaserBeamBrightness brightness = LaserBeamBrightness.fromBeam(beam);
            for (int step = 1; step < samples.size(); step++) {
                double from = samples.get(step - 1);
                double to = samples.get(step);
                List<Vec3> ends = varyingSection(display, to, horizontal, vertical, contacts);
                Section geometry = new Section(starts, ends, ends.stream().anyMatch(point -> fade.at(point) < 1), profile.widthSlope() != 0 || sourceRegion != null);
                result.add(new DrawSection(geometry, appearance, index, pathDistances[index] + from, fade, domain, false, brightness));
                starts = ends;
                terminalSections.set(index, ends);
            }
        }
        return List.copyOf(result);
    }

    public record DrawSection(Section geometry, LaserBeamAppearance appearance, int segmentIndex, double pathDistance, @Nullable LaserBeamFade fade, SamplingDomain samplingDomain, boolean grazingTerminal, @Nullable LaserBeamBrightness brightness, @Nullable MappedData mapped) {
        public DrawSection(Section geometry, LaserBeamAppearance appearance, int segmentIndex, double pathDistance, @Nullable LaserBeamFade fade, SamplingDomain samplingDomain, boolean grazingTerminal, @Nullable LaserBeamBrightness brightness) {
            this(geometry, appearance, segmentIndex, pathDistance, fade, samplingDomain, grazingTerminal, brightness, null);
        }

        public Section renderSupport() {
            if (mapped == null) {
                return geometry;
            }

            return mapped.support();
        }

        public double fadeAt(Vec3 point) {
            if (fade == null) {
                return 1;
            }

            return fade.at(point);
        }

        public double opacityAt(Vec3 point) {
            if (brightness == null) {
                return appearance.opacity();
            }

            return brightness.opacityAt(point, appearance.opacity());
        }

        public double getReferenceWidth() {
            double width = 0;
            for (List<Vec3> vertices : List.of(geometry.starts(), geometry.ends())) {
                if (vertices.size() != 4) {
                    continue;
                }

                Vec3 second = vertices.get(1);
                double horizontal = vertices.getFirst().distanceTo(second) / (samplingDomain.maxU() - samplingDomain.minU());
                double vertical = second.distanceTo(vertices.get(2)) / (samplingDomain.maxV() - samplingDomain.minV());
                width = Math.max(width, Math.min(horizontal, vertical));
            }
            return width;
        }
    }

    public record MappedData(Section support, LaserBeamBrightness widthField, LaserBeamBrightness haloField, @Nullable List<OpticalPlane> endPlanes) {
        public MappedData {
            endPlanes = endPlanes == null ? null : List.copyOf(endPlanes);
        }
    }

    public record SamplingDomain(double minU, double minV, double maxU, double maxV, @Nullable BeamRegion region) {
        public static final SamplingDomain FULL = new SamplingDomain(0, 0, 1, 1);

        public SamplingDomain(double minU, double minV, double maxU, double maxV) {
            this(minU, minV, maxU, maxV, null);
        }
    }
}
