package net.ty.createcraftedbeginning.content.opticalpower.laser.exposure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Density;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record OpticalSurfaceLight(double value, @Nullable Density density) {
    private static final double DENSITY_TOLERANCE = 0.001;
    private static final double ILLUMINANCE_RELATIVE_TOLERANCE = 1.0E-9;
    private static final double NORMAL_ALIGNMENT_TOLERANCE = 1.0E-9;
    private static final double DIRECTION_TOLERANCE_SQR = 1.0E-12;
    private static final int MAX_ILLUMINANCE_BISECTION_STEPS = 52;
    private static final int MAX_PARTS = 4096;

    static Vec3 point(Point point, Axis axis, double plane) {
        return switch (axis) {
            case X -> new Vec3(plane, point.u(), point.v());
            case Y -> new Vec3(point.u(), plane, point.v());
            case Z -> new Vec3(point.u(), point.v(), plane);
        };
    }

    static List<List<Point>> rings(Area area) {
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

    static Qualification qualified(Area area, List<OpticalSurfaceLight> lights, Axis axis, double plane, Vec3 normal, double threshold) {
        if (threshold <= 0) {
            return new Qualification(area, 0);
        }

        if (lights.stream().allMatch(light -> light.density() == null)) {
            double illuminance = lights.stream().mapToDouble(OpticalSurfaceLight::value).sum();
            if (illuminance < threshold * (1 - ILLUMINANCE_RELATIVE_TOLERANCE)) {
                return new Qualification(new Area(), 0);
            }

            return new Qualification(area, 0);
        }

        Area result = new Area();
        ArrayDeque<BeamRegion> pending = new ArrayDeque<>(BeamRegion.partition(area, MAX_PARTS));
        int parts = pending.size();
        double unresolvedArea = 0;
        if (pending.isEmpty() && !area.isEmpty()) {
            return new Qualification(new Area(), BeamRegion.area(area));
        }

        while (!pending.isEmpty()) {
            BeamRegion region = pending.removeFirst();
            List<Point> vertices = region.vertices();
            double minimum = 0;
            double maximum = 0;
            for (OpticalSurfaceLight light : lights) {
                double low = Double.POSITIVE_INFINITY;
                double high = 0;
                for (Point vertex : vertices) {
                    double illuminance = light.at(point(vertex, axis, plane), normal);
                    low = Math.min(low, illuminance);
                    high = Math.max(high, illuminance);
                }
                minimum += low;
                maximum += high;
            }
            if (maximum < threshold * (1 - ILLUMINANCE_RELATIVE_TOLERANCE)) {
                continue;
            }

            if (minimum >= threshold * (1 - ILLUMINANCE_RELATIVE_TOLERANCE)) {
                result.add(region.toArea());
                continue;
            }

            Vec3 varying = null;
            boolean aligned = true;
            for (OpticalSurfaceLight light : lights) {
                Density density = light.density();
                if (density == null) {
                    continue;
                }

                Vec3 direction = density.direction().normalize();
                if (density.slope() < 0) {
                    direction = direction.scale(-1);
                }
                if (Math.abs(direction.dot(normal)) >= 1 - NORMAL_ALIGNMENT_TOLERANCE) {
                    continue;
                }

                if (varying == null) {
                    varying = direction;
                    continue;
                }

                if (varying.distanceToSqr(direction) <= DIRECTION_TOLERANCE_SQR) {
                    continue;
                }

                aligned = false;
            }
            if (aligned && varying != null) {
                double u = varying.dot(point(new Point(1, 0), axis, 0));
                double v = varying.dot(point(new Point(0, 1), axis, 0));
                Point first = vertices.getFirst();
                Point last = first;
                for (Point vertex : vertices) {
                    double coordinate = u * vertex.u() + v * vertex.v();
                    if (coordinate < u * first.u() + v * first.v()) {
                        first = vertex;
                    }
                    if (coordinate <= u * last.u() + v * last.v()) {
                        continue;
                    }

                    last = vertex;
                }
                double low = 0;
                double high = 1;
                for (int step = 0; step < MAX_ILLUMINANCE_BISECTION_STEPS; step++) {
                    double middle = (low + high) / 2;
                    Vec3 position = point(first.lerp(last, middle), axis, plane);
                    double illuminance = 0;
                    for (OpticalSurfaceLight light : lights) {
                        illuminance += light.at(position, normal);
                    }
                    if (illuminance < threshold) {
                        high = middle;
                        continue;
                    }

                    low = middle;
                }
                Point boundary = first.lerp(last, (low + high) / 2);
                result.add(region.clip(u, v, -u * boundary.u() - v * boundary.v()).toArea());
                continue;
            }

            if (maximum <= minimum * (1 + DENSITY_TOLERANCE)) {
                Vec3 position = point(region.center(), axis, plane);
                double illuminance = 0;
                for (OpticalSurfaceLight light : lights) {
                    illuminance += light.at(position, normal);
                }
                if (illuminance >= threshold) {
                    result.add(region.toArea());
                }
                continue;
            }

            if (parts >= MAX_PARTS) {
                unresolvedArea += region.area();
                continue;
            }

            double minU = vertices.stream().mapToDouble(Point::u).min().orElseThrow();
            double maxU = vertices.stream().mapToDouble(Point::u).max().orElseThrow();
            double minV = vertices.stream().mapToDouble(Point::v).min().orElseThrow();
            double maxV = vertices.stream().mapToDouble(Point::v).max().orElseThrow();
            double u = maxU - minU >= maxV - minV ? 1 : 0;
            double v = 1 - u;
            double middle = (u * (minU + maxU) + v * (minV + maxV)) / 2;
            pending.addLast(region.clip(u, v, -middle));
            pending.addLast(region.clip(-u, -v, middle));
            parts++;
        }
        return new Qualification(result, unresolvedArea);
    }

    double at(Vec3 point, Vec3 normal) {
        if (density == null) {
            return value;
        }

        return value * density.at(point, normal);
    }

    double flux(Area region, Axis axis, double plane) {
        if (density == null) {
            return value * BeamRegion.area(region);
        }

        double area = 0;
        for (List<Point> ring : rings(region)) {
            Point first = density.project(point(ring.getFirst(), axis, plane));
            Point previous = first;
            double firstU = first.u();
            double firstV = first.v();
            for (int index = 1; index < ring.size(); index++) {
                Point next = density.project(point(ring.get(index), axis, plane));
                area += (previous.u() - firstU) * (next.v() - firstV) - (next.u() - firstU) * (previous.v() - firstV);
                previous = next;
            }
        }
        return value * density.fluxPerSourceArea() * Math.abs(area) / 2;
    }

    record Qualification(Area region, double unresolvedArea) {
    }
}
