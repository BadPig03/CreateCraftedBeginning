package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile.Span;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Density;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Patch;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BeamTerminalQuery {
    private static final double EPSILON = 1.0E-9;
    private static final int MAX_PATCHES = 4096;

    private final BeamState beam;
    private final BeamState relativeBeam;
    private final Vec3 localOrigin;
    private final LaserQuerySpace space;
    private final Vec3 anchor;
    private final List<BeamSweepSection> volumes;
    private final List<AABB> obstacles;
    private final List<AABB> receiving;
    private final int budget;
    private int tests;

    BeamTerminalQuery(BeamState beam, LaserQuerySpace space, BlockPos pos, double range, @Nullable OpticalPlane startPlane, List<AABB> obstacles, List<AABB> receiving, int budget) {
        this.beam = beam;
        this.space = space;
        anchor = Vec3.atLowerCornerOf(pos);
        BeamFrame frame = beam.frame();
        relativeBeam = new BeamState(new BeamFrame(Vec3.ZERO, frame.direction(), frame.transverseAxis()), beam.profile(), beam.fluxLm(), beam.footprint());
        localOrigin = space.localPosition(frame.origin()).subtract(anchor);
        List<BeamSweepSection> sections = new ArrayList<>();
        OpticalPlane localStart = null;
        if (startPlane != null) {
            localStart = new OpticalPlane(space.localPosition(startPlane.point()).subtract(anchor), covector(startPlane.normal()));
        }
        for (BeamSweepSection section : BeamSweepSection.create(relativeBeam, range, null)) {
            BeamSweepSection local = section.transform(point -> localOrigin.add(space.localVector(point)));
            if (localStart != null) {
                local = local.clip(localStart);
            }
            if (local == null) {
                continue;
            }

            sections.add(local);
        }
        volumes = List.copyOf(sections);
        this.obstacles = obstacles;
        this.receiving = receiving;
        this.budget = budget;
    }

    private static double cross(Point first, Point second, Point third) {
        return (second.u() - first.u()) * (third.v() - first.v()) - (second.v() - first.v()) * (third.u() - first.u());
    }

    int tests() {
        return tests;
    }

    @Nullable BeamTerminal resolve() {
        List<Face> faces = new ArrayList<>();
        Area covered = new Area();
        BeamFrame frame = beam.frame();
        Vec3 direction = space.localVector(frame.direction());
        Vec3 horizontal = space.localVector(frame.transverseAxis());
        Vec3 vertical = space.localVector(frame.getSecondTransverseAxis());
        double sourceArea = beam.footprint().domain().area();
        int patchCount = 0;
        for (AABB box : receiving) {
            for (Direction face : Iterate.directions) {
                Axis axis = face.getAxis();
                Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
                double plane = face.getAxisDirection() == AxisDirection.POSITIVE ? box.max(axis) : box.min(axis);
                Vec3 center = box.getCenter();
                Vec3 planePoint = center.add(normal.scale((plane - axis.choose(center.x, center.y, center.z)) * face.getAxisDirection().getStep()));
                AABB surfaceBox = switch (axis) {
                    case X -> new AABB(plane, box.minY, box.minZ, plane, box.maxY, box.maxZ);
                    case Y -> new AABB(box.minX, plane, box.minZ, box.maxX, plane, box.maxZ);
                    case Z -> new AABB(box.minX, box.minY, plane, box.maxX, box.maxY, plane);
                };
                for (int spanIndex = 0; spanIndex < volumes.size(); spanIndex++) {
                    BeamSweepSection volume = volumes.get(spanIndex);
                    if (tests++ >= budget) {
                        return null;
                    }

                    BeamSweepSection contact = volume.clip(surfaceBox);
                    if (contact == null) {
                        continue;
                    }

                    Span span = volume.span();
                    double slope = span.radiusSlope();
                    Area visible = project(contact.points().stream().map(point -> point.subtract(normal.scale(point.subtract(planePoint).dot(normal)))).toList()).clip(slope * horizontal.dot(normal), slope * vertical.dot(normal), direction.dot(normal) + EPSILON).toArea();
                    visible.intersect(beam.footprint().domain().toArea());
                    if (visible.isEmpty()) {
                        continue;
                    }

                    for (int earlier = 0; earlier <= spanIndex; earlier++) {
                        BeamSweepSection upstream = volumes.get(earlier);
                        if (earlier == spanIndex) {
                            upstream = upstream.clip(new OpticalPlane(planePoint.add(normal.scale(2 * BeamSweepSection.CONTACT_TOLERANCE)), normal));
                        }
                        if (upstream == null) {
                            continue;
                        }

                        for (AABB obstacle : obstacles) {
                            if (tests++ >= budget) {
                                return null;
                            }

                            BeamSweepSection shadow = upstream.clip(obstacle);
                            if (shadow == null) {
                                continue;
                            }

                            visible.subtract(project(shadow.points()).toArea());
                        }
                    }
                    visible.subtract(covered);
                    if (visible.isEmpty() || BeamRegion.area(visible) <= 1.0E-12) {
                        continue;
                    }

                    List<BeamRegion> regions = BeamRegion.partition(visible, MAX_PATCHES - patchCount);
                    if (regions.isEmpty()) {
                        return null;
                    }

                    List<Patch> patches = new ArrayList<>();
                    Density density = null;
                    if (slope != 0) {
                        double radius = beam.profile().advance(span.from()).radius();
                        double initialRadius = beam.profile().radius();
                        Vec3 focus = frame.direction().scale(span.from() - radius / slope).subtract(frame.transverseAxis().scale(initialRadius * beam.footprint().horizontalOffset())).subtract(frame.getSecondTransverseAxis().scale(initialRadius * beam.footprint().verticalOffset()));
                        density = new Density(localOrigin.add(space.localVector(focus)), covector(frame.direction()), covector(frame.transverseAxis()), covector(frame.getSecondTransverseAxis()), slope, 1 / sourceArea);
                    }
                    for (BeamRegion region : regions) {
                        List<Vec3> vertices = new ArrayList<>();
                        for (Point point : region.vertices()) {
                            Vec3 start = localOrigin.add(space.localVector(BeamContact.point(relativeBeam, span.from(), point.u(), point.v())));
                            Vec3 end = localOrigin.add(space.localVector(BeamContact.point(relativeBeam, span.to(), point.u(), point.v())));
                            Vec3 delta = end.subtract(start);
                            double fraction = planePoint.subtract(start).dot(normal) / delta.dot(normal);
                            vertices.add(start.add(delta.scale(fraction)));
                        }
                        Patch patch = new Patch(vertices, region.area() / sourceArea, density);
                        if (patch.area() <= EPSILON * EPSILON) {
                            continue;
                        }

                        patches.add(patch);
                    }
                    patchCount += patches.size();
                    covered.add(visible);
                    faces.add(new Face(face, plane, patches));
                }
            }
        }
        return new BeamTerminal(faces, space.transformPosition(anchor), space.x(), space.y(), space.z());
    }

    private Vec3 covector(Vec3 vector) {
        return new Vec3(space.x().dot(vector), space.y().dot(vector), space.z().dot(vector));
    }

    private BeamRegion project(List<Vec3> vertices) {
        BeamFrame frame = beam.frame();
        double initialRadius = beam.profile().radius();
        Vec3 direction = covector(frame.direction());
        Vec3 horizontal = covector(frame.transverseAxis());
        Vec3 vertical = covector(frame.getSecondTransverseAxis());
        List<Point> points = new ArrayList<>();
        for (Vec3 local : vertices) {
            Vec3 delta = local.subtract(localOrigin);
            double distance = Math.max(0, delta.dot(direction));
            double radius = beam.profile().advance(distance).radius();
            points.add(new Point((delta.dot(horizontal) + initialRadius * beam.footprint().horizontalOffset()) / radius, (delta.dot(vertical) + initialRadius * beam.footprint().verticalOffset()) / radius));
        }
        points = points.stream().distinct().sorted(Comparator.comparingDouble(Point::u).thenComparingDouble(Point::v)).toList();
        if (points.size() < 3) {
            return new BeamRegion(List.of());
        }

        List<Point> hull = new ArrayList<>();
        for (Point point : points) {
            while (hull.size() >= 2 && cross(hull.get(hull.size() - 2), hull.getLast(), point) <= 0) {
                hull.removeLast();
            }
            hull.add(point);
        }
        int lower = hull.size();
        for (int index = points.size() - 2; index >= 0; index--) {
            Point point = points.get(index);
            while (hull.size() > lower && cross(hull.get(hull.size() - 2), hull.getLast(), point) <= 0) {
                hull.removeLast();
            }
            hull.add(point);
        }
        hull.removeLast();
        return new BeamRegion(hull);
    }
}
