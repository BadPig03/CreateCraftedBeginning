package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
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
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalAperture;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceGeometry.PreparedBlock;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.LocalRegion;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.geom.Area;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BeamPlanarPartition {
    private static final int MAX_BLOCKS = 4096;
    private static final double EPSILON = 1.0E-9;
    private static final double SURFACE_NORMAL_TOLERANCE_SQR = 1.0E-10;
    private static final double MIN_PARTITION_AREA = 1.0E-12;
    private static final double FLUX_RELATIVE_TOLERANCE = 1.0E-10;

    private final Level level;
    private final Vec3 anchor;
    private final LaserCollisionContext collision;
    private final boolean rendering;
    private final PropagationBudget budget;
    private int checked;
    private int comparisons;

    private BeamPlanarPartition(Level level, Vec3 anchor, LaserCollisionContext collision, boolean rendering, PropagationBudget budget) {
        this.level = level;
        this.anchor = anchor;
        this.collision = collision;
        this.rendering = rendering;
        this.budget = budget;
    }

    static @Nullable BeamPropagation solve(Level level, BeamState source, PropagationBudget budget, LaserCollisionContext collision, boolean rendering, BeamRayQuery query, int regionLimit, int segmentLimit) {
        BeamProfile profile = source.profile();
        if (profile.widthSlope() != 0 || regionLimit < 1) {
            return null;
        }

        BeamFrame frame = source.frame();
        Vec3 sourceOrigin = frame.origin();
        BeamFootprint footprint = source.footprint();
        BeamRegion domain = footprint.domain();
        double sourceFlux = source.fluxLm();
        double radius = profile.radius();
        Vec3 sourceDirection = frame.direction();
        Vec3 transverse = frame.transverseAxis();
        Vec3 u = transverse.scale(radius);
        Vec3 v = frame.getSecondTransverseAxis().scale(radius);
        RayMap map = new RayMap(u.scale(-footprint.horizontalOffset()).subtract(v.scale(footprint.verticalOffset())), u, v);
        ArrayDeque<Branch> pending = new ArrayDeque<>();
        pending.add(new Branch(domain, map, sourceDirection, new Distance(0, 0, 0), List.of()));
        BeamPlanarPartition planner = new BeamPlanarPartition(level, sourceOrigin, collision, rendering, budget);
        List<Leaf> leaves = new ArrayList<>();
        int regions = 1;
        int outputSegments = 0;
        while (!pending.isEmpty()) {
            Branch branch = pending.removeFirst();
            List<Face> prefix = branch.prefix();
            if (prefix.size() >= budget.maxDepth()) {
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
                List<Face> path = new ArrayList<>(prefix);
                if (face != null) {
                    path.add(face);
                }
                BeamRegion pieceRegion = piece.region();
                OpticalSurface surface = face == null ? null : face.surface();
                if (surface != null && surface.aperture() != null) {
                    Vec3 direction = branch.direction();
                    Vec3 normal = surface.normal();
                    Vec3 reflected = direction.subtract(normal.scale(2 * direction.dot(normal))).normalize();
                    Distance distance = face.distance().affine();
                    pending.addLast(new Branch(pieceRegion, branch.map().advance(direction, distance), reflected, branch.traveled().add(distance), List.copyOf(path)));
                    continue;
                }

                int count = path.size();
                if (face == null) {
                    count++;
                }
                outputSegments += count;
                if (outputSegments > segmentLimit) {
                    return null;
                }

                leaves.add(new Leaf(pieceRegion, List.copyOf(path), face == null));
            }
        }
        List<Segment> segments = new ArrayList<>();
        List<Termination> terminations = new ArrayList<>();
        for (Leaf leaf : leaves) {
            BeamRegion region = leaf.region();
            Point center = region.center();
            List<Point> vertices = region.vertices();
            double minU = vertices.stream().mapToDouble(Point::u).min().orElseThrow();
            double maxU = vertices.stream().mapToDouble(Point::u).max().orElseThrow();
            double minV = vertices.stream().mapToDouble(Point::v).min().orElseThrow();
            double maxV = vertices.stream().mapToDouble(Point::v).max().orElseThrow();
            BeamFootprint restricted = new BeamFootprint((maxU - minU) / 2, (maxV - minV) / 2, center.u(), center.v(), region);
            double flux = sourceFlux * region.area() / domain.area();
            BeamState state = new BeamState(new BeamFrame(sourceOrigin.add(map.at(center)), sourceDirection, transverse), profile, flux, restricted);
            if (flux < budget.minFluxLm()) {
                terminations.add(new Termination(-1, state, Reason.BELOW_MIN_POWER));
                continue;
            }

            int parent = -1;
            double remaining = budget.range();
            for (Face face : leaf.path()) {
                OpticalQueryResult result = query.findNext(state, remaining);
                if (result == null || query.exhausted()) {
                    return null;
                }

                OpticalHit hit = result.hit();
                if (hit == null) {
                    return null;
                }

                OpticalTarget target = hit.target();
                OpticalSurface surface = hit.surface();
                OpticalSurface planned = face.surface();
                OpticalInteractor interactor = surface.interactor();
                if (!target.equals(face.target()) || surface.normal().distanceToSqr(planned.normal()) > SURFACE_NORMAL_TOLERANCE_SQR || (surface.aperture() == null) != (planned.aperture() == null) || !interactor.equals(planned.interactor())) {
                    return null;
                }

                double length = result.length();
                BeamState incident = state.advance(length);
                BeamContact contact = BeamContact.create(state, remaining, hit);
                if (contact == null) {
                    return null;
                }

                OpticalInteraction interaction = interactor.interact(incident, hit);
                segments.add(new Segment(parent, state, incident, length, remaining, hit, interaction.absorbedFluxLm(), contact));
                parent = segments.size() - 1;
                remaining -= length;
                BeamState outgoing = interaction.outgoingBeam();
                if (outgoing == null) {
                    terminations.add(new Termination(parent, new BeamState(incident.frame(), incident.profile(), 0, incident.footprint()), Reason.ABSORBED));
                    break;
                }

                if (!outgoing.profile().equals(state.profile()) || outgoing.fluxLm() != flux || outgoing.footprint().region() == null) {
                    return null;
                }

                state = outgoing;
            }
            if (!leaf.miss()) {
                continue;
            }

            BeamState end = state.advance(remaining);
            segments.add(new Segment(parent, state, end, remaining, remaining, null, 0));
            terminations.add(new Termination(segments.size() - 1, end, Reason.RANGE_EXHAUSTED));
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
        Vec3 direction = branch.direction();
        Distance remaining = new Distance(0, 0, budget.range()).subtract(branch.traveled());
        RayMap end = map.advance(direction, remaining);
        List<Point> vertices = domain.vertices();
        List<Vec3> swept = new ArrayList<>();
        for (Point vertex : vertices) {
            swept.add(anchor.add(map.at(vertex)));
            swept.add(anchor.add(end.at(vertex)));
        }
        Vec3 first = swept.getFirst();
        AABB bounds = new AABB(first, first);
        for (Vec3 point : swept) {
            bounds = bounds.minmax(new AABB(point, point));
        }
        float partialTick = collision.getPartialTick();
        LaserTraceGeometry geometry = collision.getGeometry();
        geometry.includeBounds(bounds);
        Point domainCenter = domain.center();
        ClipContext context = new ClipContext(anchor.add(map.at(domainCenter)), anchor.add(end.at(domainCenter)), Block.COLLIDER, Fluid.NONE, collision);
        Map<FaceKey, Face> faces = new LinkedHashMap<>();
        List<LaserQuerySpace> spaces = new ArrayList<>();
        for (LocalRegion region : SubLevelBridge.localRegions(level, bounds.inflate(LaserBlockClip.NEIGHBOR_REACH), partialTick, rendering)) {
            spaces.add(LaserQuerySpace.world(level, region));
        }
        for (LaserQuerySpace space : geometry.mounted(level, partialTick)) {
            if (!space.worldBounds().intersects(bounds)) {
                continue;
            }

            spaces.add(space);
        }
        for (LaserQuerySpace space : spaces) {
            List<Vec3> localSwept = swept.stream().map(space::localPosition).toList();
            AABB localBounds = space.bounds().inflate(LaserBlockClip.NEIGHBOR_REACH);
            Map<BlockPos, StructureBlockInfo> mountedBlocks = space.blocks();
            for (BlockPos pos : new BeamVolumeCells(List.of(localSwept), localBounds)) {
                PreparedBlock block;
                if (mountedBlocks != null) {
                    block = geometry.getMountedBlock(space.owner(), mountedBlocks, pos, collision);
                    if (block == null) {
                        continue;
                    }

                    if (++checked > MAX_BLOCKS) {
                        return null;
                    }
                }
                else {
                    if (level.isOutsideBuildHeight(pos)) {
                        continue;
                    }

                    if (++checked > MAX_BLOCKS || !geometry.isLoaded(level, pos)) {
                        return null;
                    }

                    block = geometry.getWorldBlock(level, pos, context, partialTick);
                    if (block == null) {
                        continue;
                    }
                }

                VoxelShape shape = block.shape();
                if (shape.isEmpty()) {
                    continue;
                }

                OpticalGeometry optical = block.optical();
                Vec3 mapU = space.localVector(map.u());
                Vec3 mapV = space.localVector(map.v());
                Vec3 localAnchor = space.localPosition(anchor, Vec3.atLowerCornerOf(pos));
                RayMap local = new RayMap(localAnchor.add(space.localVector(map.origin())), mapU, mapV);
                Vec3 ownerDirection = space.localVector(direction);
                Vec3 localDirection = ownerDirection;
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
                    local = new RayMap(mirror.toPanelCoordinates(local.origin()), new Vec3(mapU.dot(axisX), mapU.dot(axisY), mapU.dot(axisZ)), new Vec3(mapV.dot(axisX), mapV.dot(axisY), mapV.dot(axisZ)));
                    localDirection = new Vec3(ownerDirection.dot(axisX), ownerDirection.dot(axisY), ownerDirection.dot(axisZ));
                    boxes = mirror.getSurfaceBoxes();
                }
                else if (block.precise()) {
                    boxes = shape.toAabbs();
                }
                Vec3 localOrigin = local.origin();
                Vec3 localU = local.u();
                Vec3 localV = local.v();
                for (AABB box : boxes) {
                    for (Direction side : Iterate.directions) {
                        Axis axis = side.getAxis();
                        AxisDirection axisDirection = side.getAxisDirection();
                        int sign = axisDirection.getStep();
                        double incidence = axis.choose(localDirection.x, localDirection.y, localDirection.z) * sign;
                        if (incidence >= 0) {
                            continue;
                        }

                        double plane = axisDirection == AxisDirection.POSITIVE ? box.max(axis) : box.min(axis);
                        double denominator = Math.abs(incidence);
                        double orientation = -sign;
                        double numeratorU = -axis.choose(localU.x, localU.y, localU.z) * orientation;
                        double numeratorV = -axis.choose(localV.x, localV.y, localV.z) * orientation;
                        double numeratorConstant = (plane - axis.choose(localOrigin.x, localOrigin.y, localOrigin.z)) * orientation;
                        Distance numerator = new Distance(numeratorU, numeratorV, numeratorConstant);
                        FaceDistance distance = new FaceDistance(numerator, denominator);
                        BeamRegion region = domain.clip(-numeratorU, -numeratorV, -numeratorConstant);
                        Distance range = numerator.subtract(remaining.scale(denominator));
                        region = region.clip(range.u(), range.v(), range.constant());
                        Vec3 contactU = localU.scale(denominator).add(localDirection.scale(numeratorU));
                        Vec3 contactV = localV.scale(denominator).add(localDirection.scale(numeratorV));
                        Vec3 contactOrigin = localOrigin.scale(denominator).add(localDirection.scale(numeratorConstant));
                        for (Axis other : Axis.values()) {
                            if (axis == other) {
                                continue;
                            }

                            double u = other.choose(contactU.x, contactU.y, contactU.z);
                            double v = other.choose(contactV.x, contactV.y, contactV.z);
                            double constant = other.choose(contactOrigin.x, contactOrigin.y, contactOrigin.z);
                            region = region.clip(-u, -v, box.min(other) * denominator - constant).clip(u, v, constant - box.max(other) * denominator);
                        }
                        if (region.area() <= MIN_PARTITION_AREA) {
                            continue;
                        }

                        Point center = region.center();
                        double centerDistance = distance.at(center);
                        if (centerDistance <= EPSILON) {
                            continue;
                        }

                        if (optical != null && !(shape instanceof AmethystMirrorShape) && !optical.hasOpaqueBoxSurfaces()) {
                            return null;
                        }

                        Vec3 normal = switch (axis) {
                            case X -> axisX.scale(sign);
                            case Y -> axisY.scale(sign);
                            case Z -> axisZ.scale(sign);
                        };
                        Vec3 position = space.localPosition(anchor.add(map.at(center)).add(direction.scale(centerDistance)));
                        BlockHitResult hit = new BlockHitResult(position, Direction.getNearest(normal.x, normal.y, normal.z), pos, false);
                        OpticalSurface surface = optical == null ? OpticalSurface.opaque(hit) : optical.getSurface(hit, ownerDirection.normalize());
                        Vec3 surfaceNormal = surface.normal();
                        OpticalAperture aperture = surface.aperture();
                        OpticalInteractor interactor = surface.interactor();
                        if (surfaceNormal.distanceToSqr(normal) > SURFACE_NORMAL_TOLERANCE_SQR || aperture == null && interactor != OpticalInteractor.ABSORBING) {
                            return null;
                        }

                        surface = surface.transform(space);
                        OpticalTarget target = new OpticalTarget(level.dimension(), pos.immutable(), space.id(), space.inSubLevel());
                        FaceKey key = new FaceKey(target, side, plane, surface);
                        Face face = faces.get(key);
                        if (face == null) {
                            face = new Face(target, surface, distance, new Area());
                            faces.put(key, face);
                        }
                        face.area().add(region.toArea());
                        if (faces.size() > limit) {
                            return null;
                        }
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
            FaceDistance faceDistance = face.distance();
            double denominator = faceDistance.denominator();
            Area visible = new Area(face.area());
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

                FaceDistance otherDistance = other.distance();
                double otherDenominator = otherDistance.denominator();
                Distance delta = otherDistance.numerator().scale(denominator).subtract(faceDistance.numerator().scale(otherDenominator));
                Area nearer = new Area(otherArea);
                double u = delta.u();
                double v = delta.v();
                double constant = delta.constant();
                if (Math.abs(u) + Math.abs(v) + Math.abs(constant) <= EPSILON * denominator * otherDenominator) {
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
        List<BeamRegion> regions = BeamRegion.partition(miss, regionLimit - pieces.size());
        if (BeamRegion.area(miss) > MIN_PARTITION_AREA && regions.isEmpty()) {
            return null;
        }

        for (BeamRegion region : regions) {
            pieces.add(new Piece(region, null));
        }
        return List.copyOf(pieces);
    }

    private record Distance(double u, double v, double constant) {
        Distance add(Distance other) {
            return new Distance(u + other.u(), v + other.v(), constant + other.constant());
        }

        Distance subtract(Distance other) {
            return new Distance(u - other.u(), v - other.v(), constant - other.constant());
        }

        Distance scale(double scale) {
            return new Distance(u * scale, v * scale, constant * scale);
        }

        double at(Point point) {
            return Math.fma(u, point.u(), Math.fma(v, point.v(), constant));
        }
    }

    private record RayMap(Vec3 origin, Vec3 u, Vec3 v) {
        Vec3 at(Point point) {
            return origin.add(u.scale(point.u())).add(v.scale(point.v()));
        }

        RayMap advance(Vec3 direction, Distance distance) {
            return new RayMap(origin.add(direction.scale(distance.constant())), u.add(direction.scale(distance.u())), v.add(direction.scale(distance.v())));
        }
    }

    private record Branch(BeamRegion domain, RayMap map, Vec3 direction, Distance traveled, List<Face> prefix) {}

    private record FaceKey(OpticalTarget target, Direction side, double plane, OpticalSurface surface) {}

    private record FaceDistance(Distance numerator, double denominator) {
        Distance affine() {
            return numerator.scale(1 / denominator);
        }

        double at(Point point) {
            return numerator.at(point) / denominator;
        }
    }

    private record Face(OpticalTarget target, OpticalSurface surface, FaceDistance distance, Area area) {}

    private record Piece(BeamRegion region, @Nullable Face face) {}

    private record Leaf(BeamRegion region, List<Face> path, boolean miss) {}
}
