package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.amethystlens.AmethystLensBlock;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.OcclusionWork;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Reason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Termination;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalInteraction;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalInteractor;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.PropagationBudget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Vertex;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPathReplay.Focus;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPathReplay.Path;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPropagationSolver.Trace;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceGeometry.PreparedBlock;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.LocalRegion;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.geom.Area;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BeamTerminalPartition {
    private static final double EPSILON = 1.0E-9;
    private static final double FLUX_RELATIVE_TOLERANCE = 1.0E-10;
    private static final double MIN_PARTITION_AREA = 1.0E-12;
    private static final double SURFACE_NORMAL_TOLERANCE_SQR = 1.0E-10;
    private static final int MAX_BLOCKS = 4096;

    private final Level level;
    private final Vec3 anchor;
    private final LaserCollisionContext collision;
    private final boolean rendering;
    private final @Nullable OpticalTarget transmitted;
    private final double end;
    private int checked;
    private int comparisons;

    private BeamTerminalPartition(Level level, Vec3 anchor, LaserCollisionContext collision, boolean rendering, @Nullable OpticalTarget transmitted, double end) {
        this.level = level;
        this.anchor = anchor;
        this.collision = collision;
        this.rendering = rendering;
        this.transmitted = transmitted;
        this.end = end;
    }

    static @Nullable BeamPropagation solve(Level level, BeamState source, PropagationBudget budget, LaserCollisionContext collision, boolean rendering, List<Trace> samples, BeamRayQuery query, BeamVolumeQuery volume, int regionLimit, int segmentLimit) {
        if (query.exhausted() || regionLimit < 1) {
            return null;
        }

        Trace centerTrace = samples.getFirst();
        while (!centerTrace.isComplete() && !query.exhausted()) {
            List<Segment> segments = centerTrace.segments();
            if (segments.isEmpty()) {
                break;
            }

            Segment last = segments.getLast();
            OpticalHit hit = last.hit();
            if (hit == null) {
                break;
            }

            OpticalSurface surface = hit.surface();
            BeamState start = last.start();
            if (surface.aperture() == null || start.profile().widthSlope() == 0 || last.absorbedFluxLm() != 0) {
                break;
            }

            BeamState outgoing = surface.interactor().interact(last.incident(), hit).outgoingBeam();
            Vec3 normal = surface.normal();
            if (outgoing == null || start.frame().direction().dot(normal) * outgoing.frame().direction().dot(normal) < 0) {
                break;
            }

            boolean common = true;
            for (Trace sample : samples) {
                List<Segment> sampled = sample.segments();
                if (sample.isComplete() || sampled.size() != segments.size()) {
                    common = false;
                    break;
                }

                Segment other = sampled.getLast();
                OpticalHit contact = other.hit();
                if (contact == null || other.absorbedFluxLm() != 0 || !hit.target().equals(contact.target()) || !surface.equals(contact.surface())) {
                    common = false;
                    break;
                }
            }
            if (!common) {
                break;
            }

            for (Trace sample : samples) {
                sample.advance(query);
            }
        }
        if (query.exhausted()) {
            return null;
        }

        List<Segment> center = centerTrace.segments();
        List<Segment> prefix = new ArrayList<>();
        for (int index = 0; index < center.size(); index++) {
            Segment segment = center.get(index);
            OpticalHit hit = segment.hit();
            if (hit == null) {
                break;
            }

            OpticalSurface surface = hit.surface();
            if (surface.aperture() == null || segment.absorbedFluxLm() != 0) {
                break;
            }

            Vec3 normal = surface.normal();
            OpticalInteraction interaction = surface.interactor().interact(segment.incident(), hit);
            BeamState outgoing = interaction.outgoingBeam();
            BeamState start = segment.start();
            if (outgoing == null || start.profile().widthSlope() != 0 && start.frame().direction().dot(normal) * outgoing.frame().direction().dot(normal) < 0) {
                break;
            }

            boolean common = true;
            for (Trace sample : samples) {
                List<Segment> segments = sample.segments();
                if (segments.size() <= index) {
                    common = false;
                    break;
                }

                Segment other = segments.get(index);
                OpticalHit contact = other.hit();
                if (contact == null || other.absorbedFluxLm() != 0 || !hit.target().equals(contact.target()) || !surface.equals(contact.surface())) {
                    common = false;
                    break;
                }
            }
            if (!common) {
                break;
            }

            prefix.add(segment);
        }
        int maxDepth = budget.maxDepth();
        if (prefix.isEmpty() || prefix.size() >= maxDepth) {
            return null;
        }

        double range = budget.range();
        BeamLinearTail linear = BeamLinearTail.create(source, prefix, range, volume);
        if (linear == null) {
            return null;
        }

        return solve(level, source, budget, collision, rendering, prefix, linear, query, volume, regionLimit, segmentLimit);
    }

    static @Nullable BeamPropagation solveTail(Level level, BeamState source, PropagationBudget budget, LaserCollisionContext collision, boolean rendering, BeamRayQuery query, BeamVolumeQuery volume, int regionLimit, int segmentLimit) {
        BeamFrame frame = source.frame();
        BeamState relative = new BeamState(new BeamFrame(Vec3.ZERO, frame.direction(), frame.transverseAxis()), source.profile(), source.fluxLm(), source.footprint());
        Focus focus = BeamPathReplay.focus(relative, budget.range());
        if (focus == null || focus.start() != 0) {
            return null;
        }

        BeamLinearTail linear = new BeamLinearTail(new Path(List.of(), source, budget.range(), focus), 0, 1);
        return solve(level, source, budget, collision, rendering, List.of(), linear, query, volume, regionLimit, segmentLimit);
    }

    private static @Nullable BeamPropagation solve(Level level, BeamState source, PropagationBudget budget, LaserCollisionContext collision, boolean rendering, List<Segment> prefix, BeamLinearTail linear, BeamRayQuery query, BeamVolumeQuery volume, int regionLimit, int segmentLimit) {
        int maxDepth = budget.maxDepth();
        double range = budget.range();
        Path initial = linear.path();
        Focus focus = initial.focus();
        if (focus == null) {
            return null;
        }

        BeamState initialState = initial.state();
        BeamFrame frame = initialState.frame();
        BeamRegion domain = initialState.footprint().domain();
        Vec3 direction = frame.direction();
        double slope = focus.slope();
        RayMap map = new RayMap(focus.origin(), direction, frame.transverseAxis().scale(slope), frame.getSecondTransverseAxis().scale(slope));
        OpticalTarget transmitted = null;
        if (!prefix.isEmpty()) {
            Segment entry = prefix.getLast();
            OpticalHit entryHit = entry.hit();
            if (entryHit != null) {
                Vec3 normal = entryHit.surface().normal();
                if (entry.start().frame().direction().dot(normal) * direction.dot(normal) >= 0) {
                    transmitted = entryHit.target();
                }
            }
        }
        BeamFrame sourceFrame = source.frame();
        double focusDistance = focus.distance();
        BeamTerminalPartition planner = new BeamTerminalPartition(level, sourceFrame.origin(), collision, rendering, transmitted, initial.remainingRange() - focusDistance);
        ArrayDeque<Branch> pending = new ArrayDeque<>();
        pending.add(new Branch(domain, map, new Distance(linear.tailStart() - focusDistance, 0, 0, 1), List.of()));
        List<Leaf> leaves = new ArrayList<>();
        int regions = 1;
        int outputSegments = 0;
        int maxBeams = budget.maxBeams();
        while (!pending.isEmpty()) {
            Branch branch = pending.removeFirst();
            List<OpticalHit> path = branch.path();
            if (prefix.size() + path.size() >= maxDepth) {
                return null;
            }

            List<Face> faces = planner.faces(branch, segmentLimit);
            if (faces == null) {
                return null;
            }

            List<Piece> pieces = planner.partition(branch.domain(), faces, regionLimit - regions + 1, segmentLimit);
            if (pieces == null) {
                return null;
            }

            regions += pieces.size() - 1;
            for (Piece piece : pieces) {
                Face face = piece.face();
                OpticalHit hit = null;
                if (face != null) {
                    hit = face.hit();
                    OpticalSurface surface = hit.surface();
                    if (surface.aperture() != null) {
                        List<OpticalHit> reflected = new ArrayList<>(path);
                        reflected.add(hit);
                        if (surface.apertureFrame() != null) {
                            outputSegments += prefix.size() + reflected.size() + 1;
                            leaves.add(new Leaf(piece.region(), List.copyOf(reflected), null));
                            continue;
                        }

                        pending.addLast(new Branch(piece.region(), branch.map().reflect(hit.position().subtract(planner.anchor), surface.normal()), face.distance(), List.copyOf(reflected)));
                        continue;
                    }
                }

                outputSegments += prefix.size() + path.size() + 1;
                if (outputSegments > segmentLimit || outputSegments > maxBeams) {
                    return null;
                }

                leaves.add(new Leaf(piece.region(), path, hit));
            }
        }
        List<Segment> segments = new ArrayList<>();
        List<Termination> terminations = new ArrayList<>();
        double sourceFlux = source.fluxLm();
        for (Leaf leaf : leaves) {
            BeamRegion region = leaf.region();
            if (linear.verticalSign() < 0) {
                region = new BeamRegion(region.vertices().reversed().stream().map(point -> new Point(point.u(), -point.v())).toList());
            }
            Point regionCenter = region.center();
            double minU = Double.POSITIVE_INFINITY;
            double maxU = Double.NEGATIVE_INFINITY;
            double minV = Double.POSITIVE_INFINITY;
            double maxV = Double.NEGATIVE_INFINITY;
            for (Point point : region.vertices()) {
                double u = point.u();
                double v = point.v();
                minU = Math.min(minU, u);
                maxU = Math.max(maxU, u);
                minV = Math.min(minV, v);
                maxV = Math.max(maxV, v);
            }
            BeamFootprint footprint = new BeamFootprint((maxU - minU) / 2, (maxV - minV) / 2, regionCenter.u(), regionCenter.v(), region);
            double flux = sourceFlux * region.area() / source.footprint().domain().area();
            if (flux < budget.minFluxLm()) {
                Vec3 point = BeamContact.point(source, 0, footprint.horizontalOffset(), footprint.verticalOffset());
                BeamState remainder = new BeamState(new BeamFrame(point, sourceFrame.direction(), sourceFrame.transverseAxis()), source.profile(), flux, footprint);
                terminations.add(new Termination(-1, remainder, Reason.BELOW_MIN_POWER));
                continue;
            }

            List<OpticalHit> contacts = new ArrayList<>();
            for (Segment segment : prefix) {
                OpticalHit hit = segment.hit();
                if (hit == null) {
                    return null;
                }

                contacts.add(hit);
            }
            List<OpticalHit> leafPath = leaf.path();
            contacts.addAll(leafPath);
            if (!leafPath.isEmpty() && contacts.getLast().surface().apertureFrame() != null) {
                BeamPropagation continued = BeamLensContinuation.solve(level, collision, rendering, source, contacts, region, budget, query, volume, regionLimit - regions + 1, segmentLimit - segments.size());
                if (continued == null) {
                    return null;
                }

                regions += continued.occlusionWork().sampledRegions() - 1;
                int offset = segments.size();
                for (Segment segment : continued.segments()) {
                    int parent = segment.parentIndex();
                    if (parent >= 0) {
                        parent += offset;
                    }
                    segments.add(new Segment(parent, segment.start(), segment.incident(), segment.length(), segment.remainingRange(), segment.hit(), segment.absorbedFluxLm(), segment.contact()));
                }
                for (Termination termination : continued.terminations()) {
                    int parent = termination.parentIndex();
                    if (parent >= 0) {
                        parent += offset;
                    }
                    terminations.add(new Termination(parent, termination.state(), termination.reason()));
                }
                continue;
            }

            Path replayed = BeamPathReplay.replay(source, contacts, range, footprint, flux, true);
            if (replayed == null) {
                return null;
            }

            int offset = segments.size();
            for (Segment segment : replayed.segments()) {
                int parent = segment.parentIndex();
                if (parent >= 0) {
                    parent += offset;
                }
                segments.add(new Segment(parent, segment.start(), segment.incident(), segment.length(), segment.remainingRange(), segment.hit(), segment.absorbedFluxLm(), segment.contact()));
            }
            int parent = -1;
            if (!replayed.segments().isEmpty()) {
                parent = segments.size() - 1;
            }
            BeamState state = replayed.state();
            double remaining = replayed.remainingRange();
            double length = remaining;
            OpticalHit hit = leaf.terminal();
            BeamContact contact = null;
            double absorbed = 0;
            if (hit != null) {
                BeamFootprint current = state.footprint();
                Vertex vertex = BeamContact.intersect(state, state.profile().spans(remaining), hit, current.horizontalOffset(), current.verticalOffset());
                if (vertex == null) {
                    return null;
                }

                length = vertex.distance();
                if (length <= OpticalWorldQuery.TRACE_OFFSET) {
                    return null;
                }

                Vec3 position = vertex.position();
                OpticalSurface surface = hit.surface();
                Vec3 normal = surface.normal();
                for (Axis axis : Axis.values()) {
                    if (Math.abs(axis.choose(normal.x, normal.y, normal.z)) != 1) {
                        continue;
                    }

                    Vec3 plane = hit.position();
                    position = switch (axis) {
                        case X -> new Vec3(plane.x, position.y, position.z);
                        case Y -> new Vec3(position.x, plane.y, position.z);
                        case Z -> new Vec3(position.x, position.y, plane.z);
                    };
                }
                OpticalTarget target = hit.target();
                hit = new OpticalHit(target, new BlockHitResult(position, hit.localHit().getDirection(), target.blockPos(), false), position, surface, hit.localPlaneNormal());
                contact = BeamContact.create(state, remaining, hit);
                if (contact == null) {
                    return null;
                }

                absorbed = flux;
            }
            BeamState incident = state.advance(length);
            segments.add(new Segment(parent, state, incident, length, remaining, hit, absorbed, contact));
            BeamState remainder = incident;
            Reason reason = Reason.RANGE_EXHAUSTED;
            if (hit != null) {
                remainder = new BeamState(incident.frame(), incident.profile(), 0, incident.footprint());
                reason = Reason.ABSORBED;
            }
            terminations.add(new Termination(segments.size() - 1, remainder, reason));
        }
        if (segments.size() > segmentLimit || segments.size() > maxBeams) {
            return null;
        }

        BeamPropagation result = new BeamPropagation(source, segments, terminations, new OcclusionWork(regions, 0));
        if (Math.abs(result.getAbsorbedFluxLm() + result.getRemainingFluxLm() - sourceFlux) > sourceFlux * FLUX_RELATIVE_TOLERANCE) {
            return null;
        }

        return result;
    }

    private @Nullable @Unmodifiable List<Face> faces(Branch branch, int limit) {
        RayMap map = branch.map();
        BeamRegion domain = branch.domain();
        Distance start = branch.start();
        List<Vec3> swept = new ArrayList<>();
        for (Point point : domain.vertices()) {
            swept.add(anchor.add(map.at(point, start.at(point))));
            swept.add(anchor.add(map.at(point, end)));
        }
        Vec3 first = swept.getFirst();
        AABB bounds = new AABB(first, first);
        for (Vec3 point : swept) {
            bounds = bounds.minmax(new AABB(point, point));
        }
        float partialTick = collision.getPartialTick();
        for (LocalRegion region : SubLevelBridge.localRegions(level, bounds, partialTick, rendering)) {
            if (!region.origin().equals(Vec3.ZERO) || !region.axisX().equals(new Vec3(1, 0, 0)) || !region.axisY().equals(new Vec3(0, 1, 0)) || !region.axisZ().equals(new Vec3(0, 0, 1))) {
                return null;
            }
        }
        for (WeakReference<AbstractContraptionEntity> reference : ContraptionHandler.loadedContraptions.get(level).values()) {
            AbstractContraptionEntity entity = reference.get();
            if (entity == null || !entity.isAlive()) {
                continue;
            }

            Contraption contraption = entity.getContraption();
            if (contraption == null || contraption.bounds == null) {
                continue;
            }

            Vec3 position = entity.getAnchorVec();
            Vec3 offset = entity.getPrevAnchorVec().lerp(position, partialTick).subtract(position);
            List<Vec3> local = swept.stream().map(point -> entity.toLocalVector(point.subtract(offset), partialTick)).toList();
            for (int index = 2; index + 3 < local.size(); index += 2) {
                Vec3 nextStart = local.get(index + 2);
                Vec3 nextEnd = local.get(index + 3);
                List<Vec3> prism = List.of(local.getFirst(), local.get(index), nextStart, nextStart, local.get(1), local.get(index + 1), nextEnd, nextEnd);
                if (!BeamVolumeQuery.intersects(prism, contraption.bounds)) {
                    continue;
                }

                return null;
            }
        }
        LaserTraceGeometry geometry = collision.getGeometry();
        geometry.includeBounds(bounds);
        Point center = domain.center();
        ClipContext context = new ClipContext(anchor.add(map.at(center, start.at(center))), anchor.add(map.at(center, end)), Block.COLLIDER, Fluid.NONE, collision);
        Map<FaceKey, Face> faces = new LinkedHashMap<>();
        Vec3 direction = map.direction();
        double startDistance = start.distance();
        for (BlockPos pos : new BeamVolumeCells(List.of(swept), bounds.inflate(LaserBlockClip.NEIGHBOR_REACH))) {
            if (level.isOutsideBuildHeight(pos)) {
                continue;
            }

            if (++checked > MAX_BLOCKS || !geometry.isLoaded(level, pos)) {
                return null;
            }

            if (branch.path().isEmpty() && transmitted != null && transmitted.blockPos().equals(pos)) {
                continue;
            }

            PreparedBlock block = geometry.getWorldBlock(level, pos, context, partialTick);
            if (block == null) {
                continue;
            }

            VoxelShape shape = block.shape();
            if (shape.isEmpty()) {
                continue;
            }

            OpticalGeometry optical = block.optical();
            Vec3 localFocus = anchor.subtract(Vec3.atLowerCornerOf(pos)).add(map.focus());
            Vec3 localD = direction;
            Vec3 localU = map.u();
            Vec3 localV = map.v();
            Vec3 axisX = new Vec3(1, 0, 0);
            Vec3 axisY = new Vec3(0, 1, 0);
            Vec3 axisZ = new Vec3(0, 0, 1);
            List<AABB> boxes = block.boxes();
            if (shape instanceof AmethystMirrorShape mirror) {
                Vec3 zero = mirror.toPanelCoordinates(Vec3.ZERO);
                Vec3 x = mirror.toPanelCoordinates(axisX).subtract(zero);
                Vec3 y = mirror.toPanelCoordinates(axisY).subtract(zero);
                Vec3 z = mirror.toPanelCoordinates(axisZ).subtract(zero);
                axisX = new Vec3(x.x, y.x, z.x);
                axisY = new Vec3(x.y, y.y, z.y);
                axisZ = new Vec3(x.z, y.z, z.z);
                localFocus = mirror.toPanelCoordinates(localFocus);
                localD = new Vec3(localD.dot(axisX), localD.dot(axisY), localD.dot(axisZ));
                localU = new Vec3(localU.dot(axisX), localU.dot(axisY), localU.dot(axisZ));
                localV = new Vec3(localV.dot(axisX), localV.dot(axisY), localV.dot(axisZ));
                boxes = mirror.getSurfaceBoxes();
            }
            else if (block.precise()) {
                boxes = shape.toAabbs();
            }
            for (AABB box : boxes) {
                for (Direction side : Iterate.directions) {
                    Axis axis = side.getAxis();
                    int sign = side.getAxisDirection().getStep();
                    double plane = sign > 0 ? box.max(axis) : box.min(axis);
                    double distance = (axis.choose(localFocus.x, localFocus.y, localFocus.z) - plane) * sign;
                    if (distance <= EPSILON) {
                        continue;
                    }

                    double u = -sign * axis.choose(localU.x, localU.y, localU.z);
                    double v = -sign * axis.choose(localV.x, localV.y, localV.z);
                    double constant = -sign * axis.choose(localD.x, localD.y, localD.z);
                    BeamRegion region = domain.clip(-end * u, -end * v, distance - end * constant).clip(startDistance * u - distance * start.u(), startDistance * v - distance * start.v(), startDistance * constant - distance * start.constant());
                    Vec3 contactU = localFocus.scale(u).add(localU.scale(distance));
                    Vec3 contactV = localFocus.scale(v).add(localV.scale(distance));
                    Vec3 contactOrigin = localFocus.scale(constant).add(localD.scale(distance));
                    for (Axis other : Axis.values()) {
                        if (axis == other) {
                            continue;
                        }

                        double a = other.choose(contactU.x, contactU.y, contactU.z);
                        double b = other.choose(contactV.x, contactV.y, contactV.z);
                        double c = other.choose(contactOrigin.x, contactOrigin.y, contactOrigin.z);
                        region = region.clip(box.min(other) * u - a, box.min(other) * v - b, box.min(other) * constant - c).clip(a - box.max(other) * u, b - box.max(other) * v, c - box.max(other) * constant);
                    }
                    if (region.area() <= MIN_PARTITION_AREA) {
                        continue;
                    }

                    Distance projected = new Distance(distance, u, v, constant);
                    Point point = region.center();
                    double projectedDistance = projected.at(point);
                    double startAtPoint = start.at(point);
                    if (projectedDistance < startAtPoint - EPSILON || !branch.path().isEmpty() && projectedDistance <= startAtPoint + EPSILON) {
                        continue;
                    }

                    if (optical != null && !(shape instanceof AmethystMirrorShape) && !(block.state().getBlock() instanceof AmethystLensBlock) && !optical.hasOpaqueBoxSurfaces()) {
                        return null;
                    }

                    Vec3 normal = switch (axis) {
                        case X -> axisX.scale(sign);
                        case Y -> axisY.scale(sign);
                        case Z -> axisZ.scale(sign);
                    };
                    Vec3 position = anchor.add(map.at(point, projectedDistance));
                    if (!(shape instanceof AmethystMirrorShape)) {
                        double coordinate = axis.choose(pos.getX(), pos.getY(), pos.getZ()) + plane;
                        position = switch (axis) {
                            case X -> new Vec3(coordinate, position.y, position.z);
                            case Y -> new Vec3(position.x, coordinate, position.z);
                            case Z -> new Vec3(position.x, position.y, coordinate);
                        };
                    }
                    BlockHitResult blockHit = new BlockHitResult(position, Direction.getNearest(normal.x, normal.y, normal.z), pos, false);
                    OpticalSurface surface = optical == null ? OpticalSurface.opaque(blockHit) : optical.getSurface(blockHit, direction);
                    Vec3 surfaceNormal = surface.normal();
                    if (surfaceNormal.distanceToSqr(normal) > SURFACE_NORMAL_TOLERANCE_SQR || surface.aperture() == null && surface.interactor() != OpticalInteractor.ABSORBING) {
                        return null;
                    }

                    OpticalTarget target = new OpticalTarget(level.dimension(), pos.immutable(), null, false);
                    FaceKey key = new FaceKey(target, side, plane, surface);
                    Face face = faces.get(key);
                    if (face == null) {
                        face = new Face(new OpticalHit(target, blockHit, position, surface, surfaceNormal), projected, new Area());
                        faces.put(key, face);
                    }
                    face.area().add(region.toArea());
                    if (faces.size() > limit) {
                        return null;
                    }
                }
            }
        }
        return List.copyOf(faces.values());
    }

    private @Nullable @Unmodifiable List<Piece> partition(BeamRegion domain, List<Face> faces, int regionLimit, int comparisonLimit) {
        List<Piece> pieces = new ArrayList<>();
        Area miss = domain.toArea();
        for (int index = 0; index < faces.size(); index++) {
            Face face = faces.get(index);
            Area visible = new Area(face.area());
            Distance distance = face.distance();
            for (int otherIndex = 0; otherIndex < faces.size(); otherIndex++) {
                if (index == otherIndex) {
                    continue;
                }

                Face other = faces.get(otherIndex);
                Area otherArea = other.area();
                if (!visible.getBounds2D().intersects(otherArea.getBounds2D())) {
                    continue;
                }

                if (++comparisons > comparisonLimit) {
                    return null;
                }

                Distance candidate = other.distance();
                double candidateDistance = candidate.distance();
                double faceDistance = distance.distance();
                double u = candidateDistance * distance.u() - faceDistance * candidate.u();
                double v = candidateDistance * distance.v() - faceDistance * candidate.v();
                double constant = candidateDistance * distance.constant() - faceDistance * candidate.constant();
                Area nearer = new Area(otherArea);
                if (Math.abs(u) + Math.abs(v) + Math.abs(constant) <= EPSILON) {
                    if (otherIndex > index) {
                        continue;
                    }
                }
                else {
                    nearer.intersect(domain.clip(u, v, constant).toArea());
                }
                visible.subtract(nearer);
            }
            miss.subtract(visible);
            List<BeamRegion> regions = BeamRegion.partition(visible, regionLimit - pieces.size());
            if (BeamRegion.area(visible) > MIN_PARTITION_AREA && regions.isEmpty()) {
                return null;
            }

            for (BeamRegion region : regions) {
                pieces.add(new Piece(region, face));
            }
        }
        List<BeamRegion> remaining = BeamRegion.partition(miss, regionLimit - pieces.size());
        if (BeamRegion.area(miss) > MIN_PARTITION_AREA && remaining.isEmpty()) {
            return null;
        }

        for (BeamRegion region : remaining) {
            pieces.add(new Piece(region, null));
        }
        return List.copyOf(pieces);
    }

    private record RayMap(Vec3 focus, Vec3 direction, Vec3 u, Vec3 v) {
        Vec3 at(Point point, double distance) {
            return focus.add(direction.add(u.scale(point.u())).add(v.scale(point.v())).scale(distance));
        }

        RayMap reflect(Vec3 point, Vec3 normal) {
            return new RayMap(focus.subtract(normal.scale(2 * focus.subtract(point).dot(normal))), direction.subtract(normal.scale(2 * direction.dot(normal))), u.subtract(normal.scale(2 * u.dot(normal))), v.subtract(normal.scale(2 * v.dot(normal))));
        }
    }

    private record Distance(double distance, double u, double v, double constant) {
        double at(Point point) {
            return distance / Math.fma(u, point.u(), Math.fma(v, point.v(), constant));
        }
    }

    private record Branch(BeamRegion domain, RayMap map, Distance start, List<OpticalHit> path) {}

    private record FaceKey(OpticalTarget target, Direction side, double plane, OpticalSurface surface) {}

    private record Face(OpticalHit hit, Distance distance, Area area) {}

    private record Piece(BeamRegion region, @Nullable Face face) {}

    private record Leaf(BeamRegion region, List<OpticalHit> path, @Nullable OpticalHit terminal) {}
}
