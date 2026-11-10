package net.ty.createcraftedbeginning.content.opticalpower.laser.geometry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile.Span;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record BeamContact(List<Patch> patches, boolean uniformDensity, boolean splitHorizontal) {
    private static final double PLANE_EPSILON = 1.0E-9;
    private static final double DENSITY_TOLERANCE = 0.001;

    public BeamContact {
        patches = List.copyOf(patches);
    }

    public static @Nullable BeamContact create(BeamState beam, double range, OpticalHit hit) {
        BeamFootprint footprint = beam.footprint();
        BeamProfile profile = beam.profile();
        BeamRegion region = footprint.region();
        if (region != null) {
            List<Vec3> vertices = new ArrayList<>();
            List<Span> spans = profile.spans(range);
            double minimumDistance = Double.POSITIVE_INFINITY;
            double maximumDistance = 0;
            for (Point point : region.vertices()) {
                Vertex vertex = intersect(beam, spans, hit, point.u(), point.v());
                if (vertex == null) {
                    return null;
                }

                vertices.add(vertex.offset());
                double distance = vertex.distance();
                minimumDistance = Math.min(minimumDistance, distance);
                maximumDistance = Math.max(maximumDistance, distance);
            }
            Span tail = spans.getLast();
            double tailStart = tail.from();
            BeamProfile tailProfile = profile.advance(tailStart);
            double slope = tail.radiusSlope();
            Density density = null;
            boolean linear = slope > 0 && minimumDistance >= tailStart - PLANE_EPSILON;
            Span head = spans.getFirst();
            double incomingSlope = head.radiusSlope();
            if (!linear && incomingSlope < 0 && maximumDistance <= head.to() + PLANE_EPSILON) {
                tailStart = 0;
                tailProfile = profile;
                slope = incomingSlope;
                linear = true;
            }
            if (linear) {
                BeamFrame frame = beam.frame();
                double radius = profile.radius();
                Vec3 direction = frame.direction();
                Vec3 transverse = frame.transverseAxis();
                Vec3 focus = frame.origin().subtract(hit.position()).add(direction.scale(tailStart - tailProfile.radius() / slope)).subtract(transverse.scale(radius * footprint.horizontalOffset())).subtract(frame.getSecondTransverseAxis().scale(radius * footprint.verticalOffset()));
                density = new Density(focus, direction, transverse, slope, 1 / region.area());
            }
            return new BeamContact(List.of(new Patch(vertices, 1, density)), density == null, false);
        }

        Vertex[][] vertices = new Vertex[3][3];
        List<Span> spans = profile.spans(range);
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                double u = footprint.horizontalOffset() + (x - 1) * footprint.horizontalScale();
                double v = footprint.verticalOffset() + (y - 1) * footprint.verticalScale();
                Vertex vertex = intersect(beam, spans, hit, u, v);
                if (vertex == null) {
                    return null;
                }

                vertices[x][y] = vertex;
            }
        }
        List<Patch> patches = new ArrayList<>(4);
        double[] areas = new double[4];
        double minimumArea = Double.POSITIVE_INFINITY;
        double maximumArea = 0;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                Patch patch = new Patch(List.of(vertices[x][y].offset(), vertices[x + 1][y].offset(), vertices[x + 1][y + 1].offset(), vertices[x][y + 1].offset()), 0.25);
                double area = patch.area();
                if (!(area > 0) || !Double.isFinite(area)) {
                    return null;
                }

                minimumArea = Math.min(minimumArea, area);
                maximumArea = Math.max(maximumArea, area);
                areas[patches.size()] = area;
                patches.add(patch);
            }
        }
        double horizontalChange = Math.abs(areas[0] - areas[2]) + Math.abs(areas[1] - areas[3]);
        double verticalChange = Math.abs(areas[0] - areas[1]) + Math.abs(areas[2] - areas[3]);
        return new BeamContact(patches, maximumArea <= minimumArea * (1 + DENSITY_TOLERANCE), horizontalChange >= verticalChange);
    }

    public static Vec3 point(BeamState beam, double distance, double u, double v) {
        return offset(beam, distance, u, v).add(beam.frame().origin());
    }

    public static @Nullable Vertex intersect(BeamState beam, List<Span> spans, OpticalHit hit, double u, double v) {
        BeamFrame frame = beam.frame();
        Vec3 normal = hit.surface().normal();
        Vec3 plane = hit.position();
        Vec3 origin = frame.origin().subtract(plane);
        Vec3 first = origin.add(offset(beam, 0, u, v));
        double firstSide = first.dot(normal);
        for (Span span : spans) {
            double from = span.from();
            double to = span.to();
            Vec3 last = origin.add(offset(beam, to, u, v));
            double lastSide = last.dot(normal);
            double change = firstSide - lastSide;
            if (change > 0 && firstSide >= -PLANE_EPSILON && lastSide <= PLANE_EPSILON) {
                double fraction = Math.clamp(firstSide / change, 0, 1);
                Vec3 contact = first.lerp(last, fraction);
                return new Vertex(plane.add(contact), contact, from + (to - from) * fraction);
            }

            first = last;
            firstSide = lastSide;
        }
        return null;
    }

    private static Vec3 offset(BeamState beam, double distance, double u, double v) {
        BeamFrame frame = beam.frame();
        BeamFootprint footprint = beam.footprint();
        BeamProfile profile = beam.profile();
        double radius = profile.advance(distance).radius();
        double initialRadius = profile.radius();
        return frame.direction().scale(distance).add(frame.transverseAxis().scale(radius * u - initialRadius * footprint.horizontalOffset())).add(frame.getSecondTransverseAxis().scale(radius * v - initialRadius * footprint.verticalOffset()));
    }

    public record Vertex(Vec3 position, Vec3 offset, double distance) {}

    public record Patch(List<Vec3> vertices, double fluxFraction, @Nullable Density density) {
        public Patch(List<Vec3> vertices, double fluxFraction) {
            this(vertices, fluxFraction, null);
        }

        public Patch {
            vertices = List.copyOf(vertices);
        }

        public double area() {
            Vec3 origin = vertices.getFirst();
            Vec3 cross = Vec3.ZERO;
            for (int index = 1; index + 1 < vertices.size(); index++) {
                cross = cross.add(vertices.get(index).subtract(origin).cross(vertices.get(index + 1).subtract(origin)));
            }
            return cross.length() / 2;
        }
    }

    public record Density(Vec3 focus, Vec3 direction, Vec3 transverse, double slope, double fluxPerSourceArea) {
        public Point project(Vec3 point) {
            Vec3 delta = point.subtract(focus);
            double radius = delta.dot(direction) * slope;
            return new Point(delta.dot(transverse) / radius, delta.dot(direction.cross(transverse)) / radius);
        }

        public double at(Vec3 point, Vec3 normal) {
            Vec3 delta = point.subtract(focus);
            double distance = delta.dot(direction);
            return fluxPerSourceArea * Math.abs(delta.dot(normal)) / (slope * slope * Math.abs(distance * distance * distance));
        }
    }
}
