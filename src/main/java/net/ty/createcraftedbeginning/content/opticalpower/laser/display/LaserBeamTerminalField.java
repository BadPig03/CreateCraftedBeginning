package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayProfile.Contact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserDisplayFamilies.Family;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserDisplayPath.PhysicalPoint;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamTerminalField {
    private static final double EPSILON = 1.0E-7;
    private final Family family;
    private boolean connecting;
    private final Map<Point, Sample> samples = new LinkedHashMap<>();
    private final Map<Point, Ray> paths = new HashMap<>();
    private int idealNullSamples;
    private double blend = 1;

    private LaserBeamTerminalField(Family family) {
        this.family = family;
    }

    static LaserBeamWork<LaserBeamTerminalField> begin(Family family, LaserBeamSourceDomains topology) {
        return new Build(family, topology);
    }

    static Vec3 idealPoint(Ray path, double distance) {
        return LaserBeamDisplayProfile.fromBeam(path.start()).point(distance, path.u(), path.v());
    }

    boolean connecting() {
        return connecting;
    }

    Ray path(Point uv) {
        return paths.computeIfAbsent(uv, point -> {
            Sample value = sample(point);
            Vec3 terminal;
            if (connecting) {
                terminal = value.mapped().physicalPosition().lerp(value.projected(), blend);
            }
            else {
                if (value.ideal() == null) {
                    throw new UnsupportedOperationException("An interior ideal-null display point needs a connecting field rebuild: " + point + ", target=" + family.terminal().target().blockPos() + ", events=" + family.path().size() + ", sampled=" + samples.size() + '.');
                }

                terminal = value.ideal().position();
            }

            LaserBeamDisplayProfile display = value.display();
            PhysicalPoint mapped = value.mapped();
            double distance = terminal.subtract(display.origin()).dot(display.direction());
            if (distance < -EPSILON || distance > mapped.remainingRange() + EPSILON) {
                throw new UnsupportedOperationException("Mapped connecting display exceeds its actual remaining range.");
            }

            BeamState start = mapped.start();
            BeamProfile profile = start.profile();
            double slope = profile.widthSlope();
            double focus = slope == 0 ? Double.NaN : -profile.referenceWidth() / slope;
            BeamFootprint footprint = start.footprint();
            return new Ray(start, footprint.horizontalOffset(), footprint.verticalOffset(), distance, focus, terminal);
        });
    }

    private Sample sample(Point uv) {
        return samples.computeIfAbsent(uv, point -> {
            PhysicalPoint mapped = family.evaluate(point.u(), point.v());
            BeamState start = mapped.start();
            LaserBeamDisplayProfile display = LaserBeamDisplayProfile.fromBeam(start);
            BeamFootprint footprint = start.footprint();
            double u = footprint.horizontalOffset();
            double v = footprint.verticalOffset();
            OpticalHit terminal = family.terminal();
            Contact ideal = display.intersect(terminal, mapped.remainingRange(), u, v);
            if (ideal == null) {
                idealNullSamples++;
            }

            double distance = mapped.distance();
            Vec3 projected = display.point(distance, u, v);
            Vec3 normal = terminal.surface().normal();
            projected = projected.subtract(normal.scale(projected.subtract(terminal.position()).dot(normal)));
            BeamProfile profile = start.profile();
            double width = profile.referenceWidth() + profile.widthSlope() * distance;
            return new Sample(mapped, display, projected, ideal, width);
        });
    }

    private static final class Build implements LaserBeamWork<LaserBeamTerminalField> {
        private final LaserBeamTerminalField field;
        private final ArrayDeque<Unit> units = new ArrayDeque<>();
        private Iterator<Sample> values = Collections.emptyIterator();
        private Iterator<Point> keys = Collections.emptyIterator();
        private int stage;

        private Build(Family family, LaserBeamSourceDomains topology) {
            field = new LaserBeamTerminalField(family);
            for (BeamRegion domain : family.sourceDomains()) {
                List<Point> vertices = domain.vertices();
                Iterator<Point> iterator = vertices.iterator();
                units.addLast(() -> {
                    if (!iterator.hasNext()) {
                        return true;
                    }

                    field.sample(iterator.next());
                    return !iterator.hasNext();
                });
                for (int edge = 0; edge < vertices.size(); edge++) {
                    Point first = vertices.get(edge);
                    Point second = vertices.get((edge + 1) % vertices.size());
                    units.addLast(new Line(field, first, second, 16));
                    for (double event : new double[]{-BeamProfile.MIN_WIDTH, 0, BeamProfile.MIN_WIDTH}) {
                        units.addLast(new Event(field, first, second, event));
                    }
                }
            }

            for (List<Point> ring : topology.rings()) {
                for (int edge = 0; edge < ring.size(); edge++) {
                    units.addLast(new Line(field, ring.get(edge), ring.get((edge + 1) % ring.size()), 32));
                }
            }

            for (BeamRegion domain : topology.cells()) {
                for (int corner = 1; corner + 1 < domain.vertices().size(); corner++) {
                    units.addLast(new Grid(field, domain.vertices().getFirst(), domain.vertices().get(corner), domain.vertices().get(corner + 1)));
                }
            }
        }

        @Override
        public boolean step() {
            if (stage == 3) {
                return true;
            }

            if (stage == 0) {
                if (!units.isEmpty()) {
                    if (units.getFirst().step()) {
                        units.removeFirst();
                    }

                    return false;
                }

                field.connecting = field.idealNullSamples > 0;
                if (!field.connecting) {
                    stage = 3;
                    return true;
                }

                values = field.samples.values().iterator();
                stage = 1;
            }

            if (stage == 1) {
                if (values.hasNext()) {
                    Sample point = values.next();
                    field.blend = LaserBeamDisplayProfile.constrainBlend(field.blend, point.mapped().physicalPosition(), point.projected(), point.display().origin(), point.display().direction(), point.mapped().remainingRange());
                    return false;
                }

                field.blend = Math.clamp(field.blend, 0, 1);
                keys = field.samples.keySet().iterator();
                stage = 2;
            }

            if (keys.hasNext()) {
                field.path(keys.next());
                return false;
            }

            stage = 3;
            return true;
        }

        @Override
        public LaserBeamTerminalField result() {
            if (stage != 3) {
                throw new IllegalStateException("Terminal field is not complete.");
            }

            return field;
        }
    }

    @FunctionalInterface
    private interface Unit {
        boolean step();
    }

    private static final class Line implements Unit {
        private final LaserBeamTerminalField field;
        private final Point first;
        private final Point second;
        private final int parts;
        private int part;

        private Line(LaserBeamTerminalField field, Point first, Point second, int parts) {
            this.field = field;
            this.first = first;
            this.second = second;
            this.parts = parts;
        }

        @Override
        public boolean step() {
            field.sample(first.lerp(second, (double) part / parts));
            return ++part > parts;
        }
    }

    private static final class Grid implements Unit {
        private final LaserBeamTerminalField field;
        private final Point first;
        private final Point second;
        private final Point third;
        private int u;
        private int v;

        private Grid(LaserBeamTerminalField field, Point first, Point second, Point third) {
            this.field = field;
            this.first = first;
            this.second = second;
            this.third = third;
        }

        @Override
        public boolean step() {
            field.sample(new Point(first.u() + (second.u() - first.u()) * u / 16 + (third.u() - first.u()) * v / 16, first.v() + (second.v() - first.v()) * u / 16 + (third.v() - first.v()) * v / 16));
            if (++v > 16 - u) {
                v = 0;
                u++;
            }

            return u > 16;
        }
    }

    private static final class Event implements Unit {
        private final LaserBeamTerminalField field;
        private final Point first;
        private final Point second;
        private final double event;
        private Point left;
        private Point right;
        private Point low;
        private Point high;
        private double previous;
        private double next;
        private int part = 1;
        private int bisect = -1;
        private boolean initialized;

        private Event(LaserBeamTerminalField field, Point first, Point second, double event) {
            this.field = field;
            this.first = first;
            this.second = second;
            this.event = event;
            left = first;
            right = first;
            low = first;
            high = first;
        }

        @Override
        public boolean step() {
            if (!initialized) {
                previous = field.sample(left).terminalWidth() - event;
                initialized = true;
                return false;
            }

            if (bisect >= 0) {
                if (bisect < 48) {
                    Point middle = low.lerp(high, 0.5);
                    double value = field.sample(middle).terminalWidth() - event;
                    if (Math.signum(value) == Math.signum(previous)) {
                        low = middle;
                    }
                    else {
                        high = middle;
                    }

                    bisect++;
                    return false;
                }

                field.sample(low.lerp(high, 0.5));
                bisect = -1;
                left = right;
                previous = next;
                return ++part > 16;
            }

            right = first.lerp(second, part / 16.0);
            next = field.sample(right).terminalWidth() - event;
            if (previous * next < 0) {
                low = left;
                high = right;
                bisect = 0;
                return false;
            }

            left = right;
            previous = next;
            return ++part > 16;
        }
    }

    record Ray(BeamState start, double u, double v, double terminalDistance, double focusDistance, Vec3 terminalPosition) {
        boolean hasFocus() {
            return Double.isFinite(focusDistance) && focusDistance > EPSILON;
        }

        double focusEvent() {
            if (!hasFocus()) {
                return Double.NEGATIVE_INFINITY;
            }

            return terminalDistance - focusDistance;
        }

        double width(double distance) {
            return start.profile().referenceWidth() + start.profile().widthSlope() * distance;
        }
    }

    private record Sample(PhysicalPoint mapped, LaserBeamDisplayProfile display, Vec3 projected, @Nullable Contact ideal, double terminalWidth) {}
}
