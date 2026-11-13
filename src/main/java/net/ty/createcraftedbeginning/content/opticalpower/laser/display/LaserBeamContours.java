package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplaySegment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamContours {
    static final int MAX_FACES = 4096;
    static final int MAX_VERTICES = 16384;
    static final int MAX_OPERATIONS = 65536;
    private static final int MAX_LOOP_VERTICES = 64;
    private static final double EPSILON = LaserBeamSurfaces.EPSILON;
    private static final double KEY_SCALE = 1 / EPSILON;
    private static final double NORMAL_SCALE = 1000;
    private static final double NORMAL_ALIGNMENT_THRESHOLD = 0.99999999999999;

    private LaserBeamContours() {
    }

    static Result merge(List<Face> faces, List<DisplaySegment> segments, Budget budget) {
        if (faces.isEmpty()) {
            return new Result(List.of(), true);
        }

        Vec3 anchor = faces.getFirst().vertices().getFirst();
        double extent = 1;
        for (Face face : faces) {
            extent = Math.max(extent, face.vertices().getFirst().distanceTo(anchor));
        }

        double planeScale = 1 / Math.max(1 / NORMAL_SCALE, extent * EPSILON * 4);
        List<List<Face>> groups = new ArrayList<>();
        Map<NormalKey, List<Integer>> index = new LinkedHashMap<>();
        for (Face face : faces) {
            Vec3 normal = face.normal();
            DrawSection draw = face.draw();
            DisplayKey display = new DisplayKey(draw.appearance(), draw.brightness());

            long x = Math.round(normal.x * NORMAL_SCALE);
            long y = Math.round(normal.y * NORMAL_SCALE);
            long z = Math.round(normal.z * NORMAL_SCALE);
            long distance = Math.round(face.vertices().getFirst().subtract(anchor).dot(normal) * planeScale);
            List<Integer> candidates = new ArrayList<>();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int offset = -1; offset <= 1; offset++) {
                            candidates.addAll(index.getOrDefault(new NormalKey(x + dx, y + dy, z + dz, distance + offset, display), List.of()));
                        }
                    }
                }
            }

            candidates.sort(Integer::compareTo);
            List<Face> group = null;
            for (int candidateIndex : candidates) {
                List<Face> candidate = groups.get(candidateIndex);
                if (!budget.spend(1)) {
                    return new Result(faces, false);
                }

                Face first = candidate.getFirst();
                if (compatible(first, face, segments)) {
                    group = candidate;
                    break;
                }
            }

            if (group == null) {
                group = new ArrayList<>();
                index.computeIfAbsent(new NormalKey(x, y, z, distance, display), ignored -> new ArrayList<>()).add(groups.size());
                groups.add(group);
            }

            group.add(face);
        }

        List<Face> result = new ArrayList<>();
        boolean complete = true;
        for (List<Face> group : groups) {
            List<Face> merged = boundary(group, budget);
            if (merged == null) {
                result.addAll(group);
                complete = false;
                continue;
            }

            result.addAll(merged);
        }

        return new Result(List.copyOf(result), complete);
    }

    static LaserBeamWork<Result> beginMerge(List<Face> faces, List<DisplaySegment> segments, Budget budget) {
        return new MergeWork(faces, segments, budget);
    }

    private static final class MergeWork implements LaserBeamWork<Result> {
        private final List<Face> faces;
        private final List<DisplaySegment> segments;
        private final Budget budget;
        private final List<List<Face>> groups = new ArrayList<>();
        private final Map<NormalKey, List<Integer>> index = new LinkedHashMap<>();
        private final List<Face> output = new ArrayList<>();
        private final Vec3 anchor;
        private double extent = 1;
        private double planeScale;
        private int cursor;
        private int stage;
        private boolean complete = true;
        private Result result;

        private MergeWork(List<Face> faces, List<DisplaySegment> segments, Budget budget) {
            this.faces = faces;
            this.segments = segments;
            this.budget = budget;
            anchor = faces.isEmpty() ? Vec3.ZERO : faces.getFirst().vertices().getFirst();
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            if (faces.isEmpty()) {
                result = new Result(List.of(), true);
                return true;
            }

            if (stage == 0) {
                if (cursor < faces.size()) {
                    extent = Math.max(extent, faces.get(cursor++).vertices().getFirst().distanceTo(anchor));
                    return false;
                }

                planeScale = 1 / Math.max(1 / NORMAL_SCALE, extent * EPSILON * 4);
                cursor = 0;
                stage = 1;
                return false;
            }

            if (stage == 1) {
                if (cursor < faces.size()) {
                    return group(faces.get(cursor++));
                }

                cursor = 0;
                stage = 2;
                return false;
            }

            if (cursor < groups.size()) {
                List<Face> group = groups.get(cursor++);
                List<Face> merged = boundary(group, budget);
                if (merged == null) {
                    output.addAll(group);
                    complete = false;
                }
                else {
                    output.addAll(merged);
                }

                return false;
            }

            result = new Result(List.copyOf(output), complete);
            return true;
        }

        @Override
        public Result result() {
            return Objects.requireNonNull(result, "Laser beam contour merge has not completed.");
        }

        private boolean group(Face face) {
            Vec3 normal = face.normal();
            DrawSection draw = face.draw();
            DisplayKey display = new DisplayKey(draw.appearance(), draw.brightness());

            long x = Math.round(normal.x * NORMAL_SCALE);
            long y = Math.round(normal.y * NORMAL_SCALE);
            long z = Math.round(normal.z * NORMAL_SCALE);
            long distance = Math.round(face.vertices().getFirst().subtract(anchor).dot(normal) * planeScale);
            List<Integer> candidates = new ArrayList<>();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int offset = -1; offset <= 1; offset++) {
                            candidates.addAll(index.getOrDefault(new NormalKey(x + dx, y + dy, z + dz, distance + offset, display), List.of()));
                        }
                    }
                }
            }

            candidates.sort(Integer::compareTo);
            List<Face> group = null;
            for (int candidateIndex : candidates) {
                List<Face> candidate = groups.get(candidateIndex);
                if (!budget.spend(1)) {
                    result = new Result(faces, false);
                    return true;
                }

                Face first = candidate.getFirst();
                if (compatible(first, face, segments)) {
                    group = candidate;
                    break;
                }
            }

            if (group == null) {
                group = new ArrayList<>();
                index.computeIfAbsent(new NormalKey(x, y, z, distance, display), ignored -> new ArrayList<>()).add(groups.size());
                groups.add(group);
            }

            group.add(face);
            return false;
        }
    }

    static @Nullable Face polygon(Face original, List<Vec3> vertices) {
        vertices = LaserBeamSurfaces.validPolygon(vertices);
        if (vertices.isEmpty()) {
            return null;
        }

        Vec3 first = vertices.getFirst();
        AABB bounds = new AABB(first, first);
        for (Vec3 point : vertices) {
            bounds = bounds.minmax(new AABB(point, point));
        }

        return new Face(List.copyOf(vertices), original.normal(), original.draw(), original.startCenter(), original.endCenter(), bounds);
    }

    static @Nullable Face planarPolygon(Face original, List<Vec3> vertices) {
        Face polygon = polygon(original, vertices);
        if (polygon == null) {
            return null;
        }

        vertices = polygon.vertices();
        Vec3 first = vertices.getFirst();
        Vec3 normal = polygon.normal();
        for (int index = 0; index < vertices.size(); index++) {
            Vec3 previous = vertices.get((index + vertices.size() - 1) % vertices.size());
            Vec3 point = vertices.get(index);
            Vec3 next = vertices.get((index + 1) % vertices.size());
            if (Math.abs(point.subtract(first).dot(normal)) > EPSILON || point.subtract(previous).cross(next.subtract(point)).dot(normal) < -EPSILON * EPSILON) {
                return null;
            }
        }

        return polygon;
    }

    static int submittedVertices(List<Face> faces) {
        int count = 0;
        for (Face face : faces) {
            int size = face.vertices().size();
            count += size == 4 ? 4 : (size - 2) * 4;
        }

        return count;
    }

    static double area(Face face) {
        List<Vec3> vertices = face.vertices();
        Vec3 first = vertices.getFirst();
        double area = 0;
        for (int index = 1; index + 1 < vertices.size(); index++) {
            area += vertices.get(index).subtract(first).cross(vertices.get(index + 1).subtract(first)).dot(face.normal()) * 0.5;
        }

        return area;
    }

    private static boolean compatible(Face first, Face second, List<DisplaySegment> segments) {
        Vec3 normal = first.normal();
        if (normal.dot(second.normal()) < NORMAL_ALIGNMENT_THRESHOLD) {
            return false;
        }

        DrawSection firstDraw = first.draw();
        DrawSection secondDraw = second.draw();
        if (!firstDraw.appearance().equals(secondDraw.appearance()) || !Objects.equals(firstDraw.brightness(), secondDraw.brightness())) {
            return false;
        }

        int firstSegment = firstDraw.segmentIndex();
        int secondSegment = secondDraw.segmentIndex();
        if (firstSegment != secondSegment) {
            BeamProfile firstProfile = segments.get(firstSegment).start().profile();
            BeamProfile secondProfile = segments.get(secondSegment).start().profile();
            if (firstProfile.widthSlope() != 0 || secondProfile.widthSlope() != 0 || firstProfile.radius() != secondProfile.radius()) {
                return false;
            }
        }

        List<Vec3> firstVertices = first.vertices();
        Vec3 reference = firstVertices.getFirst();
        List<Vec3> secondVertices = second.vertices();
        for (Vec3 vertex : secondVertices) {
            if (Math.abs(vertex.subtract(reference).dot(normal)) > EPSILON || Math.abs(first.fadeAt(vertex) - second.fadeAt(vertex)) > EPSILON) {
                return false;
            }
        }

        for (Vec3 vertex : firstVertices) {
            if (Math.abs(first.fadeAt(vertex) - second.fadeAt(vertex)) > EPSILON) {
                return false;
            }
        }

        Vec3 firstStart = first.startCenter();
        Vec3 firstDirection = first.endCenter().subtract(firstStart).normalize();
        Vec3 secondStart = second.startCenter();
        Vec3 secondDirection = second.endCenter().subtract(secondStart).normalize();
        double firstDistance = firstDraw.pathDistance() - firstStart.dot(firstDirection);
        double secondDistance = secondDraw.pathDistance() - secondStart.dot(secondDirection);
        boolean firstFading = firstVertices.stream().anyMatch(point -> first.fadeAt(point) < 1 - EPSILON);
        boolean secondFading = secondVertices.stream().anyMatch(point -> second.fadeAt(point) < 1 - EPSILON);
        return firstDirection.distanceToSqr(secondDirection) <= EPSILON * EPSILON && Math.abs(firstDistance - secondDistance) <= EPSILON && firstFading == secondFading;
    }

    private static @Nullable @Unmodifiable List<Face> boundary(List<Face> group, Budget budget) {
        if (group.size() == 1) {
            return List.copyOf(group);
        }

        Face reference = group.getFirst();
        List<Vec3> referenceVertices = reference.vertices();
        Vec3 anchor = referenceVertices.getFirst();
        Map<PointKey, Vec3> points = new LinkedHashMap<>();
        for (Face face : group) {
            for (Vec3 vertex : face.vertices()) {
                PointKey key = PointKey.from(vertex.subtract(anchor));
                Vec3 previous = points.putIfAbsent(key, vertex);
                if (previous != null && previous.distanceToSqr(vertex) > EPSILON * EPSILON) {
                    return null;
                }
            }
        }

        Vec3 along = reference.endCenter().subtract(reference.startCenter()).normalize();
        if (along.lengthSqr() <= EPSILON) {
            along = referenceVertices.get(1).subtract(anchor).normalize();
        }

        Vec3 across = reference.normal().cross(along).normalize();
        NavigableMap<Double, List<PointSample>> alongIndex = new TreeMap<>();
        NavigableMap<Double, List<PointSample>> acrossIndex = new TreeMap<>();
        int ordinal = 0;
        for (Entry<PointKey, Vec3> entry : points.entrySet()) {
            Vec3 point = entry.getValue();
            Vec3 offset = point.subtract(anchor);
            PointSample sample = new PointSample(ordinal++, entry.getKey(), point);
            alongIndex.computeIfAbsent(offset.dot(along), ignored -> new ArrayList<>()).add(sample);
            acrossIndex.computeIfAbsent(offset.dot(across), ignored -> new ArrayList<>()).add(sample);
        }

        Map<Edge, Boolean> edges = new LinkedHashMap<>();
        for (Face face : group) {
            List<Vec3> vertices = face.vertices();
            for (int index = 0; index < vertices.size(); index++) {
                Vec3 start = vertices.get(index);
                Vec3 end = vertices.get((index + 1) % vertices.size());
                Vec3 delta = end.subtract(start);
                double lengthSqr = delta.lengthSqr();
                TreeMap<Double, PointKey> split = new TreeMap<>();
                split.put(0.0, PointKey.from(start.subtract(anchor)));
                split.put(1.0, PointKey.from(end.subtract(anchor)));
                Vec3 projection = along;
                NavigableMap<Double, List<PointSample>> pointIndex = alongIndex;
                if (Math.abs(delta.dot(across)) < Math.abs(delta.dot(along))) {
                    projection = across;
                    pointIndex = acrossIndex;
                }

                double from = start.subtract(anchor).dot(projection);
                double to = end.subtract(anchor).dot(projection);
                List<PointSample> candidates = new ArrayList<>();
                for (List<PointSample> bucket : pointIndex.subMap(Math.min(from, to) - EPSILON, true, Math.max(from, to) + EPSILON, true).values()) {
                    candidates.addAll(bucket);
                }

                candidates.sort(Comparator.comparingInt(PointSample::ordinal));
                for (PointSample sample : candidates) {
                    if (!budget.spend(1)) {
                        return null;
                    }

                    Vec3 point = sample.point();
                    double distance = point.subtract(start).dot(delta) / lengthSqr;
                    if (distance <= 0 || distance >= 1 || start.add(delta.scale(distance)).distanceToSqr(point) > EPSILON * EPSILON) {
                        continue;
                    }

                    split.put(distance, sample.key());
                }

                PointKey previous = null;
                for (PointKey key : split.values()) {
                    if (previous == null) {
                        previous = key;
                        continue;
                    }

                    if (previous.equals(key)) {
                        continue;
                    }

                    Edge edge = new Edge(previous, key);
                    Edge reverse = new Edge(key, previous);
                    previous = key;
                    if (edges.remove(reverse) != null) {
                        continue;
                    }

                    if (edges.put(edge, true) != null) {
                        return null;
                    }
                }
            }
        }

        Map<PointKey, PointKey> next = new LinkedHashMap<>();
        for (Edge edge : edges.keySet()) {
            if (next.put(edge.start(), edge.end()) != null) {
                return null;
            }
        }

        List<Face> result = new ArrayList<>();
        while (!next.isEmpty()) {
            PointKey start = next.keySet().iterator().next();
            PointKey cursor = start;
            List<Vec3> loop = new ArrayList<>();
            do {
                if (loop.size() >= MAX_LOOP_VERTICES || cursor == null || !budget.spend(1)) {
                    return null;
                }

                loop.add(points.get(cursor));
                cursor = next.remove(cursor);
            } while (!start.equals(cursor));
            Face polygon = planarPolygon(reference, loop);
            if (polygon == null) {
                return null;
            }

            result.add(polygon);
        }

        double before = 0;
        double after = 0;
        for (Face face : group) {
            before += area(face);
        }

        for (Face face : result) {
            after += area(face);
        }

        if (Math.abs(before - after) > EPSILON * Math.max(EPSILON, before)) {
            return null;
        }

        return List.copyOf(result);
    }

    record Result(List<Face> faces, boolean complete) {}

    public static final class Budget {
        private int remaining;

        public Budget() {
            this(MAX_OPERATIONS);
        }

        Budget(int remaining) {
            this.remaining = remaining;
        }

        int remaining() {
            return remaining;
        }

        boolean isExhausted() {
            return remaining <= 0;
        }

        boolean spend(int count) {
            remaining -= count;
            return remaining >= 0;
        }
    }

    private record PointKey(long x, long y, long z) {
        private static PointKey from(Vec3 point) {
            return new PointKey(Math.round(point.x * KEY_SCALE), Math.round(point.y * KEY_SCALE), Math.round(point.z * KEY_SCALE));
        }
    }

    private record Edge(PointKey start, PointKey end) {}

    private record DisplayKey(LaserBeamAppearance appearance, @Nullable LaserBeamBrightness brightness) {}

    private record NormalKey(long x, long y, long z, long distance, DisplayKey display) {}

    private record PointSample(int ordinal, PointKey key, Vec3 point) {}
}
