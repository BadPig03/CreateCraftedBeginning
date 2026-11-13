package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile.Span;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplayFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplayState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Vertex;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record LaserBeamDisplayProfile(Vec3 origin, Vec3 direction, Vec3 transverse, BeamProfile physical) {
    private static final double DISTANCE_EPSILON = 1.0E-7;

    static LaserBeamDisplayProfile fromBeam(BeamState beam) {
        BeamFrame frame = beam.frame();
        BeamFootprint footprint = beam.footprint();
        BeamProfile profile = beam.profile();
        double radius = profile.radius();
        Vec3 transverse = frame.transverseAxis();
        Vec3 origin = frame.origin().subtract(transverse.scale(radius * footprint.horizontalOffset())).subtract(frame.getSecondTransverseAxis().scale(radius * footprint.verticalOffset()));
        return new LaserBeamDisplayProfile(origin, frame.direction(), transverse, profile);
    }

    static LaserBeamDisplayProfile fromBeam(DisplayState beam) {
        DisplayFrame frame = beam.frame();
        BeamFootprint footprint = beam.footprint();
        BeamProfile profile = beam.profile();
        double radius = profile.radius();
        Vec3 transverse = frame.transverseAxis();
        Vec3 origin = frame.origin().subtract(transverse.scale(radius * footprint.horizontalOffset())).subtract(frame.getSecondTransverseAxis().scale(radius * footprint.verticalOffset()));
        return new LaserBeamDisplayProfile(origin, frame.direction(), transverse, profile);
    }

    static double constrainBlend(double blend, Vec3 physical, Vec3 projected, Vec3 origin, Vec3 direction, double range) {
        double from = physical.subtract(origin).dot(direction);
        double change = projected.subtract(physical).dot(direction);
        if (change > 0) {
            return Math.min(blend, (range - from) / change);
        }

        if (change < 0) {
            return Math.min(blend, -from / change);
        }

        return blend;
    }

    double radiusAt(double distance) {
        double slope = physical.widthSlope();
        if (slope == 0) {
            return physical.radius();
        }

        double width = physical.referenceWidth();
        if (distance == -width / slope) {
            return 0;
        }

        return Math.abs(width + slope * distance) / 2;
    }

    Vec3 point(double distance, double u, double v) {
        double radius = radiusAt(distance);
        return origin.add(direction.scale(distance)).add(transverse.scale(radius * u)).add(direction.cross(transverse).scale(radius * v));
    }

    @Unmodifiable List<Double> sampleDistances(double range) {
        double slope = physical.widthSlope();
        if (slope != 0) {
            double focus = -physical.referenceWidth() / slope;
            if (focus > DISTANCE_EPSILON && focus < range - DISTANCE_EPSILON) {
                return List.of(0.0, focus, range);
            }
        }

        return List.of(0.0, range);
    }

    @Nullable Contact intersect(OpticalPlane plane, double range, double u, double v) {
        Vec3 normal = plane.normal();
        double slope = physical.widthSlope();
        double width = physical.referenceWidth();
        Vec3 across = transverse.scale(u).add(direction.cross(transverse).scale(v));
        Contact nearest = null;
        for (int side = -1; side <= 1; side += 2) {
            Vec3 start = origin.add(across.scale(side * width / 2));
            Vec3 rate = direction.add(across.scale(side * slope / 2));
            if (slope == 0) {
                if (side < 0) {
                    continue;
                }

                start = point(0, u, v);
                rate = direction;
            }
            double denominator = rate.dot(normal);
            if (denominator >= 0) {
                continue;
            }

            double distance = plane.point().subtract(start).dot(normal) / denominator;
            if (distance < -DISTANCE_EPSILON || distance > range + DISTANCE_EPSILON || slope != 0 && side * (width + slope * distance) < -DISTANCE_EPSILON || nearest != null && distance >= nearest.distance()) {
                continue;
            }

            distance = Math.clamp(distance, 0, range);
            nearest = new Contact(start.add(rate.scale(distance)), distance);
        }
        return nearest;
    }

    Terminal createTerminal(BeamState beam, double range, OpticalPlane plane, List<Point> boundary) {
        return new Terminal(this, beam, range, plane, boundary);
    }

    record Contact(Vec3 position, double distance) {}

    static final class Terminal {
        private static final double BARYCENTRIC_EPSILON = 1.0E-7;

        private final List<Point> boundary;
        private final List<Contact> contacts;

        private Terminal(LaserBeamDisplayProfile display, BeamState beam, double range, OpticalPlane plane, List<Point> boundary) {
            this.boundary = List.copyOf(boundary);
            List<Contact> ideal = new ArrayList<>(boundary.size());
            boolean connecting = false;
            for (Point point : boundary) {
                Contact contact = display.intersect(plane, range, point.u(), point.v());
                if (contact == null) {
                    connecting = true;
                    continue;
                }

                ideal.add(contact);
            }
            if (!connecting) {
                contacts = List.copyOf(ideal);
                return;
            }

            List<Span> trajectory = beam.profile().spans(range);
            List<Vertex> physical = new ArrayList<>(boundary.size());
            List<Vec3> projected = new ArrayList<>(boundary.size());
            double blend = 1;
            Vec3 normal = plane.normal();
            Vec3 direction = display.direction();
            for (Point point : boundary) {
                Vertex vertex = BeamContact.intersect(beam, trajectory, plane, point.u(), point.v());
                if (vertex == null) {
                    throw new IllegalStateException("Accepted beam has no physical terminal intersection at source coordinates " + point + '.');
                }

                Vec3 target = display.point(vertex.distance(), point.u(), point.v());
                target = target.subtract(normal.scale(target.subtract(plane.point()).dot(normal)));
                blend = constrainBlend(blend, vertex.position(), target, display.origin(), direction, range);
                physical.add(vertex);
                projected.add(target);
            }
            blend = Math.clamp(blend, 0, 1);
            List<Contact> contacts = new ArrayList<>(boundary.size());
            for (int index = 0; index < boundary.size(); index++) {
                Vec3 point = physical.get(index).position().lerp(projected.get(index), blend);
                contacts.add(new Contact(point, point.subtract(display.origin()).dot(direction)));
            }
            this.contacts = List.copyOf(contacts);
        }

        @Nullable Contact intersect(double u, double v) {
            Point first = boundary.getFirst();
            Contact anchor = contacts.getFirst();
            for (int index = 1; index + 1 < boundary.size(); index++) {
                Point second = boundary.get(index);
                Point third = boundary.get(index + 1);
                double firstU = second.u() - first.u();
                double firstV = second.v() - first.v();
                double secondU = third.u() - first.u();
                double secondV = third.v() - first.v();
                double determinant = firstU * secondV - firstV * secondU;
                if (determinant == 0) {
                    continue;
                }

                double relativeU = u - first.u();
                double relativeV = v - first.v();
                double b = (relativeU * secondV - relativeV * secondU) / determinant;
                double c = (firstU * relativeV - firstV * relativeU) / determinant;
                double a = 1 - b - c;
                if (a < -BARYCENTRIC_EPSILON || b < -BARYCENTRIC_EPSILON || c < -BARYCENTRIC_EPSILON) {
                    continue;
                }

                a = Math.max(0, a);
                b = Math.max(0, b);
                c = Math.max(0, c);
                double sum = a + b + c;
                a /= sum;
                b /= sum;
                c /= sum;
                Contact next = contacts.get(index);
                Contact last = contacts.get(index + 1);
                Vec3 point = anchor.position().scale(a).add(next.position().scale(b)).add(last.position().scale(c));
                return new Contact(point, anchor.distance() * a + next.distance() * b + last.distance() * c);
            }

            return null;
        }
    }
}
