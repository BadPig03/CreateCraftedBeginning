package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplayFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplaySegment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplayState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.SamplingDomain;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamParticlePaths {
    private static final double EPSILON = 1.0E-9;
    private final List<Path> paths;
    private final double length;

    LaserBeamParticlePaths(@Nullable LaserBeamContext context, List<DrawSection> sections) {
        List<Path> paths = new ArrayList<>();
        double length = 0;
        for (DrawSection draw : sections) {
            Section geometry = draw.geometry();
            List<Vec3> starts = geometry.starts();
            List<Vec3> ends = geometry.ends();
            Vec3 origin = Section.center(starts);
            Vec3 direction = Section.center(ends).subtract(origin).normalize();
            Vec3 transverse = Vec3.ZERO;
            double offset = draw.pathDistance();
            List<Point> polygon = polygon(draw);
            LaserBeamDisplayProfile profile = null;
            boolean reflected = false;
            int segmentIndex = draw.segmentIndex();
            if (context != null && segmentIndex >= 0 && segmentIndex < context.segments().size()) {
                List<DisplaySegment> segments = context.segments();
                DisplaySegment segment = segments.get(segmentIndex);
                DisplayState start = segment.start();
                DisplayFrame frame = start.frame();
                Vec3 transverseAxis = frame.transverseAxis();
                direction = context.toLocalDirection(frame.direction()).normalize();
                transverse = context.toLocalDirection(transverseAxis).normalize();
                origin = context.toLocalPosition(frame.origin());
                offset = 0;
                int parent = segment.parentIndex();
                DisplaySegment child = segment;
                while (parent >= 0) {
                    DisplaySegment previous = segments.get(parent);
                    offset += previous.length();
                    OpticalPlane contact = previous.contact();
                    if (contact != null && previous.start().frame().direction().dot(contact.normal()) * child.start().frame().direction().dot(contact.normal()) < 0) {
                        reflected = !reflected;
                    }

                    child = previous;
                    parent = previous.parentIndex();
                }

                if (start.profile().widthSlope() != 0 || start.footprint().region() != null) {
                    LaserBeamDisplayProfile world = LaserBeamDisplayProfile.fromBeam(start);
                    profile = new LaserBeamDisplayProfile(context.toLocalPosition(world.origin()), direction, context.toLocalDirection(world.transverse()), world.physical());
                }

                polygon = align(polygon, geometry, context.toLocalDirection(transverseAxis), context.toLocalDirection(frame.getSecondTransverseAxis()));
                if (reflected) {
                    polygon = polygon.stream().map(point -> new Point(point.u(), -point.v())).toList();
                }

            }

            if (polygon.size() != starts.size() || polygon.size() != ends.size() || polygon.size() < 3) {
                continue;
            }

            List<OpticalPlane> caps = new ArrayList<>(2);
            for (List<Vec3> cap : List.of(starts, ends)) {
                Vec3 first = cap.getFirst();
                Vec3 normal = cap.get(1).subtract(first).cross(cap.getLast().subtract(first)).normalize();
                if (normal.lengthSqr() < EPSILON && cap.stream().allMatch(point -> point.distanceToSqr(first) < EPSILON)) {
                    normal = direction;
                }

                caps.add(new OpticalPlane(first, normal));
            }

            List<OpticalPlane> planes = List.copyOf(caps);

            Path path = new Path(draw, polygon, origin, direction, transverse, offset, planes, profile, reflected);
            paths.add(path);
            for (Vec3 end : ends) {
                length = Math.max(length, offset + end.subtract(origin).dot(direction));
            }
        }

        this.paths = List.copyOf(paths);
        this.length = length;
    }

    private static List<Point> polygon(DrawSection draw) {
        SamplingDomain domain = draw.samplingDomain();
        double left = 2 * domain.minU() - 1;
        double right = 2 * domain.maxU() - 1;
        double bottom = 2 * domain.minV() - 1;
        double top = 2 * domain.maxV() - 1;
        BeamRegion region = domain.region();
        if (region == null) {
            return BeamRegion.rectangle(left, bottom, right, top).vertices();
        }

        return region.clip(-1, 0, left).clip(1, 0, -right).clip(0, -1, bottom).clip(0, 1, -top).vertices();
    }

    private static List<Point> align(List<Point> polygon, Section geometry, Vec3 transverse, Vec3 second) {
        int count = polygon.size();
        List<Vec3> cap = geometry.starts();
        if (count != cap.size()) {
            return polygon;
        }

        Vec3 center = Section.center(cap);
        Vec3 startCenter = center;
        if (cap.stream().allMatch(point -> point.distanceToSqr(startCenter) < EPSILON)) {
            cap = geometry.ends();
            center = Section.center(cap);
        }

        double best = Double.NEGATIVE_INFINITY;
        int rotation = 0;
        int winding = 1;
        for (int sign : new int[]{1, -1}) {
            for (int shift = 0; shift < count; shift++) {
                double score = 0;
                for (int index = 0; index < count; index++) {
                    Point point = polygon.get(Math.floorMod(shift + sign * index, count));
                    Vec3 delta = cap.get(index).subtract(center);
                    score += point.u() * delta.dot(transverse) + point.v() * delta.dot(second);
                }

                if (!(score > best)) {
                    continue;
                }

                best = score;
                rotation = shift;
                winding = sign;
            }
        }

        List<Point> aligned = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            aligned.add(polygon.get(Math.floorMod(rotation + winding * index, count)));
        }

        return List.copyOf(aligned);
    }

    double length() {
        return length;
    }

    List<Path> paths() {
        return paths;
    }

    static final class Path {
        private final DrawSection draw;
        private final Vec3 origin;
        private final Vec3 direction;
        private final Vec3 transverse;
        private final Vec3 secondAxis;
        private final double offset;
        private final List<OpticalPlane> endPlanes;
        @Nullable
        private final LaserBeamDisplayProfile profile;
        private final boolean reflected;
        private final Triangle[] triangles;
        private final double minDistance;
        private final double maxDistance;

        private Path(DrawSection draw, List<Point> polygon, Vec3 origin, Vec3 direction, Vec3 transverse, double offset, List<OpticalPlane> endPlanes, @Nullable LaserBeamDisplayProfile profile, boolean reflected) {
            this.draw = draw;
            this.origin = origin;
            this.direction = direction;
            this.transverse = transverse;
            secondAxis = direction.cross(transverse);
            this.offset = offset;
            this.endPlanes = endPlanes;
            this.profile = profile;
            this.reflected = reflected;
            Section geometry = draw.geometry();
            List<Triangle> triangles = new ArrayList<>(polygon.size() - 2);
            for (int index = 1; index + 1 < polygon.size(); index++) {
                Triangle triangle = new Triangle(polygon, geometry, index);
                if (Math.abs(triangle.determinant) < EPSILON) {
                    continue;
                }

                triangles.add(triangle);
            }

            this.triangles = triangles.toArray(Triangle[]::new);
            double from = Double.POSITIVE_INFINITY;
            double to = Double.NEGATIVE_INFINITY;
            double coordinateScale = 0;
            for (int index = 0; index < polygon.size(); index++) {
                Vec3 start = geometry.starts().get(index);
                Vec3 end = geometry.ends().get(index);
                from = Math.min(from, offset + start.subtract(origin).dot(direction));
                to = Math.max(to, offset + end.subtract(origin).dot(direction));
                coordinateScale = Math.max(coordinateScale, Math.abs(start.x * direction.x) + Math.abs(start.y * direction.y) + Math.abs(start.z * direction.z));
                coordinateScale = Math.max(coordinateScale, Math.abs(end.x * direction.x) + Math.abs(end.y * direction.y) + Math.abs(end.z * direction.z));
            }

            coordinateScale += Math.abs(offset) + Math.abs(origin.x * direction.x) + Math.abs(origin.y * direction.y) + Math.abs(origin.z * direction.z);
            double padding = 8 * EPSILON * Math.max(1, coordinateScale);
            minDistance = from - padding;
            maxDistance = to + padding;
        }

        DrawSection draw() {
            return draw;
        }

        Vec3 direction() {
            return direction;
        }

        List<OpticalPlane> endPlanes() {
            return endPlanes;
        }

        Vec3 radialDirection(double u, double v) {
            if (reflected) {
                v = -v;
            }

            Vec3 radial = transverse.scale(u).add(secondAxis.scale(v));
            double length = radial.length();
            if (length < EPSILON) {
                return transverse;
            }

            return radial.scale(1 / length);
        }

        double radialLimit(Vec3 position, Vec3 radial) {
            double limit = Double.POSITIVE_INFINITY;
            for (OpticalPlane plane : endPlanes) {
                Vec3 normal = plane.normal();
                double rate = radial.dot(normal);
                if (Math.abs(rate) < EPSILON) {
                    continue;
                }

                Vec3 origin = plane.point();
                double distance = ((origin.x - position.x) * normal.x + (origin.y - position.y) * normal.y + (origin.z - position.z) * normal.z) / rate;
                if (distance < 0) {
                    continue;
                }

                limit = Math.min(limit, distance);
            }

            return limit;
        }

        @Nullable Vec3 sample(double u, double v, double distance) {
            if (distance < minDistance || distance >= maxDistance) {
                return null;
            }

            for (Triangle triangle : triangles) {
                double relativeU = u - triangle.first.u();
                double relativeV = v - triangle.first.v();
                double b = (relativeU * triangle.thirdV - relativeV * triangle.thirdU) / triangle.determinant;
                double c = (triangle.secondU * relativeV - triangle.secondV * relativeU) / triangle.determinant;
                double a = 1 - b - c;
                if (a < -EPSILON || b < -EPSILON || c < -EPSILON) {
                    continue;
                }

                double startX = triangle.startFirst.x * a + triangle.startSecond.x * b + triangle.startThird.x * c;
                double startY = triangle.startFirst.y * a + triangle.startSecond.y * b + triangle.startThird.y * c;
                double startZ = triangle.startFirst.z * a + triangle.startSecond.z * b + triangle.startThird.z * c;
                double endX = triangle.endFirst.x * a + triangle.endSecond.x * b + triangle.endThird.x * c;
                double endY = triangle.endFirst.y * a + triangle.endSecond.y * b + triangle.endThird.y * c;
                double endZ = triangle.endFirst.z * a + triangle.endSecond.z * b + triangle.endThird.z * c;
                double from = offset + ((startX - origin.x) * direction.x + (startY - origin.y) * direction.y + (startZ - origin.z) * direction.z);
                double to = offset + ((endX - origin.x) * direction.x + (endY - origin.y) * direction.y + (endZ - origin.z) * direction.z);
                if (distance < from || distance >= to || to - from < EPSILON) {
                    return null;
                }

                if (profile != null) {
                    if (reflected) {
                        return profile.point(distance - offset, u, -v);
                    }

                    return profile.point(distance - offset, u, v);
                }

                double progress = (distance - from) / (to - from);
                return new Vec3(startX + progress * (endX - startX), startY + progress * (endY - startY), startZ + progress * (endZ - startZ));
            }

            return null;
        }
    }

    private static final class Triangle {
        private final Point first;
        private final double secondU;
        private final double secondV;
        private final double thirdU;
        private final double thirdV;
        private final double determinant;
        private final Vec3 startFirst;
        private final Vec3 startSecond;
        private final Vec3 startThird;
        private final Vec3 endFirst;
        private final Vec3 endSecond;
        private final Vec3 endThird;

        private Triangle(List<Point> polygon, Section geometry, int index) {
            first = polygon.getFirst();
            Point second = polygon.get(index);
            Point third = polygon.get(index + 1);
            secondU = second.u() - first.u();
            secondV = second.v() - first.v();
            thirdU = third.u() - first.u();
            thirdV = third.v() - first.v();
            determinant = secondU * thirdV - secondV * thirdU;
            startFirst = geometry.starts().getFirst();
            startSecond = geometry.starts().get(index);
            startThird = geometry.starts().get(index + 1);
            endFirst = geometry.ends().getFirst();
            endSecond = geometry.ends().get(index);
            endThird = geometry.ends().get(index + 1);
        }
    }
}
