package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContours.Budget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamSurfaces {
    static final double EPSILON = 1.0E-7;
    private static final double PLANE_PRECISION = 1000;
    private static final double PLANE_TOLERANCE = 1.0E-5;
    private static final int MAX_FRAGMENTS = 32;
    private static final Comparator<VertexKey> VERTEX_ORDER = Comparator.comparingLong(VertexKey::x).thenComparingLong(VertexKey::y).thenComparingLong(VertexKey::z);

    private LaserBeamSurfaces() {
    }

    public static List<Face> create(List<DrawSection> sections) {
        return create(sections, new Budget());
    }

    public static List<Face> create(List<DrawSection> sections, Budget budget) {
        return build(sections, budget).faces();
    }

    static SurfaceBuild resolve(List<Face> faces, Budget budget) {
        if (faces.isEmpty()) {
            return new SurfaceBuild(List.of(), SurfaceReason.EMPTY);
        }

        if (!budget.spend(faces.size())) {
            return new SurfaceBuild(List.of(), SurfaceReason.OPERATION_BUDGET);
        }

        Vec3 anchor = faces.getFirst().vertices().getFirst();
        Map<PlaneKey, List<Face>> planes = new LinkedHashMap<>();
        for (Face face : faces) {
            add(planes, anchor, face.vertices(), face.draw(), face.startCenter(), face.endCenter());
        }
        return resolvePlanes(planes, anchor, budget);
    }

    static SurfaceBuild build(List<DrawSection> sections, Budget budget) {
        if (sections.isEmpty()) {
            return new SurfaceBuild(List.of(), SurfaceReason.EMPTY);
        }

        if (!budget.spend(sections.size())) {
            return new SurfaceBuild(List.of(), SurfaceReason.OPERATION_BUDGET);
        }

        sections = sections.stream().flatMap(draw -> LaserBeamMesh.split(draw).stream()).toList();
        Vec3 anchor = Vec3.ZERO;
        for (DrawSection draw : sections) {
            List<Vec3> cap = draw.geometry().getStartCap();
            if (cap.isEmpty()) {
                continue;
            }

            anchor = cap.getFirst();
            break;
        }
        Map<PlaneKey, List<Face>> planes = new LinkedHashMap<>();
        for (DrawSection draw : sections) {
            Section geometry = draw.geometry();
            double referenceWidth = draw.getReferenceWidth();
            List<List<Vec3>> sides = geometry.createSides(referenceWidth);
            if (sides.isEmpty()) {
                List<Vec3> cap = geometry.getStartCap();
                if (cap.isEmpty()) {
                    continue;
                }

                Vec3 center = Section.center(cap);
                Section capped = new Section(cap, cap, false);
                DrawSection cappedDraw = new DrawSection(capped, draw.appearance(), draw.segmentIndex(), draw.pathDistance(), null, draw.samplingDomain(), false, draw.brightness(), draw.mapped());
                add(planes, anchor, cap, cappedDraw, center, center);
                continue;
            }

            List<Vec3> starts = geometry.starts();
            List<Vec3> ends = geometry.ends();
            Vec3 startCenter = Section.center(starts);
            Vec3 endCenter = Section.center(ends);
            LaserBeamFade fade = draw.fade();
            for (List<Vec3> side : sides) {
                if (fade == null || !geometry.fadesAtEnd()) {
                    add(planes, anchor, side, draw, startCenter, endCenter);
                    continue;
                }

                Vec3 cutCenter = fade.start();
                Vec3 direction = fade.direction();
                add(planes, anchor, clip(side, cutCenter, direction, true), draw, startCenter, endCenter);
                add(planes, anchor, clip(side, cutCenter, direction, false), draw, startCenter, endCenter);
            }
        }
        SurfaceBuild resolved = resolvePlanes(planes, anchor, budget);
        if (resolved.isComplete()) {
            return resolved;
        }

        List<Face> caps = new ArrayList<>();
        int vertices = 0;
        for (DrawSection draw : sections) {
            double pathDistance = draw.pathDistance();
            if (pathDistance > EPSILON) {
                continue;
            }

            List<Vec3> cap = draw.geometry().getStartCap();
            if (cap.isEmpty()) {
                continue;
            }

            Vec3 center = Section.center(cap);
            Section capped = new Section(cap, cap, false);
            DrawSection cappedDraw = new DrawSection(capped, draw.appearance(), draw.segmentIndex(), pathDistance, null, draw.samplingDomain(), false, draw.brightness(), null);
            Map<PlaneKey, List<Face>> fallback = new LinkedHashMap<>();
            add(fallback, anchor, cap, cappedDraw, center, center);
            for (List<Face> group : fallback.values()) {
                for (Face face : group) {
                    int submitted = LaserBeamContours.submittedVertices(List.of(face));
                    if (caps.size() + 1 > LaserBeamContours.MAX_FACES || vertices + submitted > LaserBeamContours.MAX_VERTICES) {
                        continue;
                    }

                    caps.add(face);
                    vertices += submitted;
                }
            }
        }
        return new SurfaceBuild(List.copyOf(caps), resolved.reason());
    }

    static List<Vec3> validPolygon(List<Vec3> vertices) {
        ArrayList<Vec3> result = new ArrayList<>(vertices);
        if (!compact(result)) {
            return List.of();
        }

        return List.copyOf(result);
    }

    static @Unmodifiable List<Vec3> clip(List<Vec3> polygon, Vec3 origin, Vec3 normal, boolean inside) {
        ArrayList<Vec3> result = new ArrayList<>(polygon.size() + 1);
        clip(polygon, origin, normal, inside, result);
        return List.copyOf(result);
    }

    private static void clip(List<Vec3> polygon, Vec3 origin, Vec3 normal, boolean inside, ArrayList<Vec3> result) {
        result.clear();
        if (polygon.isEmpty()) {
            return;
        }

        Vec3 first = polygon.getFirst();
        double firstDistance = (first.x - origin.x) * normal.x + (first.y - origin.y) * normal.y + (first.z - origin.z) * normal.z;
        if (!inside) {
            firstDistance = -firstDistance;
        }
        double currentDistance = firstDistance;
        for (int index = 0; index < polygon.size(); index++) {
            Vec3 current = polygon.get(index);
            Vec3 next = polygon.get((index + 1) % polygon.size());
            double nextDistance = firstDistance;
            if (index + 1 < polygon.size()) {
                nextDistance = (next.x - origin.x) * normal.x + (next.y - origin.y) * normal.y + (next.z - origin.z) * normal.z;
                if (!inside) {
                    nextDistance = -nextDistance;
                }
            }
            boolean currentIn = currentDistance < -EPSILON;
            boolean nextIn = nextDistance < -EPSILON;
            if (currentDistance <= EPSILON) {
                result.add(current);
            }
            if (currentIn == nextIn || Math.abs(currentDistance - nextDistance) <= EPSILON) {
                currentDistance = nextDistance;
                continue;
            }

            result.add(current.lerp(next, Math.clamp(currentDistance / (currentDistance - nextDistance), 0, 1)));
            currentDistance = nextDistance;
        }
        if (compact(result)) {
            return;
        }

        result.clear();
    }

    private static SurfaceBuild resolvePlanes(Map<PlaneKey, List<Face>> planes, Vec3 anchor, Budget budget) {
        Map<List<VertexKey>, Face> paired = new LinkedHashMap<>();
        Set<Face> internal = new HashSet<>();
        for (List<Face> group : planes.values()) {
            for (Face face : group) {
                List<Vec3> vertices = face.vertices();
                List<VertexKey> key = new ArrayList<>(vertices.size());
                for (Vec3 point : vertices) {
                    key.add(new VertexKey(Math.round((point.x - anchor.x) / EPSILON), Math.round((point.y - anchor.y) / EPSILON), Math.round((point.z - anchor.z) / EPSILON)));
                }
                key.sort(VERTEX_ORDER);
                Face other = paired.get(key);
                if (other == null || face.normal().dot(other.normal()) > -1 + EPSILON || !face.draw().appearance().equals(other.draw().appearance()) || vertices.stream().anyMatch(point -> Math.abs(face.fadeAt(point) - other.fadeAt(point)) > EPSILON)) {
                    paired.put(key, face);
                    continue;
                }

                internal.add(face);
                internal.add(other);
                paired.remove(key);
            }
        }
        for (List<Face> group : planes.values()) {
            group.removeIf(internal::contains);
        }
        SurfaceBuild resolved = removeOverlaps(planes, budget);
        if (!resolved.isComplete()) {
            return resolved;
        }

        List<Face> result = resolved.faces();
        if (result.size() > LaserBeamContours.MAX_FACES) {
            return new SurfaceBuild(List.of(), SurfaceReason.FACE_LIMIT);
        }

        if (LaserBeamContours.submittedVertices(result) > LaserBeamContours.MAX_VERTICES) {
            return new SurfaceBuild(List.of(), SurfaceReason.VERTEX_LIMIT);
        }

        return resolved;
    }

    private static SurfaceBuild removeOverlaps(Map<PlaneKey, List<Face>> planes, Budget budget) {
        List<Face> result = new ArrayList<>();
        ClipWorkspace workspace = new ClipWorkspace();
        List<List<Vec3>> fragments = new ArrayList<>();
        List<List<Vec3>> remaining = new ArrayList<>();
        PlaneLookup lookup = new PlaneLookup(planes);
        for (Entry<PlaneKey, List<Face>> entry : planes.entrySet()) {
            PlaneKey key = entry.getKey();
            List<Face> candidates = new ArrayList<>();
            PlaneGroup source = Objects.requireNonNull(lookup.get(key.x(), key.y(), key.z()), "Laser beam surface resolution requires a plane group for the current face normal.");
            for (PlaneGroup group : source.neighbors) {
                for (int distance = -1; distance <= 1; distance++) {
                    candidates.addAll(group.faces.getOrDefault(-key.distance() + distance, List.of()));
                }
            }
            for (Face face : entry.getValue()) {
                fragments.clear();
                List<Vec3> faceVertices = face.vertices();
                fragments.add(faceVertices);
                AABB faceBounds = face.bounds();
                AABB bounds = faceBounds.inflate(EPSILON);
                Vec3 normal = face.normal();
                DrawSection draw = face.draw();
                for (Face other : candidates) {
                    if (!budget.spend(1)) {
                        return new SurfaceBuild(List.of(), SurfaceReason.OPERATION_BUDGET);
                    }

                    Vec3 otherNormal = other.normal();
                    double alignment = normal.dot(otherNormal);
                    if (face == other || !bounds.intersects(other.bounds()) || alignment > -1 + EPSILON || !draw.appearance().equals(other.draw().appearance())) {
                        continue;
                    }

                    Vec3 reference = faceVertices.getFirst();
                    boolean coplanar = true;
                    List<Vec3> otherVertices = other.vertices();
                    for (Vec3 vertex : otherVertices) {
                        if (Math.abs(vertex.subtract(reference).dot(normal)) > PLANE_TOLERANCE || Math.abs(face.fadeAt(vertex) - other.fadeAt(vertex)) > EPSILON) {
                            coplanar = false;
                            break;
                        }
                    }
                    if (!coplanar) {
                        continue;
                    }

                    if (fragments.size() > MAX_FRAGMENTS) {
                        return new SurfaceBuild(List.of(), SurfaceReason.FRAGMENT_LIMIT);
                    }

                    remaining.clear();
                    for (List<Vec3> polygon : fragments) {
                        if (!budget.spend(1)) {
                            return new SurfaceBuild(List.of(), SurfaceReason.OPERATION_BUDGET);
                        }

                        subtract(polygon, otherVertices, otherNormal, workspace, remaining);
                    }
                    List<List<Vec3>> previous = fragments;
                    fragments = remaining;
                    remaining = previous;
                    if (fragments.isEmpty()) {
                        break;
                    }
                }
                for (List<Vec3> polygon : fragments) {
                    result.add(new Face(List.copyOf(polygon), normal, draw, face.startCenter(), face.endCenter(), faceBounds));
                }
            }
        }
        return new SurfaceBuild(List.copyOf(result), SurfaceReason.COMPLETE);
    }

    private static void add(Map<PlaneKey, List<Face>> planes, Vec3 anchor, List<Vec3> vertices, DrawSection draw, Vec3 startCenter, Vec3 endCenter) {
        vertices = validPolygon(vertices);
        if (vertices.size() < 3) {
            return;
        }

        Vec3 first = vertices.getFirst();
        Vec3 normal = Vec3.ZERO;
        double normalLengthSqr = 0;
        for (int index = 1; index + 1 < vertices.size(); index++) {
            normal = vertices.get(index).subtract(first).cross(vertices.get(index + 1).subtract(first));
            normalLengthSqr = normal.lengthSqr();
            if (normalLengthSqr >= EPSILON * EPSILON * EPSILON * EPSILON) {
                break;
            }
        }
        if (normalLengthSqr < EPSILON * EPSILON * EPSILON * EPSILON) {
            return;
        }

        normal = normal.scale(1 / normal.length());
        Vec3 middle = Vec3.ZERO;
        for (Vec3 vertex : vertices) {
            middle = middle.add(vertex);
        }
        middle = middle.scale(1.0 / vertices.size());
        Vec3 direction = endCenter.subtract(startCenter).normalize();
        Vec3 outward = middle.subtract(startCenter);
        outward = outward.subtract(direction.scale(outward.dot(direction)));
        if (normal.dot(outward) < 0) {
            vertices = new ArrayList<>(vertices);
            Collections.reverse(vertices);
            normal = normal.scale(-1);
        }
        AABB bounds = new AABB(first, first);
        for (Vec3 point : vertices) {
            bounds = bounds.minmax(new AABB(point, point));
        }
        Face face = new Face(vertices, normal, draw, startCenter, endCenter, bounds);
        PlaneKey key = new PlaneKey(Math.round(normal.x * PLANE_PRECISION), Math.round(normal.y * PLANE_PRECISION), Math.round(normal.z * PLANE_PRECISION), Math.round(first.subtract(anchor).dot(normal) * PLANE_PRECISION));
        planes.computeIfAbsent(key, ignored -> new ArrayList<>()).add(face);
    }

    private static void subtract(List<Vec3> polygon, List<Vec3> cutter, Vec3 normal, ClipWorkspace workspace, List<List<Vec3>> outside) {
        List<Vec3> overlap = polygon;
        List<ArrayList<Vec3>> intersections = workspace.intersections;
        List<Vec3> outwards = workspace.outwards;
        outwards.clear();
        for (int edge = 0; edge < cutter.size(); edge++) {
            Vec3 a = cutter.get(edge);
            Vec3 b = cutter.get((edge + 1) % cutter.size());
            Vec3 boundary = b.subtract(a);
            Vec3 outward = boundary.cross(normal).scale(1 / boundary.length());
            outwards.add(outward);
            if (edge >= intersections.size()) {
                intersections.add(new ArrayList<>(polygon.size() + 1));
            }
            ArrayList<Vec3> clipped = intersections.get(edge);
            clip(overlap, a, outward, true, clipped);
            if (clipped.size() < 3) {
                outside.add(polygon);
                return;
            }

            overlap = clipped;
        }
        List<Vec3> input = polygon;
        ArrayList<Vec3> remainder = workspace.remainder;
        for (int edge = 0; edge < cutter.size(); edge++) {
            clip(input, cutter.get(edge), outwards.get(edge), false, remainder);
            input = intersections.get(edge);
            if (remainder.size() < 3) {
                continue;
            }

            outside.add(List.copyOf(remainder));
        }
    }

    private static boolean compact(ArrayList<Vec3> result) {
        for (Vec3 point : result) {
            if (!Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z)) {
                return false;
            }
        }
        int retained = 0;
        for (int index = 0; index < result.size(); index++) {
            Vec3 point = result.get(index);
            if (retained > 0 && result.get(retained - 1).distanceToSqr(point) <= EPSILON * EPSILON) {
                continue;
            }

            result.set(retained++, point);
        }
        while (result.size() > retained) {
            result.removeLast();
        }
        if (result.size() > 1 && result.getFirst().distanceToSqr(result.getLast()) <= EPSILON * EPSILON) {
            result.removeLast();
        }
        int count = result.size();
        if (count < 3) {
            return false;
        }

        Vec3 previous = result.getLast();
        int end = count;
        int readIndex = 0;
        retained = 0;
        while (readIndex < end && count >= 3) {
            Vec3 point = result.get(readIndex);
            Vec3 next = result.getFirst();
            if (readIndex + 1 < end) {
                next = result.get(readIndex + 1);
            }
            double edgeX = next.x - previous.x;
            double edgeY = next.y - previous.y;
            double edgeZ = next.z - previous.z;
            double lengthSqr = edgeX * edgeX + edgeY * edgeY + edgeZ * edgeZ;
            boolean keep = lengthSqr <= EPSILON * EPSILON;
            if (!keep) {
                double along = Math.clamp(((point.x - previous.x) * edgeX + (point.y - previous.y) * edgeY + (point.z - previous.z) * edgeZ) / lengthSqr, 0, 1);
                double distanceX = previous.x + edgeX * along - point.x;
                double distanceY = previous.y + edgeY * along - point.y;
                double distanceZ = previous.z + edgeZ * along - point.z;
                keep = distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ > EPSILON * EPSILON;
            }
            if (!keep) {
                count--;
                readIndex++;
                continue;
            }

            if (retained < readIndex) {
                result.set(retained, point);
            }
            retained++;
            previous = point;
            readIndex++;
        }
        while (readIndex < end) {
            result.set(retained++, result.get(readIndex++));
        }
        while (result.size() > retained) {
            result.removeLast();
        }
        if (count < 3) {
            return false;
        }

        Vec3 first = result.getFirst();
        Vec3 second = result.get(1);
        double firstX = second.x - first.x;
        double firstY = second.y - first.y;
        double firstZ = second.z - first.z;
        double area = 0;
        for (int index = 1; index + 1 < count; index++) {
            Vec3 next = result.get(index + 1);
            double nextX = next.x - first.x;
            double nextY = next.y - first.y;
            double nextZ = next.z - first.z;
            double crossX = firstY * nextZ - firstZ * nextY;
            double crossY = firstZ * nextX - firstX * nextZ;
            double crossZ = firstX * nextY - firstY * nextX;
            area += Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
            firstX = nextX;
            firstY = nextY;
            firstZ = nextZ;
        }
        return Double.isFinite(area) && area > EPSILON * EPSILON;
    }

    public enum SurfaceReason {
        COMPLETE,
        EMPTY,
        OPERATION_BUDGET,
        FRAGMENT_LIMIT,
        FACE_LIMIT,
        VERTEX_LIMIT
    }

    record SurfaceBuild(List<Face> faces, SurfaceReason reason) {
        boolean isComplete() {
            return reason == SurfaceReason.COMPLETE || reason == SurfaceReason.EMPTY;
        }
    }

    public record Face(List<Vec3> vertices, Vec3 normal, DrawSection draw, Vec3 startCenter, Vec3 endCenter, AABB bounds) {
        public Face {
            vertices = List.copyOf(vertices);
        }

        public double distanceTo(Vec3 point) {
            Vec3 first = vertices.getFirst();
            double distance = point.subtract(first).dot(normal);
            Vec3 projected = point.subtract(normal.scale(distance));
            boolean inside = true;
            double edgeDistance = Double.POSITIVE_INFINITY;
            for (int index = 0; index < vertices.size(); index++) {
                Vec3 a = vertices.get(index);
                Vec3 b = vertices.get((index + 1) % vertices.size());
                Vec3 edge = b.subtract(a);
                double lengthSqr = edge.lengthSqr();
                if (projected.subtract(a).dot(edge.cross(normal)) > EPSILON * Math.sqrt(lengthSqr)) {
                    inside = false;
                }
                if (lengthSqr <= EPSILON * EPSILON) {
                    continue;
                }

                Vec3 nearest = a.add(edge.scale(Math.clamp(point.subtract(a).dot(edge) / lengthSqr, 0, 1)));
                edgeDistance = Math.min(edgeDistance, point.distanceTo(nearest));
            }
            if (!inside) {
                return edgeDistance;
            }

            return Math.abs(distance);
        }

        public double fadeAt(Vec3 point) {
            return draw.fadeAt(point);
        }
    }

    private static final class ClipWorkspace {
        private final List<ArrayList<Vec3>> intersections = new ArrayList<>();
        private final List<Vec3> outwards = new ArrayList<>();
        private final ArrayList<Vec3> remainder = new ArrayList<>();
    }

    private record VertexKey(long x, long y, long z) {}

    private static final class PlaneLookup {
        private static final long X_HASH_MULTIPLIER = 0x9E3779B97F4A7C15L;
        private static final long Y_HASH_MULTIPLIER = 0xC2B2AE3D27D4EB4FL;
        private static final long Z_HASH_MULTIPLIER = 0x165667B19E3779F9L;
        private static final float LOAD_FACTOR = 0.5F;
        private final @Nullable PlaneGroup[] slots;
        private final int mask;

        private PlaneLookup(Map<PlaneKey, List<Face>> planes) {
            int capacity = HashCommon.arraySize(planes.size(), LOAD_FACTOR);
            slots = new PlaneGroup[capacity];
            mask = capacity - 1;
            for (Entry<PlaneKey, List<Face>> entry : planes.entrySet()) {
                PlaneKey key = entry.getKey();
                long keyX = key.x();
                long keyY = key.y();
                long keyZ = key.z();
                int slot = hash(keyX, keyY, keyZ) & mask;
                PlaneGroup group = slots[slot];
                while (group != null && (group.key.x() != keyX || group.key.y() != keyY || group.key.z() != keyZ)) {
                    slot = slot + 1 & mask;
                    group = slots[slot];
                }
                if (group == null) {
                    group = new PlaneGroup(key);
                    slots[slot] = group;
                }
                group.faces.put(key.distance(), entry.getValue());
            }
            for (PlaneGroup group : slots) {
                if (group == null) {
                    continue;
                }

                PlaneKey key = group.key;
                long keyX = key.x();
                long keyY = key.y();
                long keyZ = key.z();
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            PlaneGroup neighbor = get(-keyX + x, -keyY + y, -keyZ + z);
                            if (neighbor == null) {
                                continue;
                            }

                            group.neighbors.add(neighbor);
                        }
                    }
                }
            }
        }

        private static int hash(long x, long y, long z) {
            return (int) HashCommon.mix(x * X_HASH_MULTIPLIER ^ y * Y_HASH_MULTIPLIER ^ z * Z_HASH_MULTIPLIER);
        }

        private @Nullable PlaneGroup get(long x, long y, long z) {
            int slot = hash(x, y, z) & mask;
            while (true) {
                PlaneGroup group = slots[slot];
                if (group == null) {
                    return null;
                }

                PlaneKey key = group.key;
                if (key.x() == x && key.y() == y && key.z() == z) {
                    return group;
                }

                slot = slot + 1 & mask;
            }
        }
    }

    private static final class PlaneGroup {
        private final PlaneKey key;
        private final Long2ObjectOpenHashMap<List<Face>> faces = new Long2ObjectOpenHashMap<>();
        private final List<PlaneGroup> neighbors = new ArrayList<>();

        private PlaneGroup(PlaneKey key) {
            this.key = key;
        }
    }

    private record PlaneKey(long x, long y, long z, long distance) {}
}
