package net.ty.createcraftedbeginning.content.opticalpower.laser.exposure;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalSurfaceLight.Qualification;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Density;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Patch;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.geom.Area;
import java.awt.geom.Path2D.Double;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalIllumination {
    public static final double MIN_QUALIFIED_AREA = 9.765625E-4;
    public static final double GROWTH_ILLUMINANCE_LX = 4096;
    private static final double GEOMETRY_EPSILON = 1.0E-9;

    private final Map<Surface, List<IlluminatedPatch>> surfaces = new HashMap<>();

    private static double coordinate(Vec3 point, Axis axis) {
        return axis.choose(point.x, point.y, point.z);
    }

    private static Point project(Vec3 point, Vec3 direction, Axis axis, double plane) {
        Vec3 projected = point.add(direction.scale((plane - coordinate(point, axis)) / coordinate(direction, axis)));
        return switch (axis) {
            case X -> new Point(projected.y, projected.z);
            case Y -> new Point(projected.x, projected.z);
            case Z -> new Point(projected.x, projected.y);
        };
    }

    private static double cross(Point first, Point second, Point third) {
        double x = first.x();
        double y = first.y();
        return (second.x() - x) * (third.y() - y) - (second.y() - y) * (third.x() - x);
    }

    private static Area polygon(List<Point> points) {
        List<Point> sorted = points.stream().distinct().sorted(Comparator.comparingDouble(Point::x).thenComparingDouble(Point::y)).toList();
        if (sorted.size() < 3) {
            return new Area();
        }

        List<Point> hull = new ArrayList<>();
        for (Point point : sorted) {
            while (hull.size() >= 2 && cross(hull.get(hull.size() - 2), hull.getLast(), point) <= 0) {
                hull.removeLast();
            }
            hull.add(point);
        }
        int lowerSize = hull.size();
        for (int index = sorted.size() - 2; index >= 0; index--) {
            Point point = sorted.get(index);
            while (hull.size() > lowerSize && cross(hull.get(hull.size() - 2), hull.getLast(), point) <= 0) {
                hull.removeLast();
            }
            hull.add(point);
        }
        Double path = new Double();
        Point first = hull.getFirst();
        path.moveTo(first.x(), first.y());
        for (int index = 1; index < hull.size(); index++) {
            Point point = hull.get(index);
            path.lineTo(point.x(), point.y());
        }
        path.closePath();
        return new Area(path);
    }

    private static Area projectBox(AABB box, Vec3 direction, Axis axis, double plane) {
        List<Point> points = new ArrayList<>(8);
        for (int corner = 0; corner < 8; corner++) {
            Vec3 point = new Vec3((corner & 1) == 0 ? box.minX : box.maxX, (corner & 2) == 0 ? box.minY : box.maxY, (corner & 4) == 0 ? box.minZ : box.maxZ);
            points.add(project(point, direction, axis, plane));
        }
        return polygon(points);
    }

    private static AABB slice(AABB box, Axis axis, double minimum, double maximum) {
        return switch (axis) {
            case X -> new AABB(minimum, box.minY, box.minZ, maximum, box.maxY, box.maxZ);
            case Y -> new AABB(box.minX, minimum, box.minZ, box.maxX, maximum, box.maxZ);
            case Z -> new AABB(box.minX, box.minY, minimum, box.maxX, box.maxY, maximum);
        };
    }

    private static double area(Area region) {
        PathIterator iterator = region.getPathIterator(null);
        double[] coordinates = new double[6];
        double startX = 0;
        double startY = 0;
        double previousX = 0;
        double previousY = 0;
        double twiceArea = 0;
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coordinates);
            if (type == PathIterator.SEG_MOVETO) {
                startX = coordinates[0];
                startY = coordinates[1];
            }
            else if (type == PathIterator.SEG_LINETO) {
                twiceArea += previousX * coordinates[1] - coordinates[0] * previousY;
            }
            else if (type == PathIterator.SEG_CLOSE) {
                twiceArea += previousX * startY - startX * previousY;
            }
            previousX = coordinates[0];
            previousY = coordinates[1];
            iterator.next();
        }
        return Math.abs(twiceArea) / 2;
    }

    public void add(OpticalExposure exposure, int group, VoxelShape receivingShape, VoxelShape collisionShape) {
        BeamState incident = exposure.incident();
        double absorbedFlux = exposure.absorbedFluxLm();
        if (incident.fluxLm() <= 0 || absorbedFlux <= 0) {
            return;
        }

        BeamTerminal terminal = exposure.terminal();
        if (terminal != null) {
            for (Face face : terminal.faces()) {
                addContacts(exposure.absorbedFluxLm(), face.direction(), face.plane(), face.patches(), group, receivingShape, collisionShape);
            }
            return;
        }

        if (!exposure.contactPatches().isEmpty()) {
            addContacts(exposure, group, receivingShape, collisionShape);
            return;
        }

        BeamFrame frame = exposure.localFrame();
        Vec3 direction = frame.direction();
        BeamProfile profile = incident.profile();
        BeamFootprint footprint = incident.footprint();
        double radius = profile.radius();
        Vec3 horizontal = frame.transverseAxis().scale(radius * footprint.horizontalScale());
        Vec3 vertical = frame.getSecondTransverseAxis().scale(radius * footprint.verticalScale());
        Vec3 origin = frame.origin();
        double density = absorbedFlux / (profile.getArea() * footprint.areaFraction());
        List<AABB> obstacles = collisionShape.toAabbs();
        Map<Surface, Area> visible = new HashMap<>();
        for (AABB box : receivingShape.toAabbs()) {
            for (Direction face : Iterate.directions) {
                Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
                if (direction.dot(normal) >= -GEOMETRY_EPSILON) {
                    continue;
                }

                Axis axis = face.getAxis();
                double plane = box.min(axis);
                if (face.getAxisDirection() == AxisDirection.POSITIVE) {
                    plane = box.max(axis);
                }
                Area region = projectBox(slice(box, axis, plane, plane), direction, axis, plane);
                for (AABB obstacle : obstacles) {
                    double minimum = obstacle.min(axis);
                    double maximum = obstacle.max(axis);
                    if (coordinate(direction, axis) < 0) {
                        minimum = Math.max(minimum, plane);
                    }
                    else {
                        maximum = Math.min(maximum, plane);
                    }
                    if (maximum - minimum <= GEOMETRY_EPSILON) {
                        continue;
                    }

                    region.subtract(projectBox(slice(obstacle, axis, minimum, maximum), direction, axis, plane));
                }
                Surface surface = new Surface(group, face, plane);
                visible.computeIfAbsent(surface, ignored -> new Area()).add(region);
            }
        }
        for (Entry<Surface, Area> entry : visible.entrySet()) {
            Surface surface = entry.getKey();
            Direction face = surface.face();
            Axis axis = face.getAxis();
            double plane = surface.plane();
            List<Point> corners = new ArrayList<>(4);
            corners.add(project(origin.add(horizontal).add(vertical), direction, axis, plane));
            corners.add(project(origin.add(horizontal).subtract(vertical), direction, axis, plane));
            corners.add(project(origin.subtract(horizontal).add(vertical), direction, axis, plane));
            corners.add(project(origin.subtract(horizontal).subtract(vertical), direction, axis, plane));
            Area projectedFootprint = polygon(corners);
            projectedFootprint.intersect(entry.getValue());
            if (projectedFootprint.isEmpty()) {
                continue;
            }

            addPatch(surface, projectedFootprint, density * Math.abs(coordinate(direction, axis)));
        }
    }

    public double getFluxLm() {
        double flux = 0;
        for (Entry<Surface, List<IlluminatedPatch>> entry : surfaces.entrySet()) {
            Surface surface = entry.getKey();
            for (IlluminatedPatch patch : entry.getValue()) {
                for (OpticalSurfaceLight light : patch.lights()) {
                    flux += light.flux(patch.region(), surface.face().getAxis(), surface.plane());
                }
            }
        }
        return flux;
    }

    public double getQualifiedArea(int group, double thresholdLx) {
        double qualifiedArea = 0;
        for (Entry<Surface, List<IlluminatedPatch>> entry : surfaces.entrySet()) {
            Surface surface = entry.getKey();
            if (surface.group() != group) {
                continue;
            }

            Direction face = surface.face();
            Axis axis = face.getAxis();
            Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
            for (IlluminatedPatch patch : entry.getValue()) {
                qualifiedArea += area(OpticalSurfaceLight.qualified(patch.region(), patch.lights(), axis, surface.plane(), normal, thresholdLx).region());
            }
        }
        return qualifiedArea;
    }

    public double getQualifiedFluxLm(int group, double thresholdLx) {
        double qualifiedArea = 0;
        double flux = 0;
        for (Entry<Surface, List<IlluminatedPatch>> entry : surfaces.entrySet()) {
            Surface surface = entry.getKey();
            if (surface.group() != group) {
                continue;
            }

            Direction face = surface.face();
            Axis axis = face.getAxis();
            Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
            double plane = surface.plane();
            for (IlluminatedPatch patch : entry.getValue()) {
                List<OpticalSurfaceLight> lights = patch.lights();
                Area qualified = OpticalSurfaceLight.qualified(patch.region(), lights, axis, plane, normal, thresholdLx).region();
                qualifiedArea += area(qualified);
                for (OpticalSurfaceLight light : lights) {
                    flux += light.flux(qualified, axis, plane);
                }
            }
        }
        if (qualifiedArea + GEOMETRY_EPSILON < MIN_QUALIFIED_AREA) {
            return 0;
        }

        return flux;
    }

    public QualificationResult getQualification(int group, double thresholdLx) {
        double qualifiedArea = 0;
        double flux = 0;
        double unresolvedArea = 0;
        for (Entry<Surface, List<IlluminatedPatch>> entry : surfaces.entrySet()) {
            Surface surface = entry.getKey();
            if (surface.group() != group) {
                continue;
            }

            Direction face = surface.face();
            Axis axis = face.getAxis();
            Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
            double plane = surface.plane();
            for (IlluminatedPatch patch : entry.getValue()) {
                List<OpticalSurfaceLight> lights = patch.lights();
                Qualification result = OpticalSurfaceLight.qualified(patch.region(), lights, axis, plane, normal, thresholdLx);
                Area qualified = result.region();
                qualifiedArea += area(qualified);
                unresolvedArea += result.unresolvedArea();
                for (OpticalSurfaceLight light : lights) {
                    flux += light.flux(qualified, axis, plane);
                }
            }
        }
        if (qualifiedArea + GEOMETRY_EPSILON < MIN_QUALIFIED_AREA) {
            return new QualificationResult(qualifiedArea, 0, unresolvedArea);
        }

        return new QualificationResult(qualifiedArea, flux, unresolvedArea);
    }

    private void addContacts(OpticalExposure exposure, int group, VoxelShape receivingShape, VoxelShape collisionShape) {
        Direction face = exposure.hit().localHit().getDirection();
        double plane = coordinate(exposure.localFrame().origin(), face.getAxis());
        addContacts(exposure.absorbedFluxLm(), face, plane, exposure.contactPatches(), group, receivingShape, collisionShape);
    }

    private void addContacts(double absorbedFlux, Direction face, double plane, List<Patch> patches, int group, VoxelShape receivingShape, VoxelShape collisionShape) {
        Axis axis = face.getAxis();
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        AxisDirection axisDirection = face.getAxisDirection();
        Area visible = new Area();
        for (AABB box : receivingShape.toAabbs()) {
            double boundary = axisDirection == AxisDirection.POSITIVE ? box.max(axis) : box.min(axis);
            if (Math.abs(boundary - plane) > GEOMETRY_EPSILON) {
                continue;
            }

            visible.add(projectBox(slice(box, axis, plane, plane), normal, axis, plane));
        }
        for (AABB obstacle : collisionShape.toAabbs()) {
            boolean outside = axisDirection == AxisDirection.POSITIVE ? obstacle.min(axis) <= plane + GEOMETRY_EPSILON && obstacle.max(axis) > plane + GEOMETRY_EPSILON : obstacle.max(axis) >= plane - GEOMETRY_EPSILON && obstacle.min(axis) < plane - GEOMETRY_EPSILON;
            if (!outside) {
                continue;
            }

            visible.subtract(projectBox(slice(obstacle, axis, plane, plane), normal, axis, plane));
        }
        Surface surface = new Surface(group, face, plane);
        for (Patch patch : patches) {
            List<Point> vertices = patch.vertices().stream().map(vertex -> project(vertex, normal, axis, plane)).toList();
            Area region = polygon(vertices);
            double fullArea = area(region);
            if (fullArea <= 0) {
                continue;
            }

            region.intersect(visible);
            if (region.isEmpty()) {
                continue;
            }

            Density density = patch.density();
            if (density != null) {
                addPatch(surface, region, new OpticalSurfaceLight(absorbedFlux, density));
                continue;
            }

            addPatch(surface, region, absorbedFlux * patch.fluxFraction() / fullArea);
        }
    }

    private void addPatch(Surface surface, Area region, double illuminanceLx) {
        addPatch(surface, region, new OpticalSurfaceLight(illuminanceLx, null));
    }

    private void addPatch(Surface surface, Area region, OpticalSurfaceLight light) {
        List<IlluminatedPatch> previous = surfaces.computeIfAbsent(surface, ignored -> new ArrayList<>());
        List<IlluminatedPatch> combined = new ArrayList<>();
        Area remaining = new Area(region);
        for (IlluminatedPatch patch : previous) {
            Area previousRegion = patch.region();
            List<OpticalSurfaceLight> previousLights = patch.lights();
            Area overlap = new Area(previousRegion);
            overlap.intersect(region);
            Area outside = new Area(previousRegion);
            outside.subtract(region);
            if (!outside.isEmpty()) {
                combined.add(new IlluminatedPatch(outside, previousLights));
            }
            if (!overlap.isEmpty()) {
                List<OpticalSurfaceLight> lights = new ArrayList<>(previousLights);
                lights.add(light);
                combined.add(new IlluminatedPatch(overlap, List.copyOf(lights)));
            }
            remaining.subtract(previousRegion);
        }
        if (!remaining.isEmpty()) {
            combined.add(new IlluminatedPatch(remaining, List.of(light)));
        }
        surfaces.put(surface, combined);
    }

    public record QualificationResult(double qualifiedArea, double qualifiedFluxLm, double unresolvedArea) {
        public boolean isComplete() {
            return unresolvedArea == 0;
        }
    }

    private record Point(double x, double y) {}

    private record Surface(int group, Direction face, double plane) {}

    private record IlluminatedPatch(Area region, List<OpticalSurfaceLight> lights) {}
}
