package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.Tile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMappedGeometry.FamilyGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserDisplayFamilies.Family;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserDisplayPath.PhysicalPoint;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamStripDisplay {
    private static final double GATE = 1.0E-4;
    private static final double TILE_ACCEPTANCE_MARGIN = 0.8;
    private static final double WIDTH_DEPENDENCE_TOLERANCE = 1.0E-8;
    private static final double COORDINATE_TOLERANCE = 1.0E-12;
    private static final double DOMAIN_AREA_TOLERANCE = 1.0E-12;
    private static final double CAP_POSITION_TOLERANCE = 1.0E-7;
    private static final double MIN_SCALAR_SUPPORT_DETERMINANT = 1.0E-24;
    private static final double MIN_BRIGHTNESS_GRADIENT = 1.0E-14;
    private static final double FOCUS_WIDTH_TOLERANCE = 1.0E-9;
    private static final double BARYCENTRIC_TOLERANCE = 1.0E-12;
    private static final double BARYCENTRIC_MAX_SUM = 1.000000000001;
    private static final int MAX_TILES = 128;
    private static final int MAX_REFINEMENT_DEPTH = 16;
    private static final int REFINEMENT_SAMPLE_INTERVALS = 4;
    private static final int VALIDATION_SAMPLE_INTERVALS = 32;
    private final Family family;
    private final List<Bounds> components;
    private final Map<Key, PhysicalPoint> points = new HashMap<>();
    private Bounds bounds;

    private LaserBeamStripDisplay(Family family) {
        this.family = family;
        components = rectangleComponents(family.sourceDomains());
    }

    static FamilyGeometry create(Family family) {
        LaserBeamWork<FamilyGeometry> work = begin(family);
        while (!work.step()) {}
        return work.result();
    }

    static LaserBeamWork<FamilyGeometry> begin(Family family) {
        return new Build(family);
    }

    private static final class Build implements LaserBeamWork<FamilyGeometry> {
        private final LaserBeamStripDisplay owner;
        private final ArrayDeque<Pending> pending = new ArrayDeque<>();
        private final List<Strip> strips = new ArrayList<>();
        private final List<Tile> tiles = new ArrayList<>();
        private int component;
        private int finalIndex;
        private boolean validating;
        private Pending current;
        private LaserBeamBrightness field;
        private @Nullable ErrorWork error;
        private FamilyGeometry result;

        private Build(Family family) {
            owner = new LaserBeamStripDisplay(family);
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            if (error != null) {
                if (!error.step()) {
                    return false;
                }

                double measured = error.geometry;
                error = null;
                if (validating) {
                    if (measured > GATE) {
                        throw new UnsupportedOperationException("Source strip exceeds its finite display geometry limit.");
                    }

                    tiles.add(tile(strips.get(finalIndex++)));
                    return false;
                }

                if (measured <= GATE * TILE_ACCEPTANCE_MARGIN) {
                    strips.add(new Strip(current.a(), current.b(), field, owner.bounds));
                    if (strips.size() > MAX_TILES) {
                        throw new UnsupportedOperationException("Live display exceeded its explicit 128-tile limit.");
                    }

                    return false;
                }

                if (current.depth() >= MAX_REFINEMENT_DEPTH) {
                    throw new UnsupportedOperationException("Live display refinement exhausted.");
                }

                double middle = (current.a() + current.b()) / 2;
                pending.addFirst(new Pending(middle, current.b(), current.depth() + 1));
                pending.addFirst(new Pending(current.a(), middle, current.depth() + 1));
                return false;
            }

            if (!pending.isEmpty()) {
                current = pending.removeFirst();
                field = field(current.a(), current.b());
                error = new ErrorWork(owner, current.a(), current.b(), REFINEMENT_SAMPLE_INTERVALS);
                return false;
            }

            if (component < owner.components.size()) {
                owner.bounds = owner.components.get(component++);
                pending.addLast(new Pending(owner.bounds.minU(), owner.bounds.maxU(), 0));
                return false;
            }

            validating = true;
            if (finalIndex < strips.size()) {
                Strip strip = strips.get(finalIndex);
                owner.bounds = strip.bounds();
                error = new ErrorWork(owner, strip.a(), strip.b(), VALIDATION_SAMPLE_INTERVALS);
                return false;
            }

            result = new FamilyGeometry(owner.family, tiles, false);
            return true;
        }

        @Override
        public FamilyGeometry result() {
            return Objects.requireNonNull(result, "Laser beam strip display build has not completed.");
        }

        private Tile tile(Strip strip) {
            Bounds bounds = strip.bounds();
            owner.bounds = bounds;
            double a = strip.a();
            double b = strip.b();
            double minV = bounds.minV();
            double maxV = bounds.maxV();
            double supportA = a;
            double supportB = b;
            if (owner.point(a, minV, 0).distanceTo(owner.point(a, minV, 1)) < CAP_POSITION_TOLERANCE) {
                supportA = a + (b - a) * 0.25;
            }

            if (owner.point(b, minV, 0).distanceTo(owner.point(b, minV, 1)) < CAP_POSITION_TOLERANCE) {
                supportB = a + (b - a) * 0.75;
            }

            double u = (a + b) / 2;
            double v = (minV + maxV) / 2;
            PhysicalPoint center = owner.sample(u, v);
            Vec3 middle = owner.point(u, v, 0.5);
            List<Point> polygon = List.of(new Point(a, minV), new Point(b, minV), new Point(b, maxV), new Point(a, maxV));
            List<Vec3> outward = List.of(owner.point(u, minV, 0.5).subtract(middle), owner.point(b, v, 0.5).subtract(middle), owner.point(u, maxV, 0.5).subtract(middle), owner.point(a, v, 0.5).subtract(middle));
            BeamState start = center.start();
            BeamFrame startFrame = start.frame();
            Vec3 startCenter = startFrame.origin();
            LaserBeamBrightness axis = new LaserBeamBrightness(startCenter, startFrame.direction(), start.profile());
            Section geometry = new Section(cap(bounds, a, b, 0), cap(bounds, a, b, 1), false, true);
            Section support = new Section(cap(bounds, supportA, supportB, 0), cap(bounds, supportA, supportB, 1), false, true);
            return new Tile(geometry, support, polygon, startCenter, center.incident().frame().origin(), strip.field(), axis, 0, List.of(true, b == bounds.maxU(), true, a == bounds.minU()), outward, null, null);
        }

        private LaserBeamBrightness field(double a, double b) {
            Bounds bounds = owner.bounds;
            double v = (bounds.minV() + bounds.maxV()) / 2;
            Vec3 origin = owner.point(a, v, 0);
            Vec3 x = owner.point(b, v, 0).subtract(origin);
            Vec3 z = owner.point(a, v, 1).subtract(origin);
            double xx = x.lengthSqr();
            double xz = x.dot(z);
            double zz = z.lengthSqr();
            double determinant = xx * zz - xz * xz;
            if (!(determinant > MIN_SCALAR_SUPPORT_DETERMINANT)) {
                throw new UnsupportedOperationException("Degenerate live display scalar support.");
            }

            double q = owner.width(a, v, 0);
            double dx = owner.width(b, v, 0) - q;
            double dz = owner.width(a, v, 1) - q;
            Vec3 gradient = x.scale((dx * zz - dz * xz) / determinant).add(z.scale((dz * xx - dx * xz) / determinant));
            double slope = gradient.length();
            if (slope < MIN_BRIGHTNESS_GRADIENT) {
                return new LaserBeamBrightness(origin, owner.sample(a, v).start().frame().direction(), new BeamProfile(q, 0));
            }

            Vec3 direction = gradient.scale(1 / slope);
            return new LaserBeamBrightness(origin.subtract(direction), direction, new BeamProfile(q - slope, slope));
        }

        private List<Vec3> cap(Bounds bounds, double a, double b, double t) {
            double minV = bounds.minV();
            double maxV = bounds.maxV();
            return List.of(owner.point(a, minV, t), owner.point(b, minV, t), owner.point(b, maxV, t), owner.point(a, maxV, t));
        }
    }

    private static final class ErrorWork {
        private final LaserBeamStripDisplay owner;
        private final double a;
        private final double b;
        private final int n;
        private int i;
        private int j;
        private double geometry;
        private final DistanceTriangle[][] sides = new DistanceTriangle[2][];

        private ErrorWork(LaserBeamStripDisplay owner, double a, double b, int n) {
            this.owner = owner;
            this.a = a;
            this.b = b;
            this.n = n;
        }

        private boolean step() {
            double centerV = (owner.bounds.minV() + owner.bounds.maxV()) / 2;
            double f = (double) i / n;
            double u = a + (b - a) * f;
            double t = (double) j / n;
            double q = owner.width(u, centerV, t);
            for (int side = 0; side < 2; side++) {
                double v = side == 0 ? owner.bounds.minV() : owner.bounds.maxV();
                if (Math.abs(owner.width(u, v, t) - q) > WIDTH_DEPENDENCE_TOLERANCE) {
                    throw new UnsupportedOperationException("Live display q also varies along v; this bounded loft does not support it.");
                }

                Vec3 p = owner.point(u, v, t);
                DistanceTriangle[] metrics = sides[side];
                if (metrics == null) {
                    Vec3 p0 = owner.point(a, v, 0);
                    Vec3 p1 = owner.point(a, v, 1);
                    Vec3 p2 = owner.point(b, v, 1);
                    Vec3 p3 = owner.point(b, v, 0);
                    metrics = new DistanceTriangle[]{new DistanceTriangle(p0, p1, p2), new DistanceTriangle(p0, p2, p3)};
                    sides[side] = metrics;
                }

                geometry = Math.max(geometry, Math.min(metrics[0].distanceTo(p), metrics[1].distanceTo(p)));
            }

            j++;
            if (j > n) {
                j = 0;
                i++;
            }

            return i > n;
        }
    }

    private record Pending(double a, double b, int depth) {}

    private static @Unmodifiable List<Bounds> rectangleComponents(List<BeamRegion> domains) {
        if (domains.isEmpty()) {
            throw new UnsupportedOperationException("Empty live display domain.");
        }

        List<Bounds> rectangles = new ArrayList<>();
        for (BeamRegion region : domains) {
            List<Point> points = region.vertices();
            double minU = points.stream().mapToDouble(Point::u).min().orElseThrow();
            double maxU = points.stream().mapToDouble(Point::u).max().orElseThrow();
            double minV = points.stream().mapToDouble(Point::v).min().orElseThrow();
            double maxV = points.stream().mapToDouble(Point::v).max().orElseThrow();
            if (points.size() != 4 || points.stream().anyMatch(p -> Math.min(Math.abs(p.u() - minU), Math.abs(p.u() - maxU)) > COORDINATE_TOLERANCE || Math.min(Math.abs(p.v() - minV), Math.abs(p.v() - maxV)) > COORDINATE_TOLERANCE)) {
                throw new UnsupportedOperationException("Nonrectangular source domain requires polygon boundary loft; no enclosing rectangle is substituted.");
            }

            rectangles.add(new Bounds(minU, maxU, minV, maxV));
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            outer:
            for (int i = 0; i < rectangles.size(); i++) {
                Bounds a = rectangles.get(i);
                for (ListIterator<Bounds> remaining = rectangles.listIterator(i + 1); remaining.hasNext();) {
                    Bounds b = remaining.next();
                    Bounds joined = null;
                    if (Math.abs(a.minV() - b.minV()) <= COORDINATE_TOLERANCE && Math.abs(a.maxV() - b.maxV()) <= COORDINATE_TOLERANCE && (Math.abs(a.maxU() - b.minU()) <= COORDINATE_TOLERANCE || Math.abs(b.maxU() - a.minU()) <= COORDINATE_TOLERANCE)) {
                        joined = new Bounds(Math.min(a.minU(), b.minU()), Math.max(a.maxU(), b.maxU()), a.minV(), a.maxV());
                    }
                    else if (Math.abs(a.minU() - b.minU()) <= COORDINATE_TOLERANCE && Math.abs(a.maxU() - b.maxU()) <= COORDINATE_TOLERANCE && (Math.abs(a.maxV() - b.minV()) <= COORDINATE_TOLERANCE || Math.abs(b.maxV() - a.minV()) <= COORDINATE_TOLERANCE)) {
                        joined = new Bounds(a.minU(), a.maxU(), Math.min(a.minV(), b.minV()), Math.max(a.maxV(), b.maxV()));
                    }

                    if (joined == null) {
                        continue;
                    }

                    rectangles.set(i, joined);
                    remaining.remove();
                    changed = true;
                    break outer;
                }
            }
        }

        for (int i = 0; i < rectangles.size(); i++) {
            for (int j = i + 1; j < rectangles.size(); j++) {
                Bounds a = rectangles.get(i);
                Bounds b = rectangles.get(j);
                double overlapU = Math.min(a.maxU(), b.maxU()) - Math.max(a.minU(), b.minU());
                double overlapV = Math.min(a.maxV(), b.maxV()) - Math.max(a.minV(), b.minV());
                if (overlapU >= -COORDINATE_TOLERANCE && overlapV > COORDINATE_TOLERANCE || overlapV >= -COORDINATE_TOLERANCE && overlapU > COORDINATE_TOLERANCE) {
                    throw new UnsupportedOperationException("Source components overlap or share only part of an edge; polygon exterior extraction is required, not a filled bbox.");
                }
            }
        }

        rectangles.sort(Comparator.comparingDouble(Bounds::minV).thenComparingDouble(Bounds::minU));
        double expected = domains.stream().mapToDouble(BeamRegion::area).sum();
        double actual = rectangles.stream().mapToDouble(b -> (b.maxU() - b.minU()) * (b.maxV() - b.minV())).sum();
        if (Math.abs(expected - actual) > DOMAIN_AREA_TOLERANCE) {
            throw new UnsupportedOperationException("Exact component union changed source area.");
        }

        return List.copyOf(rectangles);
    }

    private PhysicalPoint sample(double u, double v) {
        return points.computeIfAbsent(new Key(u, v), ignored -> {
            PhysicalPoint value = family.evaluate(u, v);
            double first = value.start().profile().referenceWidth();
            double last = value.incident().profile().referenceWidth();
            if (first * last <= 0 || Math.min(Math.abs(first), Math.abs(last)) < BeamProfile.MIN_WIDTH + FOCUS_WIDTH_TOLERANCE) {
                throw new UnsupportedOperationException("Live family crosses a physical waist/focus; bounded terminal loft is not applicable.");
            }

            return value;
        });
    }

    private Vec3 point(double u, double v, double t) {
        PhysicalPoint point = sample(u, v);
        return point.start().frame().origin().lerp(point.incident().frame().origin(), t);
    }

    private double width(double u, double v, double t) {
        PhysicalPoint point = sample(u, v);
        BeamProfile profile = point.start().profile();
        return profile.referenceWidth() + profile.widthSlope() * point.distance() * t;
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

        private static double segmentDistance(Vec3 p, Vec3 a, Vec3 b) {
            Vec3 delta = b.subtract(a);
            double lengthSqr = delta.lengthSqr();
            double fraction = lengthSqr > 0 ? Math.clamp(p.subtract(a).dot(delta) / lengthSqr, 0, 1) : 0;
            return p.distanceTo(a.add(delta.scale(fraction)));
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

    private record Key(double u, double v) {}

    private record Bounds(double minU, double maxU, double minV, double maxV) {}

    private record Strip(double a, double b, LaserBeamBrightness field, Bounds bounds) {}
}
