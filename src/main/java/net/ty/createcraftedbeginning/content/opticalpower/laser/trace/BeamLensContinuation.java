package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.OcclusionWork;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Termination;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalApertureFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.PropagationBudget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile.Span;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Vertex;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalAperture;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPathReplay.Evaluation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPathReplay.Path;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPropagationSolver.Trace;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BeamLensContinuation {
    private static final double CORNER_SAMPLE_INSET_RATIO = 1.0E-4;
    private static final double MIN_PARTITION_AREA = 1.0E-12;
    private static final double FORWARD_PLANE_TOLERANCE = 1.0E-9;
    private static final double BOUNDARY_INSET = 1.0E-7;
    private static final double MIN_BOUNDARY_SPAN = 2.0E-7;
    private static final int MAX_BOUNDARY_BISECTION_STEPS = 40;
    private static final double BOUNDARY_CONVERGENCE_TOLERANCE = 1.0E-10;

    private final BeamState source;
    private final List<OpticalHit> prefix;
    private final PropagationBudget budget;
    private final BeamRayQuery query;
    private final Map<Point, @Nullable BoundarySample> boundarySamples = new HashMap<>();

    private BeamLensContinuation(BeamState source, List<OpticalHit> prefix, PropagationBudget budget, BeamRayQuery query) {
        this.source = source;
        this.prefix = prefix;
        this.budget = budget;
        this.query = query;
    }

    static @Nullable BeamPropagation solve(Level level, LaserCollisionContext collision, boolean rendering, BeamState source, List<OpticalHit> prefix, BeamRegion domain, PropagationBudget budget, BeamRayQuery query, BeamVolumeQuery volume, int regionLimit, int segmentLimit) {
        List<Point> vertices = domain.vertices();
        double left = vertices.stream().mapToDouble(Point::u).min().orElseThrow();
        double right = vertices.stream().mapToDouble(Point::u).max().orElseThrow();
        double bottom = vertices.stream().mapToDouble(Point::v).min().orElseThrow();
        double top = vertices.stream().mapToDouble(Point::v).max().orElseThrow();
        double domainArea = domain.area();
        BeamLensContinuation continuation = new BeamLensContinuation(source, prefix, budget, query);
        ArrayDeque<BeamRegion> pending = new ArrayDeque<>();
        pending.add(domain);
        int regions = 1;
        Point domainCenter = domain.center();
        Path shared = BeamPathReplay.replay(source, prefix, budget.range(), new BeamFootprint((right - left) / 2, (top - bottom) / 2, domainCenter.u(), domainCenter.v(), domain), source.fluxLm() * domainArea / source.footprint().domain().area(), true);
        if (shared == null) {
            return null;
        }

        List<Segment> segments = new ArrayList<>(shared.segments());
        List<Termination> terminations = new ArrayList<>();
        while (!pending.isEmpty()) {
            BeamRegion region = pending.removeFirst();
            Point center = region.center();
            Split boundary = continuation.boundary(region);
            if (boundary != null) {
                if (regions >= regionLimit) {
                    return null;
                }

                pending.addLast(boundary.first());
                pending.addLast(boundary.second());
                regions++;
                continue;
            }

            Trace middle = continuation.start(region, center, true);
            if (middle == null) {
                return null;
            }

            List<Trace> samples = new ArrayList<>();
            List<Point> points = new ArrayList<>();
            boolean splitHorizontal = true;
            for (Point vertex : region.vertices()) {
                Point point = vertex.lerp(center, CORNER_SAMPLE_INSET_RATIO);
                Trace sample = continuation.start(region, point, false);
                if (sample == null) {
                    return null;
                }

                points.add(point);
                samples.add(sample);
            }
            List<Trace> all = new ArrayList<>();
            all.add(middle);
            all.addAll(samples);
            if (all.stream().allMatch(trace -> {
                if (trace.isComplete()) {
                    return false;
                }

                List<Segment> path = trace.segments();
                return path.size() == prefix.size() + 1 && path.getLast().hit() == null;
            })) {
                List<Trace> curved = new ArrayList<>();
                for (Trace trace : all) {
                    Path checkpoint = trace.checkpoint();
                    BeamState state = checkpoint.state();
                    List<Span> spans = state.profile().spans(checkpoint.remainingRange());
                    double end = spans.getLast().from();
                    if (end <= 0) {
                        break;
                    }

                    Trace proof = new Trace(trace.segments().getFirst().start(), budget, checkpoint);
                    proof.advance((beam, range) -> new OpticalQueryResult(range, null), end);
                    curved.add(proof);
                }
                if (curved.size() == all.size()) {
                    List<BeamPropagation> paths = curved.stream().map(trace -> {
                        List<Segment> path = trace.segments();
                        return new BeamPropagation(path.getFirst().start(), path, List.of());
                    }).toList();
                    if (!volume.hasUnseenRegionObstacle(paths.getFirst(), paths.subList(1, paths.size()), prefix.size() + 1, true)) {
                        all = curved;
                        middle = all.getFirst();
                        samples = new ArrayList<>(all.subList(1, all.size()));
                    }
                }
            }
            if (all.stream().noneMatch(Trace::isComplete)) {
                Path checkpoint = middle.checkpoint();
                List<Segment> prefixSegments = checkpoint.segments();
                List<Segment> exitSegments = prefixSegments.subList(prefix.size(), prefixSegments.size());
                int available = Math.min(segmentLimit, budget.maxBeams()) - segments.size() - exitSegments.size();
                int depth = budget.maxDepth() - (int) prefixSegments.stream().filter(segment -> segment.hit() != null).count();
                List<BeamPropagation> paths = all.stream().map(trace -> {
                    List<Segment> path = trace.segments();
                    return new BeamPropagation(path.getFirst().start(), path, List.of());
                }).toList();
                if (available > 0 && depth > 0 && !volume.hasUnseenRegionObstacle(paths.getFirst(), paths.subList(1, paths.size()), prefix.size(), false)) {
                    PropagationBudget remaining = new PropagationBudget(checkpoint.remainingRange(), available, depth, available, budget.minFluxLm());
                    BeamPropagation tail = BeamTerminalPartition.solveTail(level, checkpoint.state(), remaining, collision, rendering, query, volume, regionLimit - regions + 1, available);
                    if (tail != null) {
                        int prefixOffset = segments.size() - prefix.size();
                        for (Segment segment : exitSegments) {
                            int parent = segment.parentIndex();
                            if (parent >= prefix.size()) {
                                parent += prefixOffset;
                            }
                            segments.add(new Segment(parent, segment.start(), segment.incident(), segment.length(), segment.remainingRange(), segment.hit(), segment.absorbedFluxLm(), segment.contact()));
                        }
                        int parent = segments.size() - 1;
                        int offset = segments.size();
                        for (Segment segment : tail.segments()) {
                            int previous = segment.parentIndex() < 0 ? parent : segment.parentIndex() + offset;
                            segments.add(new Segment(previous, segment.start(), segment.incident(), segment.length(), segment.remainingRange(), segment.hit(), segment.absorbedFluxLm(), segment.contact()));
                        }
                        for (Termination termination : tail.terminations()) {
                            int previous = termination.parentIndex() < 0 ? parent : termination.parentIndex() + offset;
                            terminations.add(new Termination(previous, termination.state(), termination.reason()));
                        }
                        regions += tail.occlusionWork().sampledRegions() - 1;
                        continue;
                    }
                }
            }
            for (Trace trace : all) {
                while (!trace.isComplete() && !query.exhausted()) {
                    trace.advance(query);
                }
            }
            if (query.exhausted()) {
                return null;
            }

            boolean uniform = true;
            for (Trace sample : samples) {
                uniform &= BeamOcclusionSolver.samePath(middle, sample, prefix.size());
            }
            List<Segment> resolved = new ArrayList<>();
            if (uniform) {
                List<Segment> path = middle.segments();
                for (Segment segment : path.subList(prefix.size(), path.size())) {
                    OpticalHit hit = segment.hit();
                    BeamContact contact = null;
                    BeamState state = segment.start();
                    double remainingRange = segment.remainingRange();
                    double absorbedFluxLm = segment.absorbedFluxLm();
                    if (hit != null) {
                        contact = BeamContact.create(state, remainingRange, hit);
                        if (contact == null) {
                            uniform = false;
                            break;
                        }

                        if (absorbedFluxLm > 0 && contact.patches().stream().anyMatch(patch -> patch.density() == null)) {
                            BeamFootprint footprint = state.footprint();
                            BeamFootprint rectangle = new BeamFootprint(footprint.horizontalScale(), footprint.verticalScale(), footprint.horizontalOffset(), footprint.verticalOffset());
                            BeamContact density = BeamContact.create(new BeamState(state.frame(), state.profile(), state.fluxLm(), rectangle), remainingRange, hit);
                            if (density == null || !density.uniformDensity()) {
                                if (density != null) {
                                    splitHorizontal = density.splitHorizontal();
                                }
                                uniform = false;
                                break;
                            }
                        }
                    }
                    resolved.add(new Segment(segment.parentIndex(), state, segment.incident(), segment.length(), remainingRange, hit, absorbedFluxLm, contact));
                }
            }
            if (uniform && !volume.hasUnseenRegionObstacle(middle.result(), samples.stream().map(Trace::result).toList(), prefix.size(), false)) {
                if (segments.size() + resolved.size() > segmentLimit || segments.size() + resolved.size() > budget.maxBeams()) {
                    return null;
                }

                int offset = segments.size() - prefix.size();
                for (Segment segment : resolved) {
                    int parent = segment.parentIndex();
                    if (parent >= prefix.size()) {
                        parent += offset;
                    }
                    segments.add(new Segment(parent, segment.start(), segment.incident(), segment.length(), segment.remainingRange(), segment.hit(), segment.absorbedFluxLm(), segment.contact()));
                }
                for (Termination termination : middle.terminations()) {
                    int parent = termination.parentIndex();
                    if (parent >= prefix.size()) {
                        parent += offset;
                    }
                    terminations.add(new Termination(parent, termination.state(), termination.reason()));
                }
                continue;
            }

            if (regions >= regionLimit || query.exhausted()) {
                return null;
            }

            boolean horizontal = splitHorizontal;
            for (int index = 0; index < points.size(); index++) {
                int next = (index + 1) % points.size();
                if (BeamOcclusionSolver.samePath(samples.get(index), samples.get(next), prefix.size())) {
                    continue;
                }

                Point first = points.get(index);
                Point second = points.get(next);
                horizontal = Math.abs(first.u() - second.u()) >= Math.abs(first.v() - second.v());
                break;
            }
            Split partition = split(region, horizontal, horizontal ? center.u() : center.v());
            if (partition == null) {
                partition = split(region, !horizontal, horizontal ? center.v() : center.u());
            }
            if (partition == null) {
                return null;
            }

            pending.addLast(partition.first());
            pending.addLast(partition.second());
            regions++;
        }
        return new BeamPropagation(source, segments, terminations, new OcclusionWork(regions, 0));
    }

    private static @Nullable Split split(BeamRegion region, boolean horizontal, double coordinate) {
        BeamRegion first = horizontal ? region.clip(1, 0, -coordinate) : region.clip(0, 1, -coordinate);
        BeamRegion second = horizontal ? region.clip(-1, 0, coordinate) : region.clip(0, -1, coordinate);
        if (first.area() <= MIN_PARTITION_AREA || second.area() <= MIN_PARTITION_AREA) {
            return null;
        }

        return new Split(first, second);
    }

    private @Nullable Split boundary(BeamRegion region) {
        OpticalHit lens = prefix.getLast();
        OpticalSurface lensSurface = lens.surface();
        OpticalApertureFrame frame = lensSurface.apertureFrame();
        if (frame == null) {
            return null;
        }

        List<Point> vertices = region.vertices();
        double minU = vertices.stream().mapToDouble(Point::u).min().orElseThrow();
        double maxU = vertices.stream().mapToDouble(Point::u).max().orElseThrow();
        double minV = vertices.stream().mapToDouble(Point::v).min().orElseThrow();
        double maxV = vertices.stream().mapToDouble(Point::v).max().orElseThrow();
        Point center = region.center();
        Split best = null;
        double balance = 0;
        Map<Point, @Nullable BoundarySample> samples = boundarySamples;
        BoundarySample middle = sample(samples, center);
        if (middle == null) {
            return null;
        }

        BeamState state = middle.state();
        BeamFrame beamFrame = state.frame();
        double centerU = center.u();
        double centerV = center.v();
        for (OpticalAperture aperture : frame.boundaries()) {
            Vec3 horizontalAxis = aperture.horizontal();
            Vec3 verticalAxis = aperture.vertical();
            Vec3 normal = horizontalAxis.cross(verticalAxis);
            if (normal.dot(beamFrame.direction()) > 0) {
                normal = normal.scale(-1);
            }
            Vec3 apertureCenter = aperture.center();
            if (beamFrame.origin().subtract(apertureCenter).dot(normal) <= FORWARD_PLANE_TOLERANCE) {
                continue;
            }

            OpticalHit plane = new OpticalHit(lens.target(), lens.localHit(), apertureCenter, new OpticalSurface(normal, null, lensSurface.interactor()), normal);
            for (boolean horizontal : new boolean[]{true, false}) {
                double low = horizontal ? minU : minV;
                double high = horizontal ? maxU : maxV;
                if (high - low <= MIN_BOUNDARY_SPAN) {
                    continue;
                }

                for (Vec3 axis : List.of(horizontalAxis, verticalAxis)) {
                    for (int side : new int[]{-1, 1}) {
                        double edge = aperture.halfSize() * side;
                        Point first = horizontal ? new Point(low + BOUNDARY_INSET, centerV) : new Point(centerU, low + BOUNDARY_INSET);
                        Point second = horizontal ? new Point(high - BOUNDARY_INSET, centerV) : new Point(centerU, high - BOUNDARY_INSET);
                        double from = coordinate(samples, first, plane, axis, edge);
                        double to = coordinate(samples, second, plane, axis, edge);
                        if (!Double.isFinite(from) || !Double.isFinite(to) || from * to >= 0) {
                            continue;
                        }

                        double left = low + BOUNDARY_INSET;
                        double right = high - BOUNDARY_INSET;
                        for (int step = 0; step < MAX_BOUNDARY_BISECTION_STEPS && right - left > BOUNDARY_CONVERGENCE_TOLERANCE; step++) {
                            double cut = (left + right) / 2;
                            Point point = horizontal ? new Point(cut, centerV) : new Point(centerU, cut);
                            double value = coordinate(samples, point, plane, axis, edge);
                            if (!Double.isFinite(value)) {
                                break;
                            }

                            if (from * value > 0) {
                                left = cut;
                                continue;
                            }

                            right = cut;
                        }
                        double cut = (left + right) / 2;
                        if (cut <= low + BOUNDARY_INSET || cut >= high - BOUNDARY_INSET) {
                            continue;
                        }

                        double score = (cut - low) * (high - cut) / ((high - low) * (high - low));
                        if (score <= balance) {
                            continue;
                        }

                        Split candidate = split(region, horizontal, cut);
                        if (candidate == null) {
                            continue;
                        }

                        best = candidate;
                        balance = score;
                    }
                }
            }
        }
        return best;
    }

    private double coordinate(Map<Point, @Nullable BoundarySample> samples, Point point, OpticalHit plane, Vec3 axis, double edge) {
        BoundarySample sample = sample(samples, point);
        if (sample == null) {
            return Double.NaN;
        }

        Map<OpticalPlane, @Nullable Vec3> intersections = sample.intersections();
        Vec3 planePosition = plane.position();
        OpticalPlane key = new OpticalPlane(planePosition, plane.surface().normal());
        Vec3 position = intersections.get(key);
        if (position == null && !intersections.containsKey(key)) {
            BeamState state = sample.state();
            BeamFootprint footprint = state.footprint();
            Vertex vertex = BeamContact.intersect(state, sample.spans(), plane, footprint.horizontalOffset(), footprint.verticalOffset());
            if (vertex != null) {
                position = vertex.position();
            }
            intersections.put(key, position);
        }
        if (position == null) {
            return Double.NaN;
        }

        return position.subtract(planePosition).dot(axis) - edge;
    }

    private @Nullable BoundarySample sample(Map<Point, @Nullable BoundarySample> samples, Point point) {
        BoundarySample sample = samples.get(point);
        if (sample != null || samples.containsKey(point)) {
            return sample;
        }

        Evaluation endpoint = BeamPathReplay.evaluate(source, prefix, budget.range(), new BeamFootprint(1, 1, point.u(), point.v()), source.fluxLm());
        if (endpoint != null) {
            BeamState state = endpoint.state();
            sample = new BoundarySample(state, state.profile().spans(endpoint.remainingRange()), new HashMap<>());
        }
        samples.put(point, sample);
        return sample;
    }

    private @Nullable Trace start(BeamRegion region, Point point, boolean contacts) {
        List<Point> vertices = region.vertices();
        double minU = vertices.stream().mapToDouble(Point::u).min().orElseThrow();
        double maxU = vertices.stream().mapToDouble(Point::u).max().orElseThrow();
        double minV = vertices.stream().mapToDouble(Point::v).min().orElseThrow();
        double maxV = vertices.stream().mapToDouble(Point::v).max().orElseThrow();
        BeamFootprint footprint = new BeamFootprint((maxU - minU) / 2, (maxV - minV) / 2, point.u(), point.v(), region);
        double flux = source.fluxLm() * region.area() / source.footprint().domain().area();
        Path path = BeamPathReplay.replay(source, prefix, budget.range(), footprint, flux, contacts);
        if (path == null) {
            return null;
        }

        Trace trace = new Trace(path.segments().getFirst().start(), budget, path);
        OpticalHit lens = prefix.getLast();
        OpticalSurface lensSurface = lens.surface();
        OpticalApertureFrame frame = lensSurface.apertureFrame();
        if (frame == null) {
            return null;
        }

        BeamState state = path.state();
        Vec3 normal = lensSurface.normal();
        Vec3 exit = frame.opening().center().subtract(normal.scale(frame.halfThickness()));
        OpticalHit plane = new OpticalHit(lens.target(), lens.localHit(), exit, new OpticalSurface(normal, null, lensSurface.interactor()), normal);
        BeamFootprint outgoing = state.footprint();
        Vertex vertex = BeamContact.intersect(state, state.profile().spans(path.remainingRange()), plane, outgoing.horizontalOffset(), outgoing.verticalOffset());
        if (vertex == null) {
            return null;
        }

        double distance = vertex.distance();
        if (distance <= 0) {
            return null;
        }

        trace.advance(query, distance);
        return trace;
    }

    private record Split(BeamRegion first, BeamRegion second) {}

    private record BoundarySample(BeamState state, List<Span> spans, Map<OpticalPlane, @Nullable Vec3> intersections) {}
}
