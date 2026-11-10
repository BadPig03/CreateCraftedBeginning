package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.Tile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMappedGeometry.FamilyGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamTerminalField.Ray;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserDisplayFamilies.Family;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamPolygonDisplay {
    private static final double SURFACE_GATE = 1.0E-4;
    private static final double FOCUS_GATE = 1.0E-5;
    private static final double CONTACT_GATE = 1.0E-5;
    private static final double MIN_REGION_TRIANGLE_AREA = 1.0E-20;
    private static final double DOMAIN_AREA_TOLERANCE = 1.0E-12;
    private static final double FOCUS_EVENT_TOLERANCE = 1.0E-10;
    private static final double MIN_SCALAR_SUPPORT_DETERMINANT = 1.0E-24;
    private static final double MIN_BRIGHTNESS_GRADIENT = 1.0E-14;
    private static final double EVENT_SIGN_TOLERANCE = 1.0E-7;
    private static final double PLANAR_SURFACE_TOLERANCE = 1.0E-12;
    private static final double EXTERIOR_LENGTH_TOLERANCE = 1.0E-10;
    private static final double BARYCENTRIC_TOLERANCE = 1.0E-12;
    private static final double BARYCENTRIC_MAX_SUM = 1.000000000001;
    private static final double MIN_REFINEMENT_EDGE_SHARE = 0.25;
    private static final int FOCUS_ROOT_ITERATIONS = 56;
    private static final int MAX_SOURCE_TRIANGLES = 512;
    private static final int MAX_REFINEMENT_DEPTH = 7;
    private static final int REFINEMENT_SAMPLE_INTERVALS = 4;
    private static final int VALIDATION_SAMPLE_INTERVALS = 16;
    private final LaserBeamSourceDomains topology;
    private final LaserBeamTerminalField terminalField;
    private final Map<Point, PathSample> paths = new HashMap<>();

    private LaserBeamPolygonDisplay(LaserBeamSourceDomains topology, LaserBeamTerminalField terminalField) {
        this.topology = topology;
        this.terminalField = terminalField;
    }

    static LaserBeamWork<FamilyGeometry> begin(Family family) {
        return new Build(family);
    }

    static FamilyGeometry create(Family family) {
        LaserBeamWork<FamilyGeometry> work = begin(family);
        while (!work.step()) {
        }
        return work.result();
    }

    private static @Unmodifiable List<Point> support(Triangle t) {
        List<Point> p = new ArrayList<>();
        List<Point> vertices = t.vertices();
        for (int i = 0; i < 3; i++) {
            Point a = vertices.get(i);
            Point b = vertices.get((i + 1) % 3);
            p.add(a);
            p.add(a.lerp(b, 0.5));
        }

        return List.copyOf(p);
    }

    private static double segmentDistance(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        double lengthSqr = d.lengthSqr();
        if (lengthSqr <= 0) {
            return p.distanceTo(a);
        }

        return p.distanceTo(a.add(d.scale(Math.clamp(p.subtract(a).dot(d) / lengthSqr, 0, 1))));
    }

    private static Vec3 point(PathSample sample, Patch patch, double t) {
        Vec3 a = sample.start();
        Ray path = sample.path();
        Vec3 b = path.terminalPosition();
        if (patch.crossing()) {
            Vec3 focus = sample.focus();
            if (focus == null) {
                focus = LaserBeamTerminalField.idealPoint(path, path.focusDistance());
            }

            if (patch.branch() == 0) {
                b = focus;
            }
            else {
                a = focus;
            }
        }

        return a.lerp(b, t);
    }

    private static double distance(Ray path, Patch patch, double t) {
        double a = 0;
        double b = path.terminalDistance();
        if (patch.crossing()) {
            if (patch.branch() == 1) {
                a = path.focusDistance();
            }
            else {
                b = path.focusDistance();
            }
        }

        return a + (b - a) * t;
    }

    private Ray path(Point uv) {
        return sample(uv).path();
    }

    private PathSample sample(Point uv) {
        return paths.computeIfAbsent(uv, point -> {
            Ray ray = terminalField.path(point);
            Vec3 start = LaserBeamTerminalField.idealPoint(ray, 0);
            Vec3 focus = ray.hasFocus() ? LaserBeamTerminalField.idealPoint(ray, ray.focusDistance()) : null;
            return new PathSample(ray, start, focus);
        });
    }

    private Vec3 point(Point uv, Patch patch, double t) {
        return point(sample(uv), patch, t);
    }

    private @Unmodifiable List<Vec3> cap(Patch patch, double t) {
        return support(patch.triangle()).stream().map(p -> point(p, patch, t)).toList();
    }

    private static final class Build implements LaserBeamWork<FamilyGeometry> {
        private final Family family;
        private final LaserBeamSourceDomains topology;
        private final LaserBeamWork<LaserBeamTerminalField> terminal;
        private @Nullable LaserBeamPolygonDisplay owner;
        private List<Triangle> triangles = List.of();
        private List<RegionTriangle> regions = List.of();
        private List<RegionTriangle> clipped = new ArrayList<>();
        private List<Patch> patches = new ArrayList<>();
        private final Set<Triangle> failed = new HashSet<>();
        private final Map<List<Point>, Point> eventRoots = new HashMap<>();
        private final Map<Triangle, List<RegionTriangle>> focusRegions = new HashMap<>();
        private final Map<Triangle, List<RegionTriangle>> regionsByTriangle = new HashMap<>();
        private final Map<Patch, CheckedPatch> checkedPatches = new HashMap<>();
        private final Set<List<Point>> marked = new HashSet<>();
        private Iterator<Triangle> failing = Collections.emptyIterator();
        private List<Triangle> next = new ArrayList<>();
        private final List<Tile> tiles = new ArrayList<>();
        private @Nullable Patch patch;
        private @Nullable ErrorWork error;
        private @Nullable FamilyGeometry result;
        private int stage;
        private int index;
        private int branch;
        private int depth;
        private boolean regionActive;
        private double priorViolation;
        private double geometryError;
        private double exteriorLength;
        private double eventSignViolation;
        private double worstMeasured;
        private @Nullable WorstSample worstContext;

        private Build(Family family) {
            this.family = family;
            topology = new LaserBeamSourceDomains(family.sourceDomains());
            terminal = LaserBeamTerminalField.begin(family, topology);
        }

        private static double area(Triangle t) {
            Point a = t.a();
            Point b = t.b();
            Point c = t.c();
            double originU = a.u();
            double originV = a.v();
            return Math.abs((b.u() - originU) * (c.v() - originV) - (b.v() - originV) * (c.u() - originU)) * 0.5;
        }

        private static @Unmodifiable List<Point> edgeKey(Point a, Point b) {
            if (a.u() < b.u() || a.u() == b.u() && a.v() < b.v()) {
                return List.of(a, b);
            }

            return List.of(b, a);
        }

        private static @Unmodifiable List<Triangle> refine(Triangle t, Set<List<Point>> marked) {
            List<Point> v = t.vertices();
            boolean[] split = new boolean[3];
            Point[] m = new Point[3];
            int count = 0;
            for (int i = 0; i < 3; i++) {
                split[i] = marked.contains(edgeKey(v.get(i), v.get((i + 1) % 3)));
                if (!split[i]) {
                    continue;
                }

                count++;
                m[i] = v.get(i).lerp(v.get((i + 1) % 3), 0.5);
            }

            if (count == 0) {
                return List.of(t);
            }

            if (count == 1) {
                int i = 2;
                if (split[0]) {
                    i = 0;
                }
                else if (split[1]) {
                    i = 1;
                }

                Point a = v.get(i);
                Point b = v.get((i + 1) % 3);
                Point c = v.get((i + 2) % 3);
                return List.of(new Triangle(a, m[i], c), new Triangle(m[i], b, c));
            }

            if (count == 3) {
                return List.of(new Triangle(v.get(0), m[0], m[2]), new Triangle(m[0], v.get(1), m[1]), new Triangle(m[2], m[1], v.get(2)), new Triangle(m[0], m[1], m[2]));
            }

            int unsplit = 2;
            if (!split[0]) {
                unsplit = 0;
            }
            else if (!split[1]) {
                unsplit = 1;
            }

            int i = (unsplit + 1) % 3;
            int j = (unsplit + 2) % 3;
            Point a = v.get(unsplit);
            Point b = v.get(i);
            Point c = v.get(j);
            return List.of(new Triangle(c, m[j], m[i]), new Triangle(a, b, m[i]), new Triangle(a, m[i], m[j]));
        }

        private static double event(LaserBeamPolygonDisplay display, Point p) {
            double h = display.path(p).focusEvent();
            if (Math.abs(h) <= FOCUS_EVENT_TOLERANCE) {
                return 0;
            }

            return h;
        }

        @Override
        public boolean step() {
            if (stage == 11) {
                return true;
            }

            if (stage == 0) {
                if (!terminal.step()) {
                    return false;
                }

                owner = new LaserBeamPolygonDisplay(topology, terminal.result());
                triangles = initialTriangles();
                stage = 1;
                return false;
            }

            LaserBeamPolygonDisplay display = Objects.requireNonNull(owner, "Polygon display must be initialized before tessellation starts.");
            if (stage == 1) {
                if (triangles.size() > MAX_SOURCE_TRIANGLES) {
                    throw new UnsupportedOperationException("Mapped polygon display exceeds the 512-source-triangle limit.");
                }

                clipped = new ArrayList<>();
                regionsByTriangle.clear();
                eventSignViolation = 0;
                index = 0;
                stage = 2;
                return false;
            }

            if (stage == 2) {
                if (index < triangles.size()) {
                    List<RegionTriangle> split = focusRegions.computeIfAbsent(triangles.get(index++), triangle -> splitFocus(display, triangle));
                    clipped.addAll(split);
                    for (RegionTriangle region : split) {
                        regionsByTriangle.computeIfAbsent(region.triangle(), ignored -> new ArrayList<>()).add(region);
                    }
                    return false;
                }

                regions = List.copyOf(clipped);
                if (regions.size() > MAX_SOURCE_TRIANGLES) {
                    throw new UnsupportedOperationException("Mapped focus-event clipping exceeds the 512-source-triangle limit; source=" + triangles.size() + ", clipped=" + regions.size() + ", depth=" + depth + '.');
                }

                patches = new ArrayList<>();
                failed.clear();
                geometryError = 0;
                worstMeasured = 0;
                worstContext = null;
                index = 0;
                regionActive = false;
                stage = 3;
                return false;
            }

            if (stage == 3) {
                if (index >= regions.size()) {
                    if (failed.isEmpty()) {
                        index = 0;
                        exteriorLength = 0;
                        stage = 9;
                        return false;
                    }

                    if (++depth > MAX_REFINEMENT_DEPTH) {
                        WorstSample worst = worstContext;
                        throw new UnsupportedOperationException("Mapped event/geometry refinement exhausted the seven levels without passing surface/focus limits; triangles=" + triangles.size() + ", regions=" + regions.size() + ", error=" + geometryError + ", event=" + eventSignViolation + ", worst=" + (worst == null ? "" : worst.describe()) + '.');
                    }

                    marked.clear();
                    failing = failed.iterator();
                    stage = 7;
                    return false;
                }

                RegionTriangle region = regions.get(index);
                if (!regionActive) {
                    priorViolation = eventSignViolation;
                    eventSignViolation = 0;
                    branch = 0;
                    regionActive = true;
                }

                boolean after = region.after();
                if (branch >= (after ? 2 : 1)) {
                    if (eventSignViolation > EVENT_SIGN_TOLERANCE) {
                        failed.add(region.triangle());
                    }

                    eventSignViolation = Math.max(eventSignViolation, priorViolation);
                    index++;
                    regionActive = false;
                    return false;
                }

                Triangle triangle = region.triangle();
                Patch key = new Patch(triangle, branch, after, null);
                CheckedPatch retained = checkedPatches.get(key);
                if (retained != null) {
                    accept(retained);
                    return false;
                }

                PatchGeometry geometry = new PatchGeometry(display, key);
                patch = new Patch(triangle, branch, after, geometry.field(display));
                error = new ErrorWork(display, patch, geometry, REFINEMENT_SAMPLE_INTERVALS);
                stage = 4;
                return false;
            }

            if (stage == 4 || stage == 5) {
                ErrorWork check = Objects.requireNonNull(error, "Polygon patch validation requires an active geometry error check.");
                if (!check.step()) {
                    return false;
                }

                GeometryError measured = check.result();
                if (stage == 4 && measured.passes()) {
                    error = new ErrorWork(display, Objects.requireNonNull(patch, "Polygon patch validation requires a patch to sample."), check.input, VALIDATION_SAMPLE_INTERVALS);
                    stage = 5;
                    return false;
                }

                PatchGeometry input = check.input;
                CheckedPatch completed = new CheckedPatch(Objects.requireNonNull(patch, "Recording polygon validation results requires the checked patch."), measured, input.eventSignViolation, input.worstMeasured, input.worstContext);
                checkedPatches.put(input.patch, completed);
                accept(completed);
                patch = null;
                error = null;
                return false;
            }

            if (stage == 7) {
                if (failing.hasNext()) {
                    Triangle triangle = failing.next();
                    List<Point> vertices = triangle.vertices();
                    List<Point> edge = null;
                    double length = -1;
                    double[] deviations = new double[3];
                    double maximumDeviation = 0;
                    for (int i = 0; i < 3; i++) {
                        Point a = vertices.get(i);
                        Point b = vertices.get((i + 1) % 3);
                        double distance = Math.hypot(a.u() - b.u(), a.v() - b.v());
                        if (distance > length) {
                            length = distance;
                            edge = edgeKey(a, b);
                        }

                        for (RegionTriangle region : regionsByTriangle.get(triangle)) {
                            for (int current = 0; current < (region.after() ? 2 : 1); current++) {
                                deviations[i] = Math.max(deviations[i], edgeDeviation(display, triangle, region.after(), current, a, b));
                            }
                        }

                        maximumDeviation = Math.max(maximumDeviation, deviations[i]);
                    }

                    if (maximumDeviation <= PLANAR_SURFACE_TOLERANCE) {
                        marked.add(Objects.requireNonNull(edge, "Planar polygon refinement requires the longest source edge."));
                        return false;
                    }

                    for (int i = 0; i < 3; i++) {
                        if (deviations[i] < maximumDeviation * MIN_REFINEMENT_EDGE_SHARE) {
                            continue;
                        }

                        marked.add(edgeKey(vertices.get(i), vertices.get((i + 1) % 3)));
                    }

                    return false;
                }

                next = new ArrayList<>();
                index = 0;
                stage = 8;
                return false;
            }

            if (stage == 8) {
                if (index < regions.size()) {
                    next.addAll(refine(regions.get(index++).triangle(), marked));
                    return false;
                }

                triangles = next;
                stage = 1;
                return false;
            }

            if (stage == 9) {
                if (index < regions.size()) {
                    Triangle triangle = regions.get(index++).triangle();
                    List<Point> vertices = triangle.vertices();
                    for (int edge = 0; edge < 3; edge++) {
                        Point a = vertices.get(edge);
                        Point b = vertices.get((edge + 1) % 3);
                        if (!topology.exterior(a, b)) {
                            continue;
                        }

                        exteriorLength += Math.hypot(a.u() - b.u(), a.v() - b.v());
                    }

                    return false;
                }

                double expectedLength = topology.boundaryLength();
                if (Math.abs(exteriorLength - expectedLength) > EXTERIOR_LENGTH_TOLERANCE) {
                    throw new UnsupportedOperationException("Actual source exterior was omitted or duplicated during polygon tessellation: expected=" + expectedLength + ", generated=" + exteriorLength + '.');
                }

                index = 0;
                stage = 10;
                return false;
            }

            if (index < patches.size()) {
                tiles.add(tile(display, patches.get(index++)));
                return false;
            }

            result = new FamilyGeometry(family, tiles, true);
            stage = 11;
            return true;
        }

        @Override
        public FamilyGeometry result() {
            return Objects.requireNonNull(result, "Polygon display is not complete.");
        }

        private void accept(CheckedPatch checked) {
            GeometryError measured = checked.error();
            geometryError = Math.max(geometryError, measured.maximum());
            if (!measured.passes()) {
                failed.add(regions.get(index).triangle());
            }

            eventSignViolation = Math.max(eventSignViolation, checked.eventSignViolation());
            double worst = checked.worstMeasured();
            if (worst > worstMeasured) {
                worstMeasured = worst;
                worstContext = checked.worstContext();
            }

            patches.add(checked.patch());
            branch++;
            stage = 3;
        }

        private List<Triangle> initialTriangles() {
            List<Triangle> triangles = new ArrayList<>();
            for (BeamRegion cell : topology.cells()) {
                List<Point> boundary = topology.boundaryVertices(cell);
                List<Point> vertices = cell.vertices();
                if (boundary.size() == vertices.size()) {
                    for (int i = 1; i + 1 < boundary.size(); i++) {
                        triangles.add(new Triangle(boundary.getFirst(), boundary.get(i), boundary.get(i + 1)));
                    }
                    continue;
                }

                Point center = new Point(vertices.stream().mapToDouble(Point::u).average().orElseThrow(), vertices.stream().mapToDouble(Point::v).average().orElseThrow());
                for (int i = 0; i < boundary.size(); i++) {
                    triangles.add(new Triangle(center, boundary.get(i), boundary.get((i + 1) % boundary.size())));
                }
            }

            return triangles;
        }

        private Tile tile(LaserBeamPolygonDisplay display, Patch patch) {
            Triangle triangle = patch.triangle();
            LaserBeamBrightness brightness = Objects.requireNonNull(patch.field(), "Mapped polygon display patch must have a brightness field.");
            Point center = triangle.center();
            Ray ray = display.path(center);
            List<Vec3> starts = display.cap(patch, 0);
            List<Vec3> ends = display.cap(patch, 1);
            Vec3 startCenter = display.point(center, patch, 0);
            Vec3 endCenter = display.point(center, patch, 1);
            boolean internalStart = patch.crossing() && patch.branch() == 1;
            boolean internalEnd = patch.crossing() && patch.branch() == 0;
            List<OpticalHit> events = family.path();
            Vec3 startNormal = events.isEmpty() ? family.source().frame().direction() : events.getLast().surface().normal();
            Vec3 endNormal = family.terminal().surface().normal();
            List<OpticalPlane> boundaries = new ArrayList<>();
            double residual = 0;
            if (!internalStart) {
                for (Vec3 vertex : starts) {
                    residual = Math.max(residual, Math.abs(vertex.subtract(startCenter).dot(startNormal)));
                }

                boundaries.add(new OpticalPlane(startCenter, startNormal));
            }

            if (!internalEnd) {
                for (Vec3 vertex : ends) {
                    residual = Math.max(residual, Math.abs(vertex.subtract(endCenter).dot(endNormal)));
                }

                boundaries.add(new OpticalPlane(endCenter, endNormal));
            }

            if (residual > CONTACT_GATE) {
                throw new UnsupportedOperationException("Mapped physical boundary exceeds the display geometry limit.");
            }

            List<Point> polygon = support(triangle);
            List<Boolean> exposed = new ArrayList<>();
            List<Vec3> outward = new ArrayList<>();
            for (int edge = 0; edge < polygon.size(); edge++) {
                Point first = polygon.get(edge);
                Point second = polygon.get((edge + 1) % polygon.size());
                exposed.add(topology.exterior(first, second));
                outward.add(display.point(first.lerp(second, 0.5), patch, 0.5).subtract(display.point(center, patch, 0.5)));
            }

            BeamState start = ray.start();
            BeamFrame frame = start.frame();
            LaserBeamBrightness axis = new LaserBeamBrightness(frame.origin(), frame.direction(), start.profile());
            Section geometry = new Section(starts, ends, false, true);
            return new Tile(geometry, geometry, polygon, startCenter, endCenter, brightness, axis, distance(ray, patch, 0), exposed, outward, boundaries, display.point(center, patch, 0.5));
        }

        private List<RegionTriangle> splitFocus(LaserBeamPolygonDisplay display, Triangle triangle) {
            List<Point> vertices = triangle.vertices();
            boolean any = vertices.stream().anyMatch(p -> display.path(p).hasFocus());
            boolean all = vertices.stream().allMatch(p -> display.path(p).hasFocus());
            if (!any) {
                return List.of(new RegionTriangle(triangle, false, triangle));
            }

            if (!all) {
                throw new UnsupportedOperationException("Source triangle crosses a focus-at-start event; terminal-focus clipping cannot silently delete that branch.");
            }

            double low = vertices.stream().mapToDouble(point -> event(display, point)).min().orElseThrow();
            double high = vertices.stream().mapToDouble(point -> event(display, point)).max().orElseThrow();
            if (high <= 0) {
                return List.of(new RegionTriangle(triangle, false, triangle));
            }

            if (low >= 0) {
                return List.of(new RegionTriangle(triangle, true, triangle));
            }

            List<RegionTriangle> result = new ArrayList<>();
            for (boolean after : List.of(false, true)) {
                List<Point> polygon = clipEvent(display, vertices, after);
                for (int i = 1; i + 1 < polygon.size(); i++) {
                    Triangle part = new Triangle(polygon.getFirst(), polygon.get(i), polygon.get(i + 1));
                    if (!(area(part) > MIN_REGION_TRIANGLE_AREA)) {
                        continue;
                    }

                    result.add(new RegionTriangle(part, after, triangle));
                }
            }

            double sum = result.stream().mapToDouble(r -> area(r.triangle())).sum();
            if (Math.abs(sum - area(triangle)) > DOMAIN_AREA_TOLERANCE) {
                throw new UnsupportedOperationException("Mixed-focus event clipping changed source-domain area.");
            }

            return List.copyOf(result);
        }

        private List<Point> clipEvent(LaserBeamPolygonDisplay display, List<Point> vertices, boolean after) {
            List<Point> result = new ArrayList<>();
            for (int i = 0; i < vertices.size(); i++) {
                Point a = vertices.get(i);
                Point b = vertices.get((i + 1) % vertices.size());
                double ha = event(display, a);
                double hb = event(display, b);
                boolean ia = after ? ha >= 0 : ha <= 0;
                boolean ib = after ? hb >= 0 : hb <= 0;
                if (ia && (result.isEmpty() || !result.getLast().equals(a))) {
                    result.add(a);
                }

                if (ia == ib) {
                    continue;
                }

                Point root = eventRoot(display, a, b);
                if (!result.isEmpty() && result.getLast().equals(root)) {
                    continue;
                }

                result.add(root);
            }

            if (result.size() > 1 && result.getFirst().equals(result.getLast())) {
                result.removeLast();
            }

            return List.copyOf(result);
        }

        private Point eventRoot(LaserBeamPolygonDisplay display, Point a, Point b) {
            List<Point> key = new ArrayList<>(List.of(a, b));
            key.sort(Comparator.comparingDouble(Point::u).thenComparingDouble(Point::v));
            key = List.copyOf(key);
            return eventRoots.computeIfAbsent(key, k -> {
                Point left = k.getFirst();
                Point right = k.getLast();
                double hl = event(display, left);
                double hr = event(display, right);
                if (hl == 0) {
                    return left;
                }

                if (hr == 0) {
                    return right;
                }

                for (int i = 0; i < FOCUS_ROOT_ITERATIONS; i++) {
                    Point middle = left.lerp(right, 0.5);
                    double h = event(display, middle);
                    if (h == 0) {
                        return middle;
                    }

                    if (Math.signum(h) == Math.signum(hl)) {
                        left = middle;
                        hl = h;
                        continue;
                    }

                    right = middle;
                }

                return left.lerp(right, 0.5);
            });
        }

        private double edgeDeviation(LaserBeamPolygonDisplay display, Triangle triangle, boolean after, int branch, Point a, Point b) {
            Patch patch = new Patch(triangle, branch, after, null);
            Vec3 a0 = display.point(a, patch, 0);
            Vec3 a1 = display.point(a, patch, 1);
            Vec3 b0 = display.point(b, patch, 0);
            Vec3 b1 = display.point(b, patch, 1);
            double result = 0;
            if (!display.terminalField.connecting()) {
                for (double phase : new double[]{0, 1}) {
                    Vec3 first = display.point(a, patch, phase);
                    Vec3 last = display.point(b, patch, phase);
                    Vec3 middle = display.point(a.lerp(b, 0.5), patch, phase);
                    for (int i = 1; i < 4; i++) {
                        Vec3 p = display.point(a.lerp(b, i / 4.0), patch, phase);
                        result = Math.max(result, Math.min(segmentDistance(p, first, middle), segmentDistance(p, middle, last)));
                    }
                }

                if (!topology.exterior(a, b)) {
                    return result;
                }
            }

            DistanceTriangle first = new DistanceTriangle(a0, a1, b1);
            DistanceTriangle second = new DistanceTriangle(a0, b1, b0);
            for (int i = 1; i < 4; i++) {
                PathSample sample = display.sample(a.lerp(b, i / 4.0));
                for (int j = 1; j < 4; j++) {
                    Vec3 p = point(sample, patch, j / 4.0);
                    result = Math.max(result, Math.min(first.distanceTo(p), second.distanceTo(p)));
                }
            }

            return result;
        }
    }

    private static final class ErrorWork implements LaserBeamWork<GeometryError> {
        private static final int SAMPLES_PER_STEP = 16;
        private final LaserBeamPolygonDisplay owner;
        private final Patch patch;
        private final int n;
        private final PatchGeometry input;
        private final Triangle triangle;
        private final List<Point> boundary;
        private final List<Vec3> startCap;
        private final List<Vec3> endCap;
        private final List<DistanceTriangle> startTriangles;
        private final List<DistanceTriangle> endTriangles;
        private int stage;
        private int i;
        private int j;
        private int edge;
        private double geometry;
        private double focusGeometry;
        private @Nullable Point a;
        private @Nullable Point b;
        private @Nullable Vec3 a0;
        private @Nullable Vec3 a1;
        private @Nullable Vec3 b0;
        private @Nullable Vec3 b1;
        private @Nullable DistanceTriangle first;
        private @Nullable DistanceTriangle second;
        private @Nullable PathSample sample;

        private ErrorWork(LaserBeamPolygonDisplay owner, Patch patch, PatchGeometry input, int n) {
            this.owner = owner;
            this.patch = patch;
            this.input = input;
            this.n = n;
            triangle = patch.triangle();
            boundary = input.boundary;
            startCap = input.startCap;
            endCap = input.endCap;
            startTriangles = input.startTriangles;
            endTriangles = input.endTriangles;
        }

        @Override
        public boolean step() {
            for (int count = 0; count < SAMPLES_PER_STEP; count++) {
                if (sampleStep()) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public GeometryError result() {
            if (stage != 3) {
                throw new IllegalStateException("Polygon error grid is not complete.");
            }

            return new GeometryError(geometry, focusGeometry);
        }

        private boolean sampleStep() {
            if (stage == 3) {
                return true;
            }

            if (stage == 0) {
                if (i <= n) {
                    Point first = triangle.a();
                    Point second = triangle.b();
                    Point third = triangle.c();
                    double originU = first.u();
                    double originV = first.v();
                    Point uv = new Point(originU + (second.u() - originU) * i / n + (third.u() - originU) * j / n, originV + (second.v() - originV) * i / n + (third.v() - originV) * j / n);
                    PathSample current = owner.sample(uv);
                    double h = current.path().focusEvent();
                    if (Double.isFinite(h)) {
                        input.eventSignViolation = Math.max(input.eventSignViolation, patch.crossing() ? Math.max(0, -h) : Math.max(0, h));
                    }

                    for (double phase : new double[]{0, 1}) {
                        Vec3 point = point(current, patch, phase);
                        List<Vec3> corners = phase == 0 ? startCap : endCap;
                        double measured = Double.POSITIVE_INFINITY;
                        for (DistanceTriangle metric : phase == 0 ? startTriangles : endTriangles) {
                            measured = Math.min(measured, metric.distanceTo(point));
                        }

                        if (patch.crossing() && (phase == 0 && patch.branch() == 1 || phase == 1 && patch.branch() == 0)) {
                            focusGeometry = Math.max(focusGeometry, measured);
                        }
                        else {
                            geometry = Math.max(geometry, measured);
                        }

                        if (!(measured > input.worstMeasured)) {
                            continue;
                        }

                        input.worstMeasured = measured;
                        input.worstContext = new WorstSample(-1, phase, n, uv, triangle, point, corners);
                    }

                    if (++j > n - i) {
                        j = 0;
                        i++;
                    }

                    return false;
                }

                stage = 1;
            }

            if (stage == 1) {
                if (edge >= boundary.size()) {
                    stage = 3;
                    return true;
                }

                a = boundary.get(edge);
                b = boundary.get((edge + 1) % boundary.size());
                if (!owner.topology.exterior(a, b)) {
                    edge++;
                    return false;
                }

                SideGeometry side = input.sides[edge];
                if (side == null) {
                    side = new SideGeometry(owner.point(a, patch, 0), owner.point(a, patch, 1), owner.point(b, patch, 0), owner.point(b, patch, 1));
                    input.sides[edge] = side;
                }

                a0 = side.a0;
                a1 = side.a1;
                b0 = side.b0;
                b1 = side.b1;
                first = side.first;
                second = side.second;
                i = 0;
                j = 0;
                stage = 2;
                return false;
            }

            if (j == 0) {
                sample = owner.sample(Objects.requireNonNull(a, "Polygon side sampling requires the first source edge vertex.").lerp(Objects.requireNonNull(b, "Polygon side sampling requires the second source edge vertex."), (double) i / n));
            }

            Vec3 point = point(Objects.requireNonNull(sample, "Polygon side validation requires a ray sample for the current edge point."), patch, (double) j / n);
            double measured = Math.min(Objects.requireNonNull(first, "Polygon side validation requires the first distance triangle.").distanceTo(point), Objects.requireNonNull(second, "Polygon side validation requires the second distance triangle.").distanceTo(point));
            if (patch.crossing() && (j == 0 && patch.branch() == 1 || j == n && patch.branch() == 0)) {
                focusGeometry = Math.max(focusGeometry, measured);
            }
            else {
                geometry = Math.max(geometry, measured);
            }

            if (measured > input.worstMeasured) {
                input.worstMeasured = measured;
                input.worstContext = new WorstSample(edge, j, n, Objects.requireNonNull(a, "Polygon side error reporting requires the first source edge vertex.").lerp(Objects.requireNonNull(b, "Polygon side error reporting requires the second source edge vertex."), (double) i / n), triangle, point, List.of(Objects.requireNonNull(a0, "Polygon side error reporting requires the first vertex on the start cap."), Objects.requireNonNull(a1, "Polygon side error reporting requires the first vertex on the end cap."), Objects.requireNonNull(b1, "Polygon side error reporting requires the second vertex on the end cap."), Objects.requireNonNull(b0, "Polygon side error reporting requires the second vertex on the start cap.")));
            }

            if (++j > n) {
                j = 0;
                if (++i > n) {
                    edge++;
                    stage = 1;
                }
            }

            return false;
        }
    }

    private static final class PatchGeometry {
        private final Patch patch;
        private final List<Point> boundary;
        private final List<Vec3> startCap;
        private final List<Vec3> endCap;
        private final List<DistanceTriangle> startTriangles = new ArrayList<>();
        private final List<DistanceTriangle> endTriangles = new ArrayList<>();
        private final SideGeometry[] sides;
        private double eventSignViolation;
        private double worstMeasured;
        private @Nullable WorstSample worstContext;

        private PatchGeometry(LaserBeamPolygonDisplay owner, Patch patch) {
            this.patch = patch;
            boundary = support(patch.triangle());
            startCap = owner.cap(patch, 0);
            endCap = owner.cap(patch, 1);
            sides = new SideGeometry[boundary.size()];
            for (int index = 1; index + 1 < startCap.size(); index++) {
                startTriangles.add(new DistanceTriangle(startCap.getFirst(), startCap.get(index), startCap.get(index + 1)));
                endTriangles.add(new DistanceTriangle(endCap.getFirst(), endCap.get(index), endCap.get(index + 1)));
            }
        }

        private static double q(LaserBeamPolygonDisplay display, Point uv, Patch patch, double t) {
            Ray path = display.path(uv);
            return path.width(distance(path, patch, t));
        }

        private LaserBeamBrightness field(LaserBeamPolygonDisplay display) {
            Patch stub = patch;
            Triangle triangle = stub.triangle();
            int branch = stub.branch();
            boolean crossing = stub.crossing();
            List<Vec3> a = startCap;
            List<Vec3> b = endCap;
            double maximum = -1;
            double t = 0;
            int ai = 0;
            int bi = 1;
            int ci = 2;
            for (int phase = 0; phase <= 1; phase++) {
                List<Vec3> p = phase == 0 ? a : b;
                for (int i = 0; i < p.size(); i++) {
                    for (int j = i + 1; j < p.size(); j++) {
                        for (int k = j + 1; k < p.size(); k++) {
                            double area = p.get(j).subtract(p.get(i)).cross(p.get(k).subtract(p.get(i))).lengthSqr();
                            if (!(area > maximum)) {
                                continue;
                            }

                            maximum = area;
                            t = phase;
                            ai = i;
                            bi = j;
                            ci = k;
                        }
                    }
                }
            }

            List<Vec3> cap = t == 0 ? a : b;
            List<Point> uv = boundary;
            Vec3 origin = cap.get(ai);
            Vec3 x = cap.get(bi).subtract(origin);
            Vec3 y = cap.get(ci).subtract(origin);
            Point center = triangle.center();
            Vec3 z = display.point(center, stub, 1 - t).subtract(origin);
            double determinant = x.dot(y.cross(z));
            if (Math.abs(determinant) < MIN_SCALAR_SUPPORT_DETERMINANT) {
                throw new UnsupportedOperationException("Degenerate mapped display scalar support; det=" + determinant + ", triangle=" + triangle + ", branch=" + branch + ", after=" + crossing + ", a=" + a + ", b=" + b + '.');
            }

            double base = q(display, uv.get(ai), stub, t);
            double dx = q(display, uv.get(bi), stub, t) - base;
            double dy = q(display, uv.get(ci), stub, t) - base;
            double dz = q(display, center, stub, 1 - t) - base;
            Vec3 gradient = y.cross(z).scale(dx / determinant).add(z.cross(x).scale(dy / determinant)).add(x.cross(y).scale(dz / determinant));
            double slope = gradient.length();
            if (slope < MIN_BRIGHTNESS_GRADIENT) {
                return new LaserBeamBrightness(origin, display.path(center).start().frame().direction(), new BeamProfile(base, 0));
            }

            Vec3 direction = gradient.scale(1 / slope);
            return new LaserBeamBrightness(origin.subtract(direction), direction, new BeamProfile(base - slope, slope));
        }
    }

    private static final class SideGeometry {
        private final Vec3 a0;
        private final Vec3 a1;
        private final Vec3 b0;
        private final Vec3 b1;
        private final DistanceTriangle first;
        private final DistanceTriangle second;

        private SideGeometry(Vec3 a0, Vec3 a1, Vec3 b0, Vec3 b1) {
            this.a0 = a0;
            this.a1 = a1;
            this.b0 = b0;
            this.b1 = b1;
            first = new DistanceTriangle(a0, a1, b1);
            second = new DistanceTriangle(a0, b1, b0);
        }
    }

    private record GeometryError(double surface, double focus) {
        private boolean passes() {
            return surface <= SURFACE_GATE && focus <= FOCUS_GATE;
        }

        private double maximum() {
            return Math.max(surface, focus);
        }
    }

    private record CheckedPatch(Patch patch, GeometryError error, double eventSignViolation, double worstMeasured, @Nullable WorstSample worstContext) {}

    private record WorstSample(int edge, double phase, int samples, Point uv, Triangle triangle, Vec3 point, List<Vec3> vertices) {
        private String describe() {
            if (edge < 0) {
                return "cap phase=" + phase + ", uv=" + uv + ", triangle=" + triangle + ", point=" + point + ", cap=" + vertices;
            }

            return "side edge=" + edge + ", uv=" + uv + ", phase=" + (int) phase + '/' + samples + ", triangle=" + triangle + ", point=" + point + ", side=" + vertices;
        }
    }

    private static final class DistanceTriangle {
        private final Vec3 a;
        private final Vec3 b;
        private final Vec3 c;
        private final Vec3 ab;
        private final Vec3 ac;
        private final Vec3 normal;
        private final double normalLengthSqr;
        private final double abLengthSqr;
        private final double abDotAc;
        private final double acLengthSqr;
        private final double determinant;
        private final double normalLength;

        private DistanceTriangle(Vec3 a, Vec3 b, Vec3 c) {
            this.a = a;
            this.b = b;
            this.c = c;
            ab = b.subtract(a);
            ac = c.subtract(a);
            normal = ab.cross(ac);
            normalLengthSqr = normal.lengthSqr();
            abLengthSqr = ab.lengthSqr();
            abDotAc = ab.dot(ac);
            acLengthSqr = ac.lengthSqr();
            determinant = abLengthSqr * acLengthSqr - abDotAc * abDotAc;
            normalLength = Math.sqrt(normalLengthSqr);
        }

        private double distanceTo(Vec3 point) {
            if (normalLengthSqr > 0) {
                double d = point.subtract(a).dot(normal) / normalLengthSqr;
                Vec3 v = point.subtract(normal.scale(d)).subtract(a);
                if (determinant > 0) {
                    double u = (v.dot(ab) * acLengthSqr - v.dot(ac) * abDotAc) / determinant;
                    double w = (v.dot(ac) * abLengthSqr - v.dot(ab) * abDotAc) / determinant;
                    if (u >= -BARYCENTRIC_TOLERANCE && w >= -BARYCENTRIC_TOLERANCE && u + w <= BARYCENTRIC_MAX_SUM) {
                        return Math.abs(d) * normalLength;
                    }
                }
            }

            return Math.min(segmentDistance(point, a, b), Math.min(segmentDistance(point, b, c), segmentDistance(point, c, a)));
        }
    }

    private record PathSample(Ray path, Vec3 start, @Nullable Vec3 focus) {}

    private record RegionTriangle(Triangle triangle, boolean after, Triangle source) {}

    private record Triangle(Point a, Point b, Point c) {
        @Unmodifiable
        List<Point> vertices() {
            return List.of(a, b, c);
        }

        Point center() {
            return new Point((a.u() + b.u() + c.u()) / 3, (a.v() + b.v() + c.v()) / 3);
        }
    }

    private record Patch(Triangle triangle, int branch, boolean crossing, @Nullable LaserBeamBrightness field) {}
}
