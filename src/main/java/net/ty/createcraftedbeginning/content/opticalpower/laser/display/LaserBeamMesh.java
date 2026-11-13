package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Boundary;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayProfile.Contact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayProfile.Terminal;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderSnapshot;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamMesh {
    private static final double EMISSION_OFFSET = 9.765625E-4;

    private LaserBeamMesh() {
    }

    public static @Unmodifiable List<DrawSection> create(BeamPropagation propagation) {
        return create(propagation, 1);
    }

    public static @Unmodifiable List<DrawSection> create(BeamPropagation propagation, double layerScale) {
        if (!Double.isFinite(layerScale) || layerScale <= 0 || layerScale > 1) {
            throw new IllegalArgumentException("Beam layer scale must be finite and in (0, 1]; got " + layerScale + '.');
        }

        List<DrawSection> result = new ArrayList<>();
        List<Segment> segments = propagation.segments();
        List<Vec3> previousEnd = List.of();
        double pathDistance = 0;
        for (int index = 0; index < segments.size(); index++) {
            Segment segment = segments.get(index);
            if (segment.parentIndex() != index - 1) {
                throw new IllegalArgumentException("Whole-beam display requires a single ordered chain at segment " + index + '.');
            }

            BeamState beam = segment.start();
            BeamFrame frame = beam.frame();
            LaserBeamDisplayProfile display = LaserBeamDisplayProfile.fromBeam(beam);
            BeamRegion domain = beam.footprint().domain().clip(-1, 0, -layerScale).clip(1, 0, -layerScale).clip(0, -1, -layerScale).clip(0, 1, -layerScale);
            List<Point> boundary = domain.vertices();
            if (boundary.size() < 3) {
                throw new IllegalArgumentException("Whole-beam display has an empty cross section at segment " + index + '.');
            }

            double length = segment.length();
            double range = segment.remainingRange();
            Boundary cutoff = segment.boundary();
            OpticalPlane plane = cutoff == null ? null : cutoff.cutoffPlane();
            List<Contact> contacts = new ArrayList<>();
            if (plane != null && cutoff.opticalInteraction()) {
                Terminal terminal = display.createTerminal(beam, range, plane, boundary);
                for (Point point : boundary) {
                    Contact contact = terminal.intersect(point.u(), point.v());
                    if (contact == null) {
                        throw new IllegalStateException("Whole-beam optical joint has no boundary contact at segment " + index + '.');
                    }

                    contacts.add(contact);
                    length = Math.max(length, contact.distance());
                }
            }

            double emission = index == 0 ? Math.min(EMISSION_OFFSET, segment.length() * 0.25) : 0;
            List<Vec3> starts = boundary.stream().map(point -> display.point(emission, point.u(), point.v())).toList();
            if (index > 0) {
                Segment parent = segments.get(index - 1);
                Boundary joint = parent.boundary();
                if (joint == null || !joint.opticalInteraction() || previousEnd.size() != boundary.size()) {
                    throw new IllegalStateException("Whole-beam display requires a complete optical joint before segment " + index + '.');
                }

                Vec3 normal = joint.cutoffPlane().normal();
                boolean reflected = parent.start().frame().direction().dot(normal) * frame.direction().dot(normal) < 0;
                starts = reflected ? previousEnd.reversed() : previousEnd;
            }

            LaserBeamFade fade = new LaserBeamFade(frame.origin(), frame.direction(), range, Math.min(LaserBeamGeometry.TERMINAL_FADE_LENGTH, pathDistance + range));
            LaserBeamAppearance appearance = LaserBeamAppearance.fromBeam(beam);
            LaserBeamBrightness brightness = LaserBeamBrightness.fromBeam(beam);
            SamplingDomain sampling = sampling(domain);
            BeamTerminal terminal = segment.terminal();
            List<Double> samples = display.sampleDistances(length);
            for (int step = 1; step < samples.size(); step++) {
                double from = samples.get(step - 1);
                double to = samples.get(step);
                List<Vec3> ends = section(display, to, boundary, contacts);
                if (step == samples.size() - 1 && terminal != null && !terminal.faces().isEmpty()) {
                    ends = boundary.stream().map(point -> BeamContact.point(beam, segment.length(), point.u(), point.v())).toList();
                }
                Section geometry = new Section(starts, ends, ends.stream().anyMatch(point -> fade.at(point) < 1), beam.profile().widthSlope() != 0);
                result.add(new DrawSection(geometry, appearance, index, pathDistance + from, fade, sampling, false, brightness));
                starts = ends;
            }
            previousEnd = starts;
            if (terminal != null && !terminal.faces().isEmpty()) {
                result.addAll(LaserBeamTerminalMesh.create(segment, index, pathDistance, domain, fade));
            }

            pathDistance += segment.length();
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
            result.add(new DrawSection(section, draw.appearance(), draw.segmentIndex(), draw.pathDistance() + partStart.distanceTo(start), fade, draw.samplingDomain(), false, draw.brightness()));
        }
        return List.copyOf(result);
    }

    static SamplingDomain sampling(BeamRegion region) {
        List<Point> boundary = region.vertices();
        double left = boundary.stream().mapToDouble(Point::u).min().orElseThrow();
        double right = boundary.stream().mapToDouble(Point::u).max().orElseThrow();
        double bottom = boundary.stream().mapToDouble(Point::v).min().orElseThrow();
        double top = boundary.stream().mapToDouble(Point::v).max().orElseThrow();
        return new SamplingDomain((left + 1) / 2, (bottom + 1) / 2, (right + 1) / 2, (top + 1) / 2, region);
    }

    private static List<Vec3> section(LaserBeamDisplayProfile display, double distance, List<Point> boundary, List<Contact> contacts) {
        List<Vec3> vertices = new ArrayList<>(boundary.size());
        for (int corner = 0; corner < boundary.size(); corner++) {
            if (!contacts.isEmpty()) {
                Contact contact = contacts.get(corner);
                if (distance >= contact.distance()) {
                    vertices.add(contact.position());
                    continue;
                }
            }

            Point point = boundary.get(corner);
            vertices.add(display.point(distance, point.u(), point.v()));
        }
        return List.copyOf(vertices);
    }

    public record DrawSection(Section geometry, LaserBeamAppearance appearance, int segmentIndex, double pathDistance, @Nullable LaserBeamFade fade, SamplingDomain samplingDomain, boolean grazingTerminal, @Nullable LaserBeamBrightness brightness) {

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

    public record SamplingDomain(double minU, double minV, double maxU, double maxV, @Nullable BeamRegion region) {
        public static final SamplingDomain FULL = new SamplingDomain(0, 0, 1, 1);

        public SamplingDomain(double minU, double minV, double maxU, double maxV) {
            this(minU, minV, maxU, maxV, null);
        }
    }
}
