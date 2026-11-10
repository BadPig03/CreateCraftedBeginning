package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamParticleIndex {
    static final double MIN_GEOMETRY_LENGTH = 1.0E-6;
    private static final double MIN_RAY_DETERMINANT = 1.0E-12;
    private static final double RAY_BOUNDS_PADDING = 1.0E-6;
    private static final double MIN_TRIANGLE_NORMAL_LENGTH = 1.0E-12;
    private static final double TRIANGLE_EDGE_TOLERANCE = 1.0E-12;
    private static final double MIN_TRIANGLE_EDGE_LENGTH_SQR = 1.0E-12;
    static final double PLANE_CLEARANCE_SIZE_FRACTION = 0.4;
    private static final int QUERY_LEAF_SIZE = 8;
    private static final Comparator<ShellCrossing> CROSSING_ORDER = Comparator.comparingDouble(ShellCrossing::distance);

    private final int first;
    private final int end;
    private final AABB bounds;
    private final boolean finiteBounds;
    private final List<LaserBeamParticleIndex> children;

    LaserBeamParticleIndex(ShellFace[] frames, int first, int end) {
        this.first = first;
        this.end = end;
        AABB bounds = first < end ? frames[first].face.bounds() : new AABB(0, 0, 0, 0, 0, 0);
        boolean finiteBounds = true;
        for (int index = first; index < end; index++) {
            ShellFace frame = frames[index];
            AABB faceBounds = frame.face.bounds();
            bounds = bounds.minmax(faceBounds);
            finiteBounds &= Double.isFinite(faceBounds.minX) && Double.isFinite(faceBounds.minY) && Double.isFinite(faceBounds.minZ) && Double.isFinite(faceBounds.maxX) && Double.isFinite(faceBounds.maxY) && Double.isFinite(faceBounds.maxZ);
        }

        this.bounds = bounds;
        this.finiteBounds = finiteBounds;
        if (end - first <= QUERY_LEAF_SIZE) {
            children = List.of();
            return;
        }

        int middle = first + (end - first) / 2;
        children = List.of(new LaserBeamParticleIndex(frames, first, middle), new LaserBeamParticleIndex(frames, middle, end));
    }

    private LaserBeamParticleIndex(int first, int end, AABB bounds, boolean finiteBounds, List<LaserBeamParticleIndex> children) {
        this.first = first;
        this.end = end;
        this.bounds = bounds;
        this.finiteBounds = finiteBounds;
        this.children = children;
    }

    private static double rayDistance(ShellFace frame, Vec3 origin, Vec3 direction) {
        Vec3 first = frame.face.vertices().getFirst();
        double offsetX = origin.x - first.x;
        double offsetY = origin.y - first.y;
        double offsetZ = origin.z - first.z;
        double distance = Double.POSITIVE_INFINITY;
        for (ShellTriangle triangle : frame.triangles) {
            Vec3 firstEdge = triangle.firstEdge;
            Vec3 secondEdge = triangle.secondEdge;
            double crossX = direction.y * secondEdge.z - direction.z * secondEdge.y;
            double crossY = direction.z * secondEdge.x - direction.x * secondEdge.z;
            double crossZ = direction.x * secondEdge.y - direction.y * secondEdge.x;
            double determinant = firstEdge.x * crossX + firstEdge.y * crossY + firstEdge.z * crossZ;
            if (Math.abs(determinant) < MIN_RAY_DETERMINANT) {
                continue;
            }

            double u = (offsetX * crossX + offsetY * crossY + offsetZ * crossZ) / determinant;
            double projectedX = offsetY * firstEdge.z - offsetZ * firstEdge.y;
            double projectedY = offsetZ * firstEdge.x - offsetX * firstEdge.z;
            double projectedZ = offsetX * firstEdge.y - offsetY * firstEdge.x;
            double v = (direction.x * projectedX + direction.y * projectedY + direction.z * projectedZ) / determinant;
            double hit = (secondEdge.x * projectedX + secondEdge.y * projectedY + secondEdge.z * projectedZ) / determinant;
            if (u < -MIN_GEOMETRY_LENGTH || v < -MIN_GEOMETRY_LENGTH || u + v > 1 + MIN_GEOMETRY_LENGTH || hit <= MIN_GEOMETRY_LENGTH) {
                continue;
            }

            distance = Math.min(distance, hit);
        }

        return distance;
    }

    private static double surfaceDistance(ShellFace frame, Vec3 point, double limit) {
        AABB bounds = frame.face.bounds();
        double x = Math.max(0, Math.max(bounds.minX - point.x, point.x - bounds.maxX));
        double y = Math.max(0, Math.max(bounds.minY - point.y, point.y - bounds.maxY));
        double z = Math.max(0, Math.max(bounds.minZ - point.z, point.z - bounds.maxZ));
        if (x * x + y * y + z * z >= limit * limit) {
            return limit;
        }

        double distance = Double.POSITIVE_INFINITY;
        for (ShellTriangle triangle : frame.triangles) {
            if (triangle.normalLength < MIN_TRIANGLE_NORMAL_LENGTH) {
                continue;
            }

            Vec3 normal = triangle.normal;
            Vec3 first = triangle.vertices.getFirst();
            double signed = point.subtract(first).dot(normal);
            Vec3 projected = point.subtract(normal.scale(signed));
            boolean inside = true;
            for (int edge = 0; edge < 3; edge++) {
                Vec3 start = triangle.vertices.get(edge);
                Vec3 delta = triangle.edges.get(edge);
                if (projected.subtract(start).dot(triangle.outwards.get(edge)) > TRIANGLE_EDGE_TOLERANCE) {
                    inside = false;
                }

                double lengthSqr = triangle.lengthsSqr[edge];
                if (lengthSqr < MIN_TRIANGLE_EDGE_LENGTH_SQR) {
                    continue;
                }

                Vec3 nearest = start.add(delta.scale(Math.clamp(point.subtract(start).dot(delta) / lengthSqr, 0, 1)));
                distance = Math.min(distance, point.distanceTo(nearest));
            }

            if (!inside) {
                continue;
            }

            distance = Math.min(distance, Math.abs(signed));
        }

        return distance;
    }

    double constrainSize(ShellFace[] frames, Vec3 point, double halfSize) {
        double limit = halfSize / PLANE_CLEARANCE_SIZE_FRACTION;
        double x = Math.max(0, Math.max(bounds.minX - point.x, point.x - bounds.maxX));
        double y = Math.max(0, Math.max(bounds.minY - point.y, point.y - bounds.maxY));
        double z = Math.max(0, Math.max(bounds.minZ - point.z, point.z - bounds.maxZ));
        if (finiteBounds && x * x + y * y + z * z >= limit * limit) {
            for (int index = first; index < end; index++) {
                double constrained = Math.min(halfSize, halfSize / PLANE_CLEARANCE_SIZE_FRACTION * PLANE_CLEARANCE_SIZE_FRACTION);
                if (constrained == halfSize) {
                    return constrained;
                }

                halfSize = constrained;
            }

            return halfSize;
        }

        if (children.isEmpty()) {
            for (int index = first; index < end; index++) {
                halfSize = Math.min(halfSize, surfaceDistance(frames[index], point, halfSize / PLANE_CLEARANCE_SIZE_FRACTION) * PLANE_CLEARANCE_SIZE_FRACTION);
            }

            return halfSize;
        }

        for (LaserBeamParticleIndex child : children) {
            halfSize = child.constrainSize(frames, point, halfSize);
        }

        return halfSize;
    }

    double exitDistance(ShellFace[] frames, Vec3 origin, Vec3 direction, double limit, List<ShellCrossing> crossings) {
        crossings.clear();
        collectCrossings(frames, origin, direction, crossings);
        crossings.sort(CROSSING_ORDER);
        int coverage = 0;
        for (ShellCrossing crossing : crossings) {
            coverage += crossing.direction();
        }

        int index = 0;
        while (index < crossings.size()) {
            double distance = crossings.get(index).distance();
            boolean outward = false;
            do {
                ShellCrossing crossing = crossings.get(index++);
                int directionSign = crossing.direction();
                coverage -= directionSign;
                outward |= directionSign > 0;
            } while (index < crossings.size() && crossings.get(index).distance() - distance <= MIN_GEOMETRY_LENGTH);
            if (!outward || coverage > 0) {
                continue;
            }

            return Math.min(limit, distance);
        }

        return limit;
    }

    ShellHit queryHit(ShellFace[] frames, Vec3 origin, Vec3 direction, ShellHit nearest) {
        if (finiteBounds && !intersectsRay(origin, direction, nearest.distance())) {
            return nearest;
        }

        if (children.isEmpty()) {
            for (int index = first; index < end; index++) {
                ShellFace frame = frames[index];
                double distance = rayDistance(frame, origin, direction);
                if (!(distance < nearest.distance())) {
                    continue;
                }

                nearest = new ShellHit(frame, distance);
            }

            return nearest;
        }

        for (LaserBeamParticleIndex child : children) {
            nearest = child.queryHit(frames, origin, direction, nearest);
        }

        return nearest;
    }

    private void collectCrossings(ShellFace[] frames, Vec3 origin, Vec3 direction, List<ShellCrossing> crossings) {
        if (finiteBounds && !intersectsRay(origin, direction, Double.POSITIVE_INFINITY)) {
            return;
        }

        if (children.isEmpty()) {
            for (int index = first; index < end; index++) {
                ShellFace frame = frames[index];
                double orientation = frame.face.normal().dot(direction);
                double distance = rayDistance(frame, origin, direction);
                if (orientation == 0 || !Double.isFinite(distance)) {
                    continue;
                }

                int crossing = 1;
                if (orientation < 0) {
                    crossing = -1;
                }

                crossings.add(new ShellCrossing(distance, crossing));
            }

            return;
        }

        for (LaserBeamParticleIndex child : children) {
            child.collectCrossings(frames, origin, direction, crossings);
        }
    }

    private boolean intersectsRay(Vec3 origin, Vec3 direction, double depth) {
        if (!Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z) || !Double.isFinite(direction.x) || !Double.isFinite(direction.y) || !Double.isFinite(direction.z)) {
            return true;
        }

        double near = 0;
        double far = depth;
        double paddingX = RAY_BOUNDS_PADDING + 2 * MIN_GEOMETRY_LENGTH * (bounds.maxX - bounds.minX);
        double minX = bounds.minX - paddingX;
        double maxX = bounds.maxX + paddingX;
        if (direction.x == 0) {
            if (origin.x < minX || origin.x > maxX) {
                return false;
            }
        }
        else {
            double first = (minX - origin.x) / direction.x;
            double second = (maxX - origin.x) / direction.x;
            near = Math.max(near, Math.min(first, second));
            far = Math.min(far, Math.max(first, second));
            if (near > far) {
                return false;
            }
        }

        double paddingY = RAY_BOUNDS_PADDING + 2 * MIN_GEOMETRY_LENGTH * (bounds.maxY - bounds.minY);
        double minY = bounds.minY - paddingY;
        double maxY = bounds.maxY + paddingY;
        if (direction.y == 0) {
            if (origin.y < minY || origin.y > maxY) {
                return false;
            }
        }
        else {
            double first = (minY - origin.y) / direction.y;
            double second = (maxY - origin.y) / direction.y;
            near = Math.max(near, Math.min(first, second));
            far = Math.min(far, Math.max(first, second));
            if (near > far) {
                return false;
            }
        }

        double paddingZ = RAY_BOUNDS_PADDING + 2 * MIN_GEOMETRY_LENGTH * (bounds.maxZ - bounds.minZ);
        double minZ = bounds.minZ - paddingZ;
        double maxZ = bounds.maxZ + paddingZ;
        if (direction.z == 0) {
            return origin.z >= minZ && origin.z <= maxZ;
        }

        double first = (minZ - origin.z) / direction.z;
        double second = (maxZ - origin.z) / direction.z;
        near = Math.max(near, Math.min(first, second));
        far = Math.min(far, Math.max(first, second));
        return near <= far;
    }

    static final class ShellFace {
        private final Face face;
        private final List<ShellTriangle> triangles;

        ShellFace(Face face) {
            this.face = face;
            List<Vec3> vertices = face.vertices();
            Vec3 first = vertices.getFirst();
            List<ShellTriangle> triangles = new ArrayList<>(Math.max(0, vertices.size() - 2));
            for (int index = 1; index + 1 < vertices.size(); index++) {
                triangles.add(new ShellTriangle(first, vertices.get(index), vertices.get(index + 1)));
            }

            this.triangles = List.copyOf(triangles);
        }

        private ShellFace(Face face, List<ShellTriangle> triangles) {
            this.face = face;
            this.triangles = List.copyOf(triangles);
        }

        Face face() {
            return face;
        }
    }

    private static final class ShellTriangle {
        private final List<Vec3> vertices;
        private final Vec3 firstEdge;
        private final Vec3 secondEdge;
        private final Vec3 normal;
        private final double normalLength;
        private final List<Vec3> edges;
        private final List<Vec3> outwards;
        private final double[] lengthsSqr;

        private ShellTriangle(Vec3 first, Vec3 second, Vec3 third) {
            vertices = List.of(first, second, third);
            firstEdge = second.subtract(first);
            secondEdge = third.subtract(first);
            Vec3 cross = firstEdge.cross(secondEdge);
            normalLength = cross.length();
            if (normalLength < MIN_TRIANGLE_NORMAL_LENGTH) {
                normal = Vec3.ZERO;
            }
            else {
                normal = cross.scale(1 / normalLength);
            }

            Vec3 thirdEdge = third.subtract(second);
            Vec3 closingEdge = first.subtract(third);
            edges = List.of(firstEdge, thirdEdge, closingEdge);
            outwards = List.of(firstEdge.cross(normal), thirdEdge.cross(normal), closingEdge.cross(normal));
            lengthsSqr = new double[]{firstEdge.lengthSqr(), thirdEdge.lengthSqr(), closingEdge.lengthSqr()};
        }
    }

    static final class FrameWork implements LaserBeamWork<ShellFace> {
        private final Face face;
        private final List<ShellTriangle> triangles = new ArrayList<>();
        private int triangle = 1;
        private @Nullable ShellFace result;

        FrameWork(Face face) {
            this.face = face;
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            List<Vec3> vertices = face.vertices();
            if (triangle + 1 < vertices.size()) {
                triangles.add(new ShellTriangle(vertices.getFirst(), vertices.get(triangle), vertices.get(triangle + 1)));
                triangle++;
                return false;
            }

            result = new ShellFace(face, triangles);
            return true;
        }

        @Override
        public ShellFace result() {
            return Objects.requireNonNull(result, "Sampler face preparation has not completed.");
        }
    }

    static final class IndexWork implements LaserBeamWork<LaserBeamParticleIndex> {
        private final ShellFace[] frames;
        private final ArrayDeque<IndexNode> stack = new ArrayDeque<>();
        private @Nullable LaserBeamParticleIndex result;

        IndexWork(ShellFace[] frames) {
            this.frames = frames;
            stack.addLast(new IndexNode(frames, 0, frames.length));
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            IndexNode node = Objects.requireNonNull(stack.peekLast(), "Particle sampler index construction requires a pending node on the build stack.");
            if (node.cursor < node.end) {
                ShellFace frame = frames[node.cursor++];
                AABB bounds = frame.face.bounds();
                node.bounds = node.bounds.minmax(bounds);
                node.finiteBounds &= Double.isFinite(bounds.minX) && Double.isFinite(bounds.minY) && Double.isFinite(bounds.minZ) && Double.isFinite(bounds.maxX) && Double.isFinite(bounds.maxY) && Double.isFinite(bounds.maxZ);
                return false;
            }

            List<LaserBeamParticleIndex> children = List.of();
            if (node.end - node.first > QUERY_LEAF_SIZE) {
                int middle = node.first + (node.end - node.first) / 2;
                if (node.left == null) {
                    stack.addLast(new IndexNode(frames, node.first, middle));
                    return false;
                }

                if (node.right == null) {
                    stack.addLast(new IndexNode(frames, middle, node.end));
                    return false;
                }

                children = List.of(node.left, node.right);
            }

            LaserBeamParticleIndex built = new LaserBeamParticleIndex(node.first, node.end, node.bounds, node.finiteBounds, children);
            stack.removeLast();
            IndexNode parent = stack.peekLast();
            if (parent == null) {
                result = built;
                return true;
            }

            if (parent.left == null) {
                parent.left = built;
            }
            else {
                parent.right = built;
            }

            return false;
        }

        @Override
        public LaserBeamParticleIndex result() {
            return Objects.requireNonNull(result, "Sampler index preparation has not completed.");
        }
    }

    private static final class IndexNode {
        private final int first;
        private final int end;
        private int cursor;
        private AABB bounds;
        private boolean finiteBounds = true;
        private @Nullable LaserBeamParticleIndex left;
        private @Nullable LaserBeamParticleIndex right;

        private IndexNode(ShellFace[] frames, int first, int end) {
            this.first = first;
            this.end = end;
            cursor = first;
            bounds = first < end ? frames[first].face.bounds() : new AABB(0, 0, 0, 0, 0, 0);
        }
    }

    record ShellCrossing(double distance, int direction) {}

    record ShellHit(@Nullable ShellFace frame, double distance) {}
}
