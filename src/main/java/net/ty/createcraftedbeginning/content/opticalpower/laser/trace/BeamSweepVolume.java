package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BeamSweepVolume {
    private static final double EPSILON = 1.0E-7;
    private static final double MIN_AXIS_LENGTH = 1.0E-12;
    private static final int[][] EDGES = {{0, 1}, {1, 3}, {3, 2}, {2, 0}, {4, 5}, {5, 7}, {7, 6}, {6, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    private static final int[][] FACES = {{0, 1, 2}, {4, 6, 5}, {0, 4, 1}, {2, 3, 6}, {0, 2, 4}, {1, 5, 3}};
    private static final List<Vec3> AXES = List.of(new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1));
    private static final int AXIS_COUNT = AXES.size() + FACES.length + EDGES.length * AXES.size();

    private final List<Vec3> points;
    private @Nullable Projection[] projections;
    private int prepared;

    BeamSweepVolume(List<Vec3> points) {
        this.points = List.copyOf(points);
    }

    List<Vec3> points() {
        return points;
    }

    AABB bounds() {
        Vec3 first = points.getFirst();
        double minX = first.x;
        double minY = first.y;
        double minZ = first.z;
        double maxX = first.x;
        double maxY = first.y;
        double maxZ = first.z;
        for (Vec3 point : points) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    boolean intersects(AABB box) {
        return intersects(box, false);
    }

    boolean intersects(AABB box, boolean includeShallowOverlap) {
        double tolerance = includeShallowOverlap ? 0 : EPSILON;
        Projection[] cached = projections;
        if (cached == null) {
            cached = new Projection[AXIS_COUNT];
            projections = cached;
        }
        for (int index = 0; index < AXIS_COUNT; index++) {
            if (index >= prepared) {
                cached[index] = project(index);
                prepared++;
            }
            Projection projection = cached[index];
            if (projection == null) {
                continue;
            }

            Vec3 axis = projection.axis();
            double center = box.getCenter().dot(axis);
            double radius = (Math.abs(axis.x) * box.getXsize() + Math.abs(axis.y) * box.getYsize() + Math.abs(axis.z) * box.getZsize()) / 2;
            if (projection.max() <= center - radius + tolerance || projection.min() >= center + radius - tolerance) {
                return false;
            }
        }
        return true;
    }

    private @Nullable Projection project(int index) {
        Vec3 axis;
        if (index < AXES.size()) {
            axis = AXES.get(index);
        }
        else if (index < AXES.size() + FACES.length) {
            int[] face = FACES[index - AXES.size()];
            Vec3 point = points.get(face[0]);
            axis = points.get(face[1]).subtract(point).cross(points.get(face[2]).subtract(point));
        }
        else {
            int offset = index - AXES.size() - FACES.length;
            int[] edge = EDGES[offset / AXES.size()];
            Vec3 direction = points.get(edge[1]).subtract(points.get(edge[0]));
            axis = direction.cross(AXES.get(offset % AXES.size()));
        }
        double length = axis.length();
        if (length < MIN_AXIS_LENGTH) {
            return null;
        }

        Vec3 normalized = axis.scale(1 / length);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Vec3 point : points) {
            double value = point.dot(normalized);
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return new Projection(normalized, min, max);
    }

    private record Projection(Vec3 axis, double min, double max) {}
}
