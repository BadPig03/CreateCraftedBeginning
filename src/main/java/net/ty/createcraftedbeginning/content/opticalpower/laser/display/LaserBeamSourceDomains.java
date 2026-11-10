package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamSourceDomains {
    private static final double COORDINATE_TOLERANCE = 1.0E-12;
    private static final int MAX_PARTITION_CELLS = 512;

    private final List<List<Point>> rings;
    private final List<BeamRegion> cells;

    LaserBeamSourceDomains(List<BeamRegion> domains) {
        Map<Double, Double> us = cluster(domains.stream().flatMap(domain -> domain.vertices().stream()).map(Point::u).toList());
        Map<Double, Double> vs = cluster(domains.stream().flatMap(domain -> domain.vertices().stream()).map(Point::v).toList());
        Area union = new Area();
        for (BeamRegion domain : domains) {
            List<Point> snapped = domain.vertices().stream().map(point -> new Point(us.get(point.u()), vs.get(point.v()))).toList();
            union.add(new BeamRegion(snapped).toArea());
        }
        List<List<Point>> boundaries = new ArrayList<>();
        double[] coordinates = new double[6];
        List<Point> ring = new ArrayList<>();
        for (PathIterator iterator = union.getPathIterator(null); !iterator.isDone(); iterator.next()) {
            int kind = iterator.currentSegment(coordinates);
            if (kind == PathIterator.SEG_MOVETO) {
                ring = new ArrayList<>();
                boundaries.add(ring);
            }
            if (kind == PathIterator.SEG_MOVETO || kind == PathIterator.SEG_LINETO) {
                ring.add(new Point(coordinates[0], coordinates[1]));
                continue;
            }

            if (kind != PathIterator.SEG_CLOSE) {
                throw new UnsupportedOperationException("Source UV union unexpectedly contains curves.");
            }
        }
        rings = boundaries.stream().map(List::copyOf).toList();
        cells = BeamRegion.partition(new Area(union), MAX_PARTITION_CELLS);
        if (cells.isEmpty()) {
            throw new UnsupportedOperationException("Empty or over-budget source-domain partition.");
        }

        Area reconstructed = new Area();
        cells.forEach(cell -> reconstructed.add(cell.toArea()));
        reconstructed.exclusiveOr(union);
        if (!reconstructed.isEmpty() && BeamRegion.area(reconstructed) > COORDINATE_TOLERANCE) {
            throw new UnsupportedOperationException("Source-domain repartition changed the actual union.");
        }
    }

    private static Map<Double, Double> cluster(List<Double> values) {
        Map<Double, Double> result = new HashMap<>();
        double representative = Double.NaN;
        for (double value : values.stream().distinct().sorted().toList()) {
            if (!Double.isFinite(representative) || value - representative > COORDINATE_TOLERANCE) {
                representative = value;
            }
            result.put(value, representative);
        }
        return result;
    }

    List<BeamRegion> cells() {
        return cells;
    }

    List<List<Point>> rings() {
        return rings;
    }

    @Unmodifiable List<Point> boundaryVertices(BeamRegion cell) {
        List<Point> candidates = new ArrayList<>();
        rings.forEach(candidates::addAll);
        cells.forEach(part -> candidates.addAll(part.vertices()));
        List<Point> result = new ArrayList<>();
        List<Point> vertices = cell.vertices();
        for (int index = 0; index < vertices.size(); index++) {
            Point first = vertices.get(index);
            Point second = vertices.get((index + 1) % vertices.size());
            double dx = second.u() - first.u();
            double dy = second.v() - first.v();
            double lengthSqr = dx * dx + dy * dy;
            List<Point> edge = new ArrayList<>();
            edge.add(first);
            for (Point point : candidates) {
                double fraction = ((point.u() - first.u()) * dx + (point.v() - first.v()) * dy) / lengthSqr;
                if (fraction <= COORDINATE_TOLERANCE || fraction >= 1 - COORDINATE_TOLERANCE || Math.abs((point.u() - first.u()) * dy - (point.v() - first.v()) * dx) > COORDINATE_TOLERANCE || edge.contains(point)) {
                    continue;
                }

                edge.add(point);
            }
            edge.sort(Comparator.comparingDouble(point -> ((point.u() - first.u()) * dx + (point.v() - first.v()) * dy) / lengthSqr));
            result.addAll(edge);
        }
        return List.copyOf(result);
    }

    boolean exterior(Point first, Point second) {
        double dx = second.u() - first.u();
        double dy = second.v() - first.v();
        double lengthSqr = dx * dx + dy * dy;
        if (lengthSqr == 0) {
            return false;
        }

        List<double[]> intervals = new ArrayList<>();
        for (List<Point> ring : rings) {
            for (int index = 0; index < ring.size(); index++) {
                Point a = ring.get(index);
                Point b = ring.get((index + 1) % ring.size());
                if (Math.abs((a.u() - first.u()) * dy - (a.v() - first.v()) * dx) > COORDINATE_TOLERANCE || Math.abs((b.u() - first.u()) * dy - (b.v() - first.v()) * dx) > COORDINATE_TOLERANCE) {
                    continue;
                }

                double t = ((a.u() - first.u()) * dx + (a.v() - first.v()) * dy) / lengthSqr;
                double s = ((b.u() - first.u()) * dx + (b.v() - first.v()) * dy) / lengthSqr;
                double low = Math.max(0, Math.min(t, s));
                double high = Math.min(1, Math.max(t, s));
                if (high <= low) {
                    continue;
                }

                intervals.add(new double[]{low, high});
            }
        }
        intervals.sort(Comparator.comparingDouble(pair -> pair[0]));
        double covered = 0;
        for (double[] interval : intervals) {
            if (interval[0] > covered + COORDINATE_TOLERANCE) {
                return false;
            }

            covered = Math.max(covered, interval[1]);
        }
        return covered >= 1 - COORDINATE_TOLERANCE;
    }

    double boundaryLength() {
        double length = 0;
        for (List<Point> ring : rings) {
            for (int index = 0; index < ring.size(); index++) {
                Point first = ring.get(index);
                Point second = ring.get((index + 1) % ring.size());
                length += Math.hypot(first.u() - second.u(), first.v() - second.v());
            }
        }
        return length;
    }
}
