package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile.Span;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceGeometry.PreparedBlock;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.LocalRegion;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BeamVolumeQuery {
    private static final int MAX_BLOCKS = 4096;

    private final Level level;
    private final LaserCollisionContext collision;
    private final boolean rendering;

    BeamVolumeQuery(Level level, LaserCollisionContext collision, boolean rendering) {
        this.level = level;
        this.collision = collision;
        this.rendering = rendering;
    }

    static boolean intersects(List<Vec3> points, AABB box) {
        return new BeamSweepVolume(points).intersects(box);
    }

    private static AABB bounds(List<BeamSweepVolume> volumes) {
        AABB result = volumes.getFirst().bounds();
        for (int index = 1; index < volumes.size(); index++) {
            result = result.minmax(volumes.get(index).bounds());
        }
        return result;
    }

    private static @Unmodifiable List<Vec3> section(BeamState beam, double distance) {
        BeamState advanced = beam.advance(distance);
        BeamFrame frame = advanced.frame();
        double radius = advanced.profile().radius();
        BeamFootprint footprint = beam.footprint();
        Vec3 horizontal = frame.transverseAxis().scale(radius * footprint.horizontalScale());
        Vec3 vertical = frame.getSecondTransverseAxis().scale(radius * footprint.verticalScale());
        Vec3 center = frame.origin();
        return List.of(center.subtract(horizontal).subtract(vertical), center.add(horizontal).subtract(vertical), center.subtract(horizontal).add(vertical), center.add(horizontal).add(vertical));
    }

    private static @Unmodifiable List<Vec3> section(BeamState beam, double distance, List<Point> domain, int parity) {
        List<Vec3> points = new ArrayList<>();
        for (Point point : domain) {
            double v = parity == 0 ? point.v() : -point.v();
            points.add(BeamContact.point(beam, distance, point.u(), v));
        }
        return List.copyOf(points);
    }

    private static @Unmodifiable List<BeamSweepVolume> volumes(BeamPropagation path, List<BeamPropagation> corners, int index, boolean polygon) {
        List<Segment> segments = path.segments();
        Segment segment = segments.get(index);
        BeamState beam = segment.start();
        BeamProfile profile = beam.profile();
        boolean varying = profile.widthSlope() != 0;
        List<Span> spans = profile.spans(segment.length());
        List<BeamSweepVolume> result = new ArrayList<>();
        List<Vec3> previous = section(beam, 0);
        int parent = segment.parentIndex();
        int parity = 0;
        Segment child = segment;
        int ancestor = parent;
        while (ancestor >= 0) {
            Segment predecessor = segments.get(ancestor);
            OpticalHit contact = predecessor.hit();
            if (contact != null) {
                Vec3 normal = contact.surface().normal();
                if (predecessor.incident().frame().direction().dot(normal) * child.start().frame().direction().dot(normal) < 0) {
                    parity ^= 2;
                }
            }
            child = predecessor;
            ancestor = child.parentIndex();
        }
        List<Point> domain = path.source().footprint().domain().vertices();
        if (polygon) {
            previous = section(beam, 0, domain, parity);
        }
        if (parent >= 0) {
            List<Vec3> starts = new ArrayList<>();
            for (int cornerIndex = 0; cornerIndex < corners.size(); cornerIndex++) {
                BeamPropagation corner = corners.get(polygon ? cornerIndex : cornerIndex ^ parity);
                starts.add(corner.segments().get(index).start().frame().origin());
            }
            previous = List.copyOf(starts);
        }
        for (int step = 0; step < spans.size(); step++) {
            Span span = spans.get(step);
            double distance = span.to();
            List<Vec3> next = polygon ? section(beam, distance, domain, parity) : section(beam, distance);
            if (step + 1 == spans.size()) {
                List<Vec3> ends = new ArrayList<>();
                for (int cornerIndex = 0; cornerIndex < corners.size(); cornerIndex++) {
                    BeamPropagation corner = corners.get(polygon ? cornerIndex : cornerIndex ^ parity);
                    ends.add(corner.segments().get(index).incident().frame().origin());
                }
                next = List.copyOf(ends);
            }
            List<Vec3> points = new ArrayList<>(previous);
            points.addAll(next);
            if (varying) {
                double middle = span.from() + (distance - span.from()) / 2;
                List<Vec3> midpoint = polygon ? section(beam, middle, domain, parity) : section(beam, middle);
                OpticalHit hit = segment.hit();
                for (Vec3 control : midpoint) {
                    if (hit != null) {
                        Vec3 normal = hit.surface().normal();
                        double behind = control.subtract(hit.position()).dot(normal);
                        if (behind < 0) {
                            control = control.subtract(normal.scale(behind));
                        }
                    }
                    points.add(control);
                }
            }
            if (polygon) {
                int size = domain.size();
                for (int vertex = 1; vertex + 1 < size; vertex++) {
                    List<Vec3> triangle = new ArrayList<>();
                    for (int offset = 0; offset < points.size(); offset += size) {
                        triangle.add(points.get(offset));
                        triangle.add(points.get(offset + vertex));
                        triangle.add(points.get(offset + vertex + 1));
                        triangle.add(points.get(offset + vertex + 1));
                    }
                    result.add(new BeamSweepVolume(triangle));
                }
            }
            else {
                result.add(new BeamSweepVolume(points));
            }
            previous = next;
        }
        return List.copyOf(result);
    }

    private static boolean intersects(PreparedBlock block, BlockPos pos, List<BeamSweepVolume> volumes, boolean includeShallowOverlap) {
        VoxelShape shape = block.shape();
        if (shape instanceof AmethystMirrorShape mirror) {
            Vec3 origin = Vec3.atLowerCornerOf(pos);
            List<BeamSweepVolume> local = transform(volumes, point -> mirror.toPanelCoordinates(point.subtract(origin)));
            for (AABB box : mirror.getPanelBoxes()) {
                for (BeamSweepVolume volume : local) {
                    if (volume.intersects(box, includeShallowOverlap)) {
                        return true;
                    }
                }
            }
            return false;
        }

        List<AABB> boxes = block.boxes();
        if (block.precise()) {
            boxes = shape.toAabbs();
        }
        for (AABB box : boxes) {
            AABB moved = box.move(pos);
            for (BeamSweepVolume volume : volumes) {
                if (volume.intersects(moved, includeShallowOverlap)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static @Unmodifiable List<BeamSweepVolume> transform(List<BeamSweepVolume> volumes, UnaryOperator<Vec3> transform) {
        List<BeamSweepVolume> result = new ArrayList<>();
        for (BeamSweepVolume volume : volumes) {
            List<Vec3> points = new ArrayList<>();
            for (Vec3 point : volume.points()) {
                points.add(transform.apply(point));
            }
            result.add(new BeamSweepVolume(points));
        }
        return List.copyOf(result);
    }

    private static boolean resolvedBoundary(BeamPropagation path, Segment segment, BlockPos pos, @Nullable UUID contraption) {
        OpticalHit hit = segment.hit();
        if (hit != null && (hit.surface().aperture() != null || BeamAperturePartition.isAbsorbed(segment))) {
            OpticalTarget target = hit.target();
            if (target.blockPos().equals(pos) && Objects.equals(target.contraptionId(), contraption)) {
                return true;
            }
        }
        int parent = segment.parentIndex();
        if (parent < 0) {
            return false;
        }

        OpticalHit previous = path.segments().get(parent).hit();
        if (previous == null || previous.surface().aperture() == null) {
            return false;
        }

        OpticalTarget target = previous.target();
        return target.blockPos().equals(pos) && Objects.equals(target.contraptionId(), contraption);
    }

    boolean hasUnseenObstacle(BeamPropagation path, List<BeamPropagation> corners) {
        return hasUnseenObstacle(path, corners, 0, false, false, false);
    }

    boolean hasUnseenPrefixObstacle(BeamPropagation path, List<BeamPropagation> corners) {
        return hasUnseenObstacle(path, corners, 0, false, false, true);
    }

    boolean hasUnseenRegionObstacle(BeamPropagation path, List<BeamPropagation> corners, int firstSegment, boolean includeStart) {
        return hasUnseenObstacle(path, corners, firstSegment, includeStart, true, false);
    }

    private boolean hasUnseenObstacle(BeamPropagation path, List<BeamPropagation> corners, int firstSegment, boolean includeStart, boolean polygon, boolean includeShallowOverlap) {
        LaserTraceGeometry geometry = collision.getGeometry();
        int checked = 0;
        float partialTick = collision.getPartialTick();
        List<Segment> segments = path.segments();
        List<Sweep> sweeps = new ArrayList<>();
        long directCells = 0;
        for (int index = firstSegment; index < segments.size(); index++) {
            List<BeamSweepVolume> volumes = volumes(path, corners, index, polygon);
            if (volumes.isEmpty()) {
                continue;
            }

            AABB bounds = bounds(volumes);
            geometry.includeBounds(bounds);
            List<LaserQuerySpace> spaces = new ArrayList<>();
            for (LocalRegion region : SubLevelBridge.localRegions(level, bounds, partialTick, rendering)) {
                spaces.add(LaserQuerySpace.world(level, region));
            }
            for (LaserQuerySpace space : geometry.mounted(level, partialTick)) {
                if (!space.worldBounds().intersects(bounds)) {
                    continue;
                }

                spaces.add(space);
            }
            for (LaserQuerySpace space : spaces) {
                List<BeamSweepVolume> localVolumes = transform(volumes, space::localPosition);
                AABB local = space.blocks() == null ? space.bounds().inflate(LaserBlockClip.NEIGHBOR_REACH) : bounds(localVolumes).inflate(LaserBlockClip.NEIGHBOR_REACH);
                BlockPos minimum = BlockPos.containing(local.minX, local.minY, local.minZ);
                BlockPos maximum = BlockPos.containing(local.maxX, local.maxY, local.maxZ);
                long count = (long) (maximum.getX() - minimum.getX() + 1) * (maximum.getY() - minimum.getY() + 1) * (maximum.getZ() - minimum.getZ() + 1);
                directCells += Math.min(count, MAX_BLOCKS + 1L);
                sweeps.add(new Sweep(segments.get(index), localVolumes, local, space, count));
            }
        }
        boolean sliced = directCells > MAX_BLOCKS;
        for (Sweep sweep : sweeps) {
            Segment segment = sweep.segment();
            LaserQuerySpace space = sweep.space();
            List<BeamSweepVolume> localVolumes = sweep.volumes();
            AABB local = sweep.bounds();
            Map<BlockPos, StructureBlockInfo> mountedBlocks = space.blocks();
            ClipContext context = new ClipContext(segment.start().frame().origin(), segment.incident().frame().origin(), Block.COLLIDER, Fluid.NONE, collision);
            Iterable<BlockPos> candidates = BlockPos.betweenClosed(BlockPos.containing(local.minX, local.minY, local.minZ), BlockPos.containing(local.maxX, local.maxY, local.maxZ));
            if (sliced || sweep.cells() > MAX_BLOCKS - checked) {
                candidates = new BeamVolumeCells(localVolumes.stream().map(BeamSweepVolume::points).toList(), local);
            }
            for (BlockPos pos : candidates) {
                if (!local.intersects(new AABB(pos))) {
                    continue;
                }

                if (mountedBlocks != null) {
                    PreparedBlock block = geometry.getMountedBlock(space.owner(), mountedBlocks, pos, collision);
                    if (block == null) {
                        continue;
                    }

                    if (++checked > MAX_BLOCKS) {
                        return true;
                    }

                    if ((includeStart || !resolvedBoundary(path, segment, pos, space.id())) && intersects(block, pos, localVolumes, includeShallowOverlap)) {
                        return true;
                    }

                    continue;
                }

                if (level.isOutsideBuildHeight(pos)) {
                    continue;
                }

                if (++checked > MAX_BLOCKS) {
                    return true;
                }

                if (!geometry.isLoaded(level, pos)) {
                    for (BeamSweepVolume volume : localVolumes) {
                        if (volume.intersects(new AABB(pos), includeShallowOverlap)) {
                            return true;
                        }
                    }
                    continue;
                }

                if (!includeStart && resolvedBoundary(path, segment, pos, null)) {
                    continue;
                }

                PreparedBlock block = geometry.getWorldBlock(level, pos, context, partialTick);
                if (block != null && intersects(block, pos, localVolumes, includeShallowOverlap)) {
                    return true;
                }
            }
        }
        return false;
    }

    private record Sweep(Segment segment, List<BeamSweepVolume> volumes, AABB bounds, LaserQuerySpace space, long cells) {}
}
