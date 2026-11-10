package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamParticleSpace {
    private static final double DISTANCE_EPSILON = 1.0E-7;
    private static final double NORMAL_EPSILON = 1.0E-12;
    private static final Comparator<Interval> INTERVAL_ORDER = Comparator.comparingDouble(Interval::from);
    private final Volume[] cores;
    private final ShellVolume[] shells;

    LaserBeamParticleSpace(List<DrawSection> sections) {
        this(sections, List.of());
    }

    LaserBeamParticleSpace(List<DrawSection> sections, List<Face> surfaces) {
        List<DrawSection> parts = sections.stream().flatMap(draw -> LaserBeamMesh.split(draw).stream()).toList();
        List<Volume> cores = new ArrayList<>(parts.size());
        for (DrawSection draw : parts) {
            if (draw.appearance().opacity() == 0) {
                continue;
            }

            Section geometry = draw.geometry();
            List<List<Vec3>> sides = geometry.createSides(draw.getReferenceWidth());
            if (sides.isEmpty()) {
                continue;
            }

            cores.add(new Volume(geometry, sides));
        }

        this.cores = cores.toArray(Volume[]::new);
        Map<DrawSection, List<Face>> groups = new LinkedHashMap<>();
        for (Face face : surfaces) {
            groups.computeIfAbsent(face.draw(), ignored -> new ArrayList<>()).add(face);
        }

        List<ShellVolume> shells = new ArrayList<>(groups.size());
        for (Entry<DrawSection, List<Face>> entry : groups.entrySet()) {
            shells.add(new ShellVolume(entry.getKey(), entry.getValue()));
        }

        this.shells = shells.toArray(ShellVolume[]::new);
    }

    Interval gap(Vec3 origin, Vec3 direction, List<Interval> intervals) {
        intervals.clear();
        for (Volume core : cores) {
            Interval interval = core.intersect(origin, direction);
            if (interval == null) {
                continue;
            }

            intervals.add(interval);
        }

        intervals.sort(INTERVAL_ORDER);
        double from = 0;
        for (Interval interval : intervals) {
            if (interval.from() > from + DISTANCE_EPSILON) {
                return new Interval(from, interval.from());
            }

            from = Math.max(from, interval.to());
        }

        return new Interval(from, Double.POSITIVE_INFINITY);
    }

    double clearance(Vec3 point, double limit) {
        for (Volume core : cores) {
            limit = core.clearance(point, limit);
            if (limit <= 0) {
                return 0;
            }
        }

        return limit;
    }

    double shellClearance(Vec3 point, double limit) {
        double clearance = 0;
        for (ShellVolume shell : shells) {
            clearance = Math.max(clearance, shell.clearance(point, limit));
            if (clearance >= limit) {
                return limit;
            }
        }

        return clearance;
    }

    record Interval(double from, double to) {}

    private static final class ShellVolume {
        private final Plane[] planes;

        private ShellVolume(DrawSection draw, List<Face> faces) {
            Section geometry = draw.geometry();
            Vec3 center = Section.center(geometry.starts()).lerp(Section.center(geometry.ends()), 0.5);
            List<Vec3> sides = new ArrayList<>();
            for (List<Vec3> side : geometry.createSides(draw.getReferenceWidth())) {
                Vec3 first = side.getFirst();
                Vec3 normal = side.get(1).subtract(first).cross(side.getLast().subtract(first)).normalize();
                if (center.subtract(first).dot(normal) > 0) {
                    normal = normal.scale(-1);
                }

                sides.add(normal);
            }

            boolean[] covered = new boolean[sides.size()];
            List<Plane> planes = new ArrayList<>();
            for (Face face : faces) {
                Vec3 normal = face.normal();
                double best = 0;
                int side = -1;
                for (int index = 0; index < sides.size(); index++) {
                    double alignment = normal.dot(sides.get(index));
                    if (!(alignment > best)) {
                        continue;
                    }

                    best = alignment;
                    side = index;
                }
                if (side >= 0) {
                    covered[side] = true;
                }

                planes.add(new Plane(face.vertices().getFirst(), normal));
            }

            boolean complete = sides.size() >= 3;
            for (boolean side : covered) {
                complete &= side;
            }
            for (List<Vec3> cap : List.of(geometry.starts(), geometry.ends())) {
                Vec3 first = cap.getFirst();
                Vec3 normal = cap.get(1).subtract(first).cross(cap.getLast().subtract(first)).normalize();
                complete &= normal.lengthSqr() > NORMAL_EPSILON;
                if (center.subtract(first).dot(normal) > 0) {
                    normal = normal.scale(-1);
                }

                planes.add(new Plane(first, normal));
            }
            if (!complete) {
                planes.clear();
            }

            this.planes = planes.toArray(Plane[]::new);
        }

        private double clearance(Vec3 point, double limit) {
            if (planes.length == 0) {
                return 0;
            }

            for (Plane plane : planes) {
                limit = Math.min(limit, plane.origin().subtract(point).dot(plane.normal()));
                if (limit <= 0) {
                    return 0;
                }
            }

            return limit;
        }
    }

    private static final class Volume {
        private final Plane[] planes;
        private final AABB bounds;

        private Volume(Section geometry, List<List<Vec3>> sides) {
            List<List<Vec3>> faces = new ArrayList<>(sides);
            faces.add(geometry.starts());
            faces.add(geometry.ends());
            Vec3 center = Section.center(geometry.starts()).lerp(Section.center(geometry.ends()), 0.5);
            List<Plane> planes = new ArrayList<>(faces.size());
            Vec3 anchor = geometry.starts().getFirst();
            AABB bounds = new AABB(anchor, anchor);
            for (List<Vec3> face : faces) {
                for (Vec3 point : face) {
                    bounds = bounds.minmax(new AABB(point, point));
                }

                Vec3 first = face.getFirst();
                for (int index = 1; index + 1 < face.size(); index++) {
                    Vec3 cross = face.get(index).subtract(first).cross(face.get(index + 1).subtract(first));
                    double length = cross.length();
                    if (length < NORMAL_EPSILON) {
                        continue;
                    }

                    Vec3 normal = cross.scale(1 / length);
                    if (center.subtract(first).dot(normal) > 0) {
                        normal = normal.scale(-1);
                    }

                    planes.add(new Plane(first, normal));
                    break;
                }
            }

            this.planes = planes.toArray(Plane[]::new);
            this.bounds = bounds;
        }

        private @Nullable Interval intersect(Vec3 origin, Vec3 direction) {
            double from = 0;
            double to = Double.POSITIVE_INFINITY;
            for (Plane plane : planes) {
                Vec3 point = plane.origin();
                Vec3 normal = plane.normal();
                double distance = (origin.x - point.x) * normal.x + (origin.y - point.y) * normal.y + (origin.z - point.z) * normal.z;
                double rate = direction.dot(normal);
                if (Math.abs(rate) < NORMAL_EPSILON) {
                    if (distance > DISTANCE_EPSILON) {
                        return null;
                    }

                    continue;
                }

                double crossing = -distance / rate;
                if (rate > 0) {
                    to = Math.min(to, crossing);
                }
                else {
                    from = Math.max(from, crossing);
                }

                if (from > to) {
                    return null;
                }
            }

            return new Interval(from, to);
        }

        private double clearance(Vec3 point, double limit) {
            double x = Math.max(0, Math.max(bounds.minX - point.x, point.x - bounds.maxX));
            double y = Math.max(0, Math.max(bounds.minY - point.y, point.y - bounds.maxY));
            double z = Math.max(0, Math.max(bounds.minZ - point.z, point.z - bounds.maxZ));
            if (x * x + y * y + z * z >= limit * limit) {
                return limit;
            }

            double distance = 0;
            for (Plane plane : planes) {
                Vec3 origin = plane.origin();
                Vec3 normal = plane.normal();
                distance = Math.max(distance, (point.x - origin.x) * normal.x + (point.y - origin.y) * normal.y + (point.z - origin.z) * normal.z);
            }

            return Math.min(limit, distance);
        }
    }

    private record Plane(Vec3 origin, Vec3 normal) {}
}
