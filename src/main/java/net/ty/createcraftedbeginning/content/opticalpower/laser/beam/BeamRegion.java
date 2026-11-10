package net.ty.createcraftedbeginning.content.opticalpower.laser.beam;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record BeamRegion(List<Point> vertices) {
    private static final double EPSILON = 1.0E-12;

    public BeamRegion {
        vertices = List.copyOf(vertices);
    }

    public static BeamRegion rectangle(double left, double bottom, double right, double top) {
        return new BeamRegion(List.of(new Point(left, bottom), new Point(right, bottom), new Point(right, top), new Point(left, top)));
    }

    public static double area(Area area) {
        double sum = 0;
        for (List<Point> ring : rings(area)) {
            sum += signedArea(ring);
        }
        return Math.abs(sum);
    }

    public static List<BeamRegion> partition(Area area, int limit) {
        List<List<Point>> rings = rings(area);
        List<Double> cuts = rings.stream().flatMap(List::stream).map(Point::u).distinct().sorted().toList();
        List<BeamRegion> regions = new ArrayList<>();
        for (int index = 1; index < cuts.size(); index++) {
            double left = cuts.get(index - 1);
            double right = cuts.get(index);
            if (right - left <= EPSILON) {
                continue;
            }

            double middle = (left + right) / 2;
            List<Edge> edges = new ArrayList<>();
            for (List<Point> ring : rings) {
                for (int vertex = 0; vertex < ring.size(); vertex++) {
                    Point first = ring.get(vertex);
                    Point second = ring.get((vertex + 1) % ring.size());
                    double firstU = first.u();
                    double secondU = second.u();
                    if (middle <= Math.min(firstU, secondU) || middle >= Math.max(firstU, secondU)) {
                        continue;
                    }

                    edges.add(new Edge(first, second));
                }
            }
            edges.sort(Comparator.comparingDouble(edge -> edge.at(middle)));
            for (int edge = 1; edge < edges.size(); edge += 2) {
                Edge lower = edges.get(edge - 1);
                Edge upper = edges.get(edge);
                List<Point> polygon = compact(List.of(new Point(left, lower.at(left)), new Point(right, lower.at(right)), new Point(right, upper.at(right)), new Point(left, upper.at(left))));
                if (polygon.size() < 3 || Math.abs(signedArea(polygon)) <= EPSILON) {
                    continue;
                }

                regions.add(new BeamRegion(polygon));
            }
            merge(regions);
            if (regions.size() > limit) {
                return List.of();
            }
        }
        return List.copyOf(regions);
    }

    private static List<List<Point>> rings(Area area) {
        List<List<Point>> rings = new ArrayList<>();
        List<Point> ring = new ArrayList<>();
        double[] coordinates = new double[6];
        for (PathIterator iterator = area.getPathIterator(null); !iterator.isDone(); iterator.next()) {
            int type = iterator.currentSegment(coordinates);
            if (type == PathIterator.SEG_MOVETO) {
                ring = new ArrayList<>();
                rings.add(ring);
            }
            if (type != PathIterator.SEG_MOVETO && type != PathIterator.SEG_LINETO) {
                continue;
            }

            ring.add(new Point(coordinates[0], coordinates[1]));
        }
        return rings;
    }

    private static double signedArea(List<Point> vertices) {
        if (vertices.size() < 3) {
            return 0;
        }

        Point origin = vertices.getFirst();
        double sum = 0;
        for (int index = 1; index + 1 < vertices.size(); index++) {
            sum += cross(origin, vertices.get(index), vertices.get(index + 1));
        }
        return sum / 2;
    }

    private static double cross(Point first, Point second, Point third) {
        double u = first.u();
        double v = first.v();
        return (second.u() - u) * (third.v() - v) - (second.v() - v) * (third.u() - u);
    }

    private static @Unmodifiable List<Point> compact(List<Point> input) {
        List<Point> points = new ArrayList<>(input);
        boolean changed = true;
        while (changed && points.size() >= 3) {
            changed = false;
            for (int index = 0; index < points.size(); index++) {
                Point previous = points.get((index + points.size() - 1) % points.size());
                Point current = points.get(index);
                Point next = points.get((index + 1) % points.size());
                if (Math.abs(cross(previous, current, next)) > EPSILON) {
                    continue;
                }

                points.remove(index);
                changed = true;
                break;
            }
        }
        return List.copyOf(points);
    }

    private static void merge(List<BeamRegion> regions) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int first = 0; first < regions.size() && !changed; first++) {
                for (int second = first + 1; second < regions.size(); second++) {
                    BeamRegion a = regions.get(first);
                    BeamRegion b = regions.get(second);
                    List<Point> points = new ArrayList<>(a.vertices());
                    points.addAll(b.vertices());
                    points = points.stream().distinct().sorted(Comparator.comparingDouble(Point::u).thenComparingDouble(Point::v)).toList();
                    List<Point> hull = new ArrayList<>();
                    for (Point point : points) {
                        while (hull.size() >= 2 && cross(hull.get(hull.size() - 2), hull.getLast(), point) <= EPSILON) {
                            hull.removeLast();
                        }
                        hull.add(point);
                    }
                    int lower = hull.size();
                    for (int index = points.size() - 2; index >= 0; index--) {
                        Point point = points.get(index);
                        while (hull.size() > lower && cross(hull.get(hull.size() - 2), hull.getLast(), point) <= EPSILON) {
                            hull.removeLast();
                        }
                        hull.add(point);
                    }
                    hull.removeLast();
                    BeamRegion combined = new BeamRegion(hull);
                    if (Math.abs(combined.area() - a.area() - b.area()) > EPSILON) {
                        continue;
                    }

                    regions.set(first, combined);
                    regions.remove(second);
                    changed = true;
                    break;
                }
            }
        }
    }

    public Area toArea() {
        Path2D.Double path = new Path2D.Double();
        if (vertices.isEmpty()) {
            return new Area();
        }

        Point first = vertices.getFirst();
        path.moveTo(first.u(), first.v());
        for (int index = 1; index < vertices.size(); index++) {
            Point point = vertices.get(index);
            path.lineTo(point.u(), point.v());
        }
        path.closePath();
        return new Area(path);
    }

    public double area() {
        return Math.abs(signedArea(vertices));
    }

    public Point center() {
        double u = 0;
        double v = 0;
        for (Point point : vertices) {
            u += point.u();
            v += point.v();
        }
        return new Point(u / vertices.size(), v / vertices.size());
    }

    public BeamRegion clip(double u, double v, double constant) {
        List<Point> clipped = new ArrayList<>();
        for (int index = 0; index < vertices.size(); index++) {
            Point first = vertices.get(index);
            Point second = vertices.get((index + 1) % vertices.size());
            double from = u * first.u() + v * first.v() + constant;
            double to = u * second.u() + v * second.v() + constant;
            if (from <= 0) {
                clipped.add(first);
            }
            if (from * to >= 0) {
                continue;
            }

            clipped.add(first.lerp(second, from / (from - to)));
        }
        return new BeamRegion(compact(clipped));
    }

    public record Point(double u, double v) {
        public Point lerp(Point other, double fraction) {
            return new Point(u + (other.u() - u) * fraction, v + (other.v() - v) * fraction);
        }
    }

    private record Edge(Point first, Point second) {
        private double at(double u) {
            double firstU = first.u();
            double firstV = first.v();
            return firstV + (second.v() - firstV) * (u - firstU) / (second.u() - firstU);
        }
    }
}
