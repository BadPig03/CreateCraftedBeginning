package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile.Span;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BeamSweepSection {
    static final double CONTACT_TOLERANCE = 1.0E-8;

    private final List<List<Vertex>> faces;
    private final AABB bounds;
    private final Span span;

    private BeamSweepSection(List<List<Vertex>> faces, Span span) {
        this.span = span;
        this.faces = List.copyOf(faces);
        Vec3 first = faces.getFirst().getFirst().point();
        AABB extent = new AABB(first, first);
        for (List<Vertex> face : faces) {
            for (Vertex vertex : face) {
                Vec3 point = vertex.point();
                extent = extent.minmax(new AABB(point, point));
            }
        }
        bounds = extent;
    }

    static List<BeamSweepSection> create(BeamState beam, double range, @Nullable OpticalPlane startPlane) {
        List<BeamSweepSection> result = new ArrayList<>();
        List<Point> domain = beam.footprint().domain().vertices();
        for (Span span : beam.profile().spans(range)) {
            List<Vertex> start = new ArrayList<>();
            List<Vertex> end = new ArrayList<>();
            for (Point point : domain) {
                start.add(new Vertex(BeamContact.point(beam, span.from(), point.u(), point.v()), span.from()));
                end.add(new Vertex(BeamContact.point(beam, span.to(), point.u(), point.v()), span.to()));
            }
            List<List<Vertex>> faces = new ArrayList<>();
            faces.add(List.copyOf(start));
            faces.add(List.copyOf(end));
            for (int index = 0; index < domain.size(); index++) {
                int next = (index + 1) % domain.size();
                faces.add(List.of(start.get(index), start.get(next), end.get(next), end.get(index)));
            }
            if (startPlane != null) {
                faces = clip(faces, startPlane.point(), startPlane.normal());
            }
            if (faces.isEmpty()) {
                continue;
            }

            result.add(new BeamSweepSection(faces, span));
        }
        return List.copyOf(result);
    }

    private static List<List<Vertex>> clip(List<List<Vertex>> faces, Vec3 origin, Vec3 normal) {
        List<List<Vertex>> result = new ArrayList<>();
        List<Vertex> cap = new ArrayList<>();
        for (List<Vertex> face : faces) {
            List<Vertex> polygon = new ArrayList<>();
            Vertex previous = face.getLast();
            double previousSide = previous.point().subtract(origin).dot(normal);
            for (Vertex current : face) {
                double side = current.point().subtract(origin).dot(normal);
                boolean inside = side >= -CONTACT_TOLERANCE;
                boolean previousInside = previousSide >= -CONTACT_TOLERANCE;
                if (inside != previousInside) {
                    double fraction = Math.clamp(previousSide / (previousSide - side), 0, 1);
                    Vertex intersection = new Vertex(previous.point().lerp(current.point(), fraction), previous.distance() + (current.distance() - previous.distance()) * fraction);
                    polygon.add(intersection);
                    cap.add(intersection);
                }
                if (inside) {
                    polygon.add(current);
                    if (Math.abs(side) <= CONTACT_TOLERANCE) {
                        cap.add(current);
                    }
                }
                previous = current;
                previousSide = side;
            }
            if (polygon.isEmpty()) {
                continue;
            }

            result.add(List.copyOf(polygon));
        }
        cap = new ArrayList<>(cap.stream().distinct().toList());
        if (cap.size() < 3) {
            return result;
        }

        Vec3 center = Vec3.ZERO;
        for (Vertex vertex : cap) {
            center = center.add(vertex.point());
        }
        Vec3 anchor = center.scale(1.0 / cap.size());
        Vec3 reference = Math.abs(normal.x) < 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 horizontal = normal.cross(reference).normalize();
        Vec3 vertical = normal.cross(horizontal);
        cap.sort(Comparator.comparingDouble(vertex -> Math.atan2(vertex.point().subtract(anchor).dot(vertical), vertex.point().subtract(anchor).dot(horizontal))));
        result.add(List.copyOf(cap));
        return result;
    }

    Span span() {
        return span;
    }

    AABB bounds() {
        return bounds;
    }

    List<Vec3> points() {
        return faces.stream().flatMap(List::stream).map(Vertex::point).distinct().toList();
    }

    BeamSweepSection transform(UnaryOperator<Vec3> transform) {
        List<List<Vertex>> transformed = new ArrayList<>();
        for (List<Vertex> face : faces) {
            transformed.add(face.stream().map(vertex -> new Vertex(transform.apply(vertex.point()), vertex.distance())).toList());
        }
        return new BeamSweepSection(transformed, span);
    }

    @Nullable BeamSweepSection clip(OpticalPlane plane) {
        List<List<Vertex>> clipped = clip(faces, plane.point(), plane.normal());
        if (clipped.isEmpty()) {
            return null;
        }

        return new BeamSweepSection(clipped, span);
    }

    @Nullable BeamSweepSection clip(AABB box) {
        List<List<Vertex>> clipped = faces;
        for (Direction face : Iterate.directions) {
            Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
            Vec3 origin = new Vec3(normal.x > 0 ? box.minX : box.maxX, normal.y > 0 ? box.minY : box.maxY, normal.z > 0 ? box.minZ : box.maxZ);
            clipped = clip(clipped, origin, normal);
            if (clipped.isEmpty()) {
                return null;
            }
        }
        return new BeamSweepSection(clipped, span);
    }

    @Nullable Contact intersect(AABB box, Vec3 direction, @Nullable OpticalPlane ignoredPlane) {
        double tolerance = CONTACT_TOLERANCE;
        if (bounds.maxX < box.minX - tolerance || bounds.minX > box.maxX + tolerance || bounds.maxY < box.minY - tolerance || bounds.minY > box.maxY + tolerance || bounds.maxZ < box.minZ - tolerance || bounds.minZ > box.maxZ + tolerance) {
            return null;
        }

        List<List<Vertex>> intersection = faces;
        for (Direction face : Iterate.directions) {
            Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
            Vec3 origin = new Vec3(normal.x > 0 ? box.minX : box.maxX, normal.y > 0 ? box.minY : box.maxY, normal.z > 0 ? box.minZ : box.maxZ);
            intersection = clip(intersection, origin, normal);
            if (intersection.isEmpty()) {
                return null;
            }
        }
        List<Vertex> vertices = intersection.stream().flatMap(List::stream).distinct().toList();
        if (ignoredPlane != null && vertices.stream().allMatch(vertex -> Math.abs(vertex.point().subtract(ignoredPlane.point()).dot(ignoredPlane.normal())) <= tolerance)) {
            return null;
        }

        double distance = vertices.stream().mapToDouble(Vertex::distance).min().orElseThrow();
        Vec3 point = Vec3.ZERO;
        int count = 0;
        for (Vertex vertex : vertices) {
            if (vertex.distance() > distance + tolerance) {
                continue;
            }

            point = point.add(vertex.point());
            count++;
        }
        point = point.scale(1.0 / count);
        Vec3 normal = direction.scale(-1).normalize();
        double nearest = Double.POSITIVE_INFINITY;
        double incidence = Double.POSITIVE_INFINITY;
        for (Direction face : Iterate.directions) {
            Vec3 candidate = Vec3.atLowerCornerOf(face.getNormal());
            Vec3 corner = new Vec3(candidate.x < 0 ? box.minX : box.maxX, candidate.y < 0 ? box.minY : box.maxY, candidate.z < 0 ? box.minZ : box.maxZ);
            double separation = Math.abs(point.subtract(corner).dot(candidate));
            double facing = direction.dot(candidate);
            if (separation > nearest + tolerance || Math.abs(separation - nearest) <= tolerance && facing >= incidence) {
                continue;
            }

            nearest = separation;
            incidence = facing;
            normal = candidate;
        }
        return new Contact(distance, point, normal);
    }

    record Contact(double distance, Vec3 point, Vec3 normal) {}

    private record Vertex(Vec3 point, double distance) {}
}
