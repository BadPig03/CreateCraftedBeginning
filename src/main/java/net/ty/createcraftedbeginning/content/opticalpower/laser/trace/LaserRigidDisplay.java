package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Reason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Termination;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserStaticDependencies.Sample;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceGeometry.PreparedBlock;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RenderGeometry;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RenderSource;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LaserRigidDisplay(UUID host, Vec3 scale, AABB localBounds) {
    private static final int MAX_OBSERVED_BLOCKS = 4096;
    private static final int MAX_CANDIDATE_BLOCKS = 65536;

    public LaserRigidDisplay {
        if (!validScale(scale)) {
            throw new IllegalArgumentException("Rigid display scale must have finite positive components; got " + scale + '.');
        }
    }

    static @Nullable Candidate capture(Level level, BlockPos pos, BeamPropagation propagation, LaserTraceDependencies traced) {
        RenderSource owner = SubLevelBridge.captureRenderSource(level, Vec3.atCenterOf(pos), 1);
        AABB sourceBounds = SubLevelBridge.sourceBounds(level, Vec3.atCenterOf(pos));
        if (owner == null || sourceBounds == null || !validScale(owner.geometry().scale())) {
            return null;
        }

        for (Termination termination : propagation.terminations()) {
            Reason reason = termination.reason();
            if (reason != Reason.ABSORBED && reason != Reason.RANGE_EXHAUSTED && reason != Reason.BELOW_MIN_POWER) {
                return null;
            }
        }
        for (Segment segment : propagation.segments()) {
            OpticalHit hit = segment.hit();
            if (hit == null) {
                continue;
            }

            if (hit.target().contraptionId() != null || !hit.target().inSubLevel()) {
                return null;
            }

            RenderSource target = SubLevelBridge.captureRenderSource(level, Vec3.atCenterOf(hit.target().blockPos()), 1);
            if (target == null || !target.id().equals(owner.id())) {
                return null;
            }
        }
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        CoordinateTransform transform = SubLevelBridge.createLogicalTransform(level, Vec3.atCenterOf(pos));
        LaserRenderSnapshot reference = new LaserRenderSnapshot(propagation, origin, transform.transformPosition(origin), transform.transformNormal(new Vec3(1, 0, 0)), transform.transformNormal(new Vec3(0, 1, 0)), transform.transformNormal(new Vec3(0, 0, 1)));
        List<AABB> bounds = new ArrayList<>();
        for (AABB query : traced.bounds()) {
            AABB local = null;
            for (Vec3 corner : corners(query)) {
                Vec3 delta = corner.subtract(reference.worldOrigin());
                Vec3 point = origin.add(delta.dot(reference.xAxis()) / reference.xAxis().lengthSqr(), delta.dot(reference.yAxis()) / reference.yAxis().lengthSqr(), delta.dot(reference.zAxis()) / reference.zAxis().lengthSqr());
                AABB next = new AABB(point, point);
                local = local == null ? next : local.minmax(next);
            }
            AABB captured = Objects.requireNonNull(local, "Rigid laser display capture requires local bounds for each dependency box.");
            if (bounds.stream().anyMatch(previous -> contains(previous, captured))) {
                continue;
            }

            bounds.removeIf(previous -> contains(captured, previous));
            bounds.add(captured);
        }
        Map<BlockPos, Sample> samples = new LinkedHashMap<>();
        LaserCollisionContext collision = new LaserCollisionContext(1);
        ClipContext clip = new ClipContext(Vec3.ZERO, Vec3.ZERO, Block.COLLIDER, Fluid.NONE, collision);
        for (AABB bound : bounds) {
            AABB observed = sourceBounds.inflate(LaserBlockClip.NEIGHBOR_REACH);
            if (!bound.intersects(observed)) {
                continue;
            }

            bound = bound.intersect(observed);
            if (volume(bound) > MAX_OBSERVED_BLOCKS) {
                return null;
            }

            for (BlockPos candidate : BlockPos.betweenClosed(BlockPos.containing(bound.minX, bound.minY, bound.minZ), BlockPos.containing(bound.maxX, bound.maxY, bound.maxZ))) {
                if (samples.containsKey(candidate)) {
                    continue;
                }

                if (samples.size() >= MAX_OBSERVED_BLOCKS) {
                    return null;
                }

                PreparedBlock block = LaserTraceGeometry.prepareWorldBlock(level, candidate, clip, 1);
                if (block == null) {
                    return null;
                }

                OpticalGeometry optical = block.optical();
                List<Double> key = null;
                if (optical != null) {
                    key = optical.displayKey();
                    if (key == null) {
                        return null;
                    }
                }
                if (!block.shape().isEmpty() || optical != null) {
                    RenderSource target = SubLevelBridge.captureRenderSource(level, Vec3.atCenterOf(candidate), 1);
                    if (target == null || !target.id().equals(owner.id())) {
                        return null;
                    }
                }
                List<AABB> boxes = block.precise() ? block.shape().toAabbs() : block.boxes();
                BlockPos immutable = candidate.immutable();
                samples.put(immutable, new Sample(immutable, block.state(), boxes, block.precise(), key));
            }
        }
        LaserRigidDisplay rigid = new LaserRigidDisplay(owner.id(), owner.geometry().scale(), sourceBounds);
        LaserStaticDependencies dependencies = new LaserStaticDependencies(List.copyOf(samples.values()), List.copyOf(samples.keySet()), bounds);
        if (!rigid.matches(level, pos, dependencies, 1)) {
            return null;
        }

        return new Candidate(rigid, dependencies, reference);
    }

    private static @Unmodifiable List<Vec3> corners(AABB box) {
        return List.of(new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.minX, box.minY, box.maxZ), new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.minX, box.maxY, box.maxZ), new Vec3(box.maxX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.maxZ), new Vec3(box.maxX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.maxZ));
    }

    private static boolean validScale(Vec3 scale) {
        return Double.isFinite(scale.x) && Double.isFinite(scale.y) && Double.isFinite(scale.z) && scale.x > 0 && scale.y > 0 && scale.z > 0;
    }

    private static double volume(AABB box) {
        return (Math.floor(box.maxX) - Math.floor(box.minX) + 1) * (Math.floor(box.maxY) - Math.floor(box.minY) + 1) * (Math.floor(box.maxZ) - Math.floor(box.minZ) + 1);
    }

    private static boolean contains(AABB outer, AABB inner) {
        return outer.minX <= inner.minX && outer.minY <= inner.minY && outer.minZ <= inner.minZ && outer.maxX >= inner.maxX && outer.maxY >= inner.maxY && outer.maxZ >= inner.maxZ;
    }

    private static double extent(Vec3 axis) {
        return (0.5 + LaserBlockClip.NEIGHBOR_REACH) * (Math.abs(axis.x) + Math.abs(axis.y) + Math.abs(axis.z)) / axis.lengthSqr();
    }

    private static boolean empty(Level level, AABB bounds) {
        int firstSection = Math.max(0, level.getSectionIndex(Mth.floor(bounds.minY)));
        int lastSection = Math.min(level.getSectionsCount() - 1, level.getSectionIndex(Mth.floor(bounds.maxY)));
        for (int x = Mth.floor(bounds.minX) >> 4; x <= Mth.floor(bounds.maxX) >> 4; x++) {
            for (int z = Mth.floor(bounds.minZ) >> 4; z <= Mth.floor(bounds.maxZ) >> 4; z++) {
                if (!level.isLoaded(new BlockPos(x << 4, 0, z << 4))) {
                    return false;
                }

                LevelChunk chunk = level.getChunk(x, z);
                for (int section = firstSection; section <= lastSection; section++) {
                    if (!chunk.getSection(section).hasOnlyAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean matches(Level level, BlockPos pos, LaserStaticDependencies dependencies, float partialTick) {
        RenderSource current = SubLevelBridge.captureRenderSource(level, Vec3.atCenterOf(pos), partialTick);
        if (current == null || !host.equals(current.id()) || !scale.equals(current.geometry().scale()) || !localBounds.equals(SubLevelBridge.sourceBounds(level, Vec3.atCenterOf(pos)))) {
            return false;
        }

        CoordinateTransform transform = SubLevelBridge.createRenderTransform(level, Vec3.atCenterOf(pos), partialTick);
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        Vec3 worldOrigin = transform.transformPosition(origin);
        Vec3 xAxis = transform.transformNormal(new Vec3(1, 0, 0));
        Vec3 yAxis = transform.transformNormal(new Vec3(0, 1, 0));
        Vec3 zAxis = transform.transformNormal(new Vec3(0, 0, 1));
        double extentX = extent(xAxis);
        double extentY = extent(yAxis);
        double extentZ = extent(zAxis);
        List<AABB> worldBounds = new ArrayList<>();
        for (AABB local : dependencies.bounds()) {
            AABB world = null;
            for (Vec3 corner : corners(local)) {
                Vec3 point = transform.transformPosition(corner);
                AABB next = new AABB(point, point);
                world = world == null ? next : world.minmax(next);
            }
            worldBounds.add(Objects.requireNonNull(world, "Rigid laser display validation requires transformed world bounds for each dependency box.").inflate(LaserBlockClip.NEIGHBOR_REACH));
        }
        if (!LaserContraptionBounds.isClear(level, worldBounds, partialTick)) {
            return false;
        }

        Map<UUID, RenderGeometry> candidates = SubLevelBridge.captureRenderGeometry(level, worldBounds, worldBounds, partialTick);
        if (candidates == null || candidates.keySet().stream().anyMatch(id -> !id.equals(host))) {
            return false;
        }

        LongSet observed = new LongOpenHashSet();
        int inspected = 0;
        LaserCollisionContext collision = new LaserCollisionContext(partialTick);
        for (int index = 0; index < worldBounds.size(); index++) {
            AABB bound = worldBounds.get(index);
            if (empty(level, bound)) {
                continue;
            }

            if (volume(bound) > MAX_CANDIDATE_BLOCKS - inspected) {
                return false;
            }

            AABB local = dependencies.bounds().get(index);
            for (BlockPos candidate : BlockPos.betweenClosed(BlockPos.containing(bound.minX, bound.minY, bound.minZ), BlockPos.containing(bound.maxX, bound.maxY, bound.maxZ))) {
                inspected++;
                Vec3 delta = Vec3.atCenterOf(candidate).subtract(worldOrigin);
                double x = origin.x + delta.dot(xAxis) / xAxis.lengthSqr();
                double y = origin.y + delta.dot(yAxis) / yAxis.lengthSqr();
                double z = origin.z + delta.dot(zAxis) / zAxis.lengthSqr();
                if (x + extentX < local.minX || x - extentX > local.maxX || y + extentY < local.minY || y - extentY > local.maxY || z + extentZ < local.minZ || z - extentZ > local.maxZ) {
                    continue;
                }

                if (!observed.add(candidate.asLong())) {
                    continue;
                }

                if (observed.size() > MAX_OBSERVED_BLOCKS) {
                    return false;
                }

                if (!level.isLoaded(candidate)) {
                    return false;
                }

                if (!level.getBlockState(candidate).getCollisionShape(level, candidate, collision).isEmpty()) {
                    return false;
                }
            }
        }
        return dependencies.matchesBlocks(level, partialTick);
    }

    public record Candidate(LaserRigidDisplay rigid, LaserStaticDependencies dependencies, LaserRenderSnapshot reference) {}
}
