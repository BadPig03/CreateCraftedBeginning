package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserDisplayPath.Endpoint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserDisplayPath.PhysicalPoint;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserDisplayFamilies {
    private static final double DOMAIN_COORDINATE_TOLERANCE = 1.0E-12;
    private static final double EVENT_PLANE_KEY_SCALE = 1.0E8;
    private static final double POSITION_TOLERANCE = 1.0E-8;
    private static final double SCALAR_TOLERANCE = 1.0E-9;

    private LaserDisplayFamilies() {}

    static @Unmodifiable List<Family> discover(BeamPropagation propagation, Set<Integer> nonRenderableSegments) {
        List<Segment> segments = propagation.segments();
        List<List<Integer>> children = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            children.add(new ArrayList<>());
        }
        for (int index = 0; index < segments.size(); index++) {
            int parent = segments.get(index).parentIndex();
            if (parent < 0) {
                continue;
            }

            children.get(parent).add(index);
        }
        Map<Key, Group> groups = new LinkedHashMap<>();
        for (int index = 0; index < segments.size(); index++) {
            Segment segment = segments.get(index);
            OpticalHit terminal = segment.hit();
            if (terminal == null || segment.absorbedFluxLm() <= 0 || terminal.surface().aperture() != null || segment.start().profile().widthSlope() == 0 || !ordinary(terminal)) {
                continue;
            }

            List<Integer> transparent = new ArrayList<>();
            int cursor = segment.parentIndex();
            int child = index;
            double distance = 0;
            while (cursor >= 0 && segments.get(cursor).hit() == null) {
                Segment step = segments.get(cursor);
                if (children.get(cursor).size() != 1 || children.get(cursor).getFirst() != child || step.absorbedFluxLm() != 0 || !sameDomain(step.start().footprint().domain(), segment.start().footprint().domain())) {
                    break;
                }

                validateState(step.start().advance(step.length()), segments.get(child).start());
                near(step.remainingRange() - step.length(), segments.get(child).remainingRange(), POSITION_TOLERANCE);
                transparent.add(cursor);
                distance += step.length();
                child = cursor;
                cursor = step.parentIndex();
            }
            if (cursor < 0) {
                continue;
            }

            OpticalHit prefixHit = segments.get(cursor).hit();
            if (prefixHit == null || prefixHit.surface().aperture() == null) {
                continue;
            }

            Prefix prefix = prefix(segments, cursor);
            if (prefix == null || prefix.events().stream().noneMatch(hit -> hit.surface().apertureFrame() != null)) {
                continue;
            }

            BeamRegion domain = segment.start().footprint().domain();
            if ((prefix.reflections() & 1) != 0) {
                domain = flip(domain);
            }
            Key key = new Key(prefix.keys(), eventKey(terminal));
            Group group = groups.computeIfAbsent(key, ignored -> new Group(prefix.events(), prefix.range(), terminal));
            group.cells.add(new Cell(index, domain));
            group.displayIds.add(index);
            group.displayIds.addAll(transparent);
            group.tailLengths.add(distance);
            group.firstSegments.add(transparent.isEmpty() ? index : child);
        }
        List<Family> families = new ArrayList<>();
        for (Group group : groups.values()) {
            Family family = group.family(propagation.source());
            for (int ordinal = 0; ordinal < family.cells().size(); ordinal++) {
                PhysicalPoint replay = family.representative(family.cells().get(ordinal));
                Segment original = segments.get(family.cells().get(ordinal).segmentIndex());
                double tail = group.tailLengths.get(ordinal);
                validateState(replay.start(), segments.get(group.firstSegments.get(ordinal)).start());
                validateState(replay.start().advance(tail), original.start());
                validateState(replay.incident(), original.incident());
                near(replay.distance(), tail + original.length(), POSITION_TOLERANCE);
                near(replay.remainingRange() - tail, original.remainingRange(), POSITION_TOLERANCE);
            }
            families.add(family);
        }
        includeCoveredPrefixes(propagation, families, nonRenderableSegments);
        return List.copyOf(families);
    }

    private static void includeCoveredPrefixes(BeamPropagation propagation, List<Family> families, Set<Integer> invalid) {
        if (invalid.isEmpty()) {
            return;
        }

        List<Segment> segments = propagation.segments();
        Set<Integer> selected = new HashSet<>();
        families.forEach(family -> selected.addAll(family.displayIds()));
        Map<Key, Group> groups = new LinkedHashMap<>();
        for (Family terminal : List.copyOf(families)) {
            Map<Integer, List<Cell>> descendants = new LinkedHashMap<>();
            boolean implicated = false;
            for (Cell cell : terminal.cells()) {
                for (int cursor = segments.get(cell.segmentIndex()).parentIndex(); cursor >= 0; cursor = segments.get(cursor).parentIndex()) {
                    descendants.computeIfAbsent(cursor, ignored -> new ArrayList<>()).add(cell);
                    implicated |= invalid.contains(cursor);
                }
            }
            if (!implicated) {
                continue;
            }

            for (Entry<Integer, List<Cell>> entry : descendants.entrySet()) {
                int index = entry.getKey();
                Segment step = segments.get(index);
                OpticalHit hit = step.hit();
                if (selected.contains(index) || hit == null || hit.surface().aperture() == null || step.absorbedFluxLm() != 0 || !ordinary(hit)) {
                    continue;
                }

                Prefix prefix = prefix(segments, step.parentIndex());
                if (prefix == null) {
                    continue;
                }

                BeamRegion domain = step.start().footprint().domain();
                if ((prefix.reflections() & 1) != 0) {
                    domain = flip(domain);
                }
                Area covered = new Area();
                entry.getValue().forEach(cell -> covered.add(cell.region().toArea()));
                covered.exclusiveOr(domain.toArea());
                if (!covered.isEmpty()) {
                    continue;
                }

                Key key = new Key(prefix.keys(), eventKey(hit));
                Group group = groups.computeIfAbsent(key, ignored -> new Group(prefix.events(), prefix.events().isEmpty() ? step.remainingRange() : prefix.range(), hit));
                if (group.displayIds.contains(index)) {
                    continue;
                }

                group.cells.add(new Cell(index, domain));
                group.displayIds.add(index);
            }
        }
        for (Group group : groups.values()) {
            Family prefix = group.family(propagation.source());
            for (Cell cell : prefix.cells()) {
                PhysicalPoint replay = prefix.representative(cell);
                Segment original = segments.get(cell.segmentIndex());
                validateState(replay.start(), original.start());
                validateState(replay.incident(), original.incident());
                near(replay.distance(), original.length(), POSITION_TOLERANCE);
                near(replay.remainingRange(), original.remainingRange(), POSITION_TOLERANCE);
            }
            families.add(prefix);
        }
    }

    private static @Nullable Prefix prefix(List<Segment> segments, int last) {
        List<Integer> chain = new ArrayList<>();
        for (int cursor = last; cursor >= 0; cursor = segments.get(cursor).parentIndex()) {
            chain.add(cursor);
        }
        Collections.reverse(chain);
        List<OpticalHit> events = new ArrayList<>();
        List<Event> keys = new ArrayList<>();
        int reflections = 0;
        for (int index : chain) {
            Segment step = segments.get(index);
            OpticalHit hit = step.hit();
            if (hit == null) {
                continue;
            }

            if (hit.surface().aperture() == null || step.absorbedFluxLm() != 0 || !ordinary(hit)) {
                return null;
            }

            BeamState outgoing = hit.surface().interactor().interact(step.incident(), hit).outgoingBeam();
            if (outgoing == null) {
                return null;
            }

            Vec3 normal = hit.surface().normal();
            if (step.incident().frame().direction().dot(normal) * outgoing.frame().direction().dot(normal) < 0) {
                reflections++;
            }
            events.add(hit);
            keys.add(eventKey(hit));
        }
        double range = chain.isEmpty() ? 0 : segments.get(chain.getFirst()).remainingRange();
        return new Prefix(List.copyOf(events), List.copyOf(keys), reflections, range);
    }

    private static Event eventKey(OpticalHit hit) {
        Vec3 normal = hit.surface().normal();
        return new Event(hit.target(), normal, Math.round(hit.position().dot(normal) * EVENT_PLANE_KEY_SCALE), hit.surface().aperture(), hit.surface().interactor().getClass().getName());
    }

    private static boolean ordinary(OpticalHit hit) {
        return hit.target().contraptionId() == null && !hit.target().inSubLevel();
    }

    private static BeamRegion flip(BeamRegion region) {
        return new BeamRegion(region.vertices().reversed().stream().map(point -> new Point(point.u(), -point.v())).toList());
    }

    private static boolean sameDomain(BeamRegion first, BeamRegion second) {
        List<Point> firstVertices = first.vertices();
        List<Point> secondVertices = second.vertices();
        return firstVertices.size() == secondVertices.size() && firstVertices.stream().allMatch(a -> secondVertices.stream().anyMatch(b -> Math.abs(a.u() - b.u()) <= DOMAIN_COORDINATE_TOLERANCE && Math.abs(a.v() - b.v()) <= DOMAIN_COORDINATE_TOLERANCE));
    }

    private static void validateState(BeamState first, BeamState second) {
        near(first.frame().origin().distanceTo(second.frame().origin()), 0, POSITION_TOLERANCE);
        near(first.frame().direction().distanceTo(second.frame().direction()), 0, SCALAR_TOLERANCE);
        near(first.frame().transverseAxis().distanceTo(second.frame().transverseAxis()), 0, SCALAR_TOLERANCE);
        near(first.profile().referenceWidth(), second.profile().referenceWidth(), SCALAR_TOLERANCE);
        near(first.profile().widthSlope(), second.profile().widthSlope(), SCALAR_TOLERANCE);
        near(first.fluxLm(), second.fluxLm(), POSITION_TOLERANCE);
        if (!sameDomain(first.footprint().domain(), second.footprint().domain())) {
            throw new UnsupportedOperationException("Display replay changes the accepted source domain.");
        }
    }

    private static void near(double first, double second, double tolerance) {
        if (!Double.isFinite(first) || !Double.isFinite(second) || Math.abs(first - second) > tolerance) {
            throw new UnsupportedOperationException("Display replay differs from its actual trace.");
        }
    }

    record Cell(int segmentIndex, BeamRegion region) {}

    record Family(LaserDisplayPath map, OpticalHit terminal, List<Cell> cells, List<Integer> displayIds) {
        Family {
            cells = List.copyOf(cells);
            displayIds = List.copyOf(displayIds);
        }

        BeamState source() {
            return map.source();
        }

        List<OpticalHit> path() {
            return map.events();
        }

        @Unmodifiable List<BeamRegion> sourceDomains() {
            return cells.stream().map(Cell::region).toList();
        }

        PhysicalPoint evaluate(double u, double v) {
            PhysicalPoint point = map.intersect(u, v, terminal);
            if (point == null) {
                throw new UnsupportedOperationException("Accepted display source point cannot replay its optical path.");
            }

            return point;
        }

        private PhysicalPoint representative(Cell cell) {
            BeamRegion domain = cell.region();
            Point center = domain.center();
            double minU = domain.vertices().stream().mapToDouble(Point::u).min().orElseThrow();
            double maxU = domain.vertices().stream().mapToDouble(Point::u).max().orElseThrow();
            double minV = domain.vertices().stream().mapToDouble(Point::v).min().orElseThrow();
            double maxV = domain.vertices().stream().mapToDouble(Point::v).max().orElseThrow();
            BeamFootprint footprint = new BeamFootprint((maxU - minU) / 2, (maxV - minV) / 2, center.u(), center.v(), domain);
            double flux = source().fluxLm() * domain.area() / source().footprint().domain().area();
            Endpoint endpoint = map.evaluate(footprint, flux);
            PhysicalPoint point = endpoint == null ? null : LaserDisplayPath.intersect(endpoint, terminal);
            if (point == null) {
                throw new UnsupportedOperationException("Accepted display receiving domain cannot replay its representative.");
            }

            return point;
        }
    }

    private record Event(Object target, Vec3 normal, long plane, @Nullable Object aperture, String interactorClass) {}

    private record Key(List<Event> path, Event terminal) {}

    private record Prefix(List<OpticalHit> events, List<Event> keys, int reflections, double range) {}

    private static final class Group {
        private final List<OpticalHit> path;
        private final double range;
        private final OpticalHit terminal;
        private final List<Cell> cells = new ArrayList<>();
        private final List<Integer> displayIds = new ArrayList<>();
        private final List<Double> tailLengths = new ArrayList<>();
        private final List<Integer> firstSegments = new ArrayList<>();

        private Group(List<OpticalHit> path, double range, OpticalHit terminal) {
            this.path = List.copyOf(path);
            this.range = range;
            this.terminal = terminal;
        }

        private Family family(BeamState source) {
            return new Family(new LaserDisplayPath(source, path, range), terminal, cells, displayIds.stream().distinct().sorted().toList());
        }
    }
}
