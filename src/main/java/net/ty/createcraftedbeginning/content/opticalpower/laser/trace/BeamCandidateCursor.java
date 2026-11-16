package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BeamCandidateCursor {
    private static final int MIN_CELLS = 1024;
    private static final int MAX_CELLS = 65536;
    private final BeamVolumeCells cells;
    private final Vec3 origin;
    private final Vec3 direction;
    private final PriorityQueue<Candidate> pending;

    private BeamCandidateCursor(Level level, LaserTraceGeometry geometry, BeamVolumeCells cells, BeamState beam, List<Node> nodes) {
        this.cells = cells;
        origin = beam.frame().origin();
        direction = beam.frame().direction();
        pending = new PriorityQueue<>(Comparator.comparingDouble(Candidate::distance));
        for (Node node : nodes) {
            geometry.includeIndexed(level, node.minimum(), node.maximum(), cells);
            double distance = node.index().hasUnbounded() ? 0 : lower(node.bounds());
            pending.add(new Candidate(distance, null, node));
        }
    }

    static @Nullable BeamCandidateCursor create(Level level, LaserTraceGeometry geometry, BeamVolumeCells cells, BeamState beam, double range, boolean eligible) {
        double width = Math.max(Math.abs(beam.profile().referenceWidth()), Math.abs(beam.profile().referenceWidth() + beam.profile().widthSlope() * range));
        double scale = Math.max(beam.footprint().horizontalScale(), beam.footprint().verticalScale());
        if (!eligible || width * scale < 2 || !(level instanceof ServerLevel || level instanceof ClientLevel)) {
            return null;
        }

        AABB bounds = cells.bounds();
        if (bounds == null) {
            return null;
        }

        int minX = (int) Math.floor(bounds.minX);
        int minY = Math.max(level.getMinBuildHeight(), (int) Math.floor(bounds.minY));
        int minZ = (int) Math.floor(bounds.minZ);
        int maxX = (int) Math.ceil(bounds.maxX) - 1;
        int maxY = Math.min(level.getMaxBuildHeight() - 1, (int) Math.ceil(bounds.maxY) - 1);
        int maxZ = (int) Math.ceil(bounds.maxZ) - 1;
        double size = (maxX - (double) minX + 1) * (maxY - (double) minY + 1) * (maxZ - (double) minZ + 1);
        if (size < MIN_CELLS || size > MAX_CELLS) {
            return null;
        }

        List<Node> nodes = new ArrayList<>();
        for (int x = minX >> 4; x <= maxX >> 4; x++) {
            for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
                BlockPos sample = new BlockPos(x << 4, minY, z << 4);
                if (!level.isLoaded(sample)) {
                    return null;
                }

                ChunkAccess chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) {
                    return null;
                }

                for (int y = minY >> 4; y <= maxY >> 4; y++) {
                    BlockPos minimum = new BlockPos(Math.max(minX, x << 4), Math.max(minY, y << 4), Math.max(minZ, z << 4));
                    BlockPos maximum = new BlockPos(Math.min(maxX, (x << 4) + 15), Math.min(maxY, (y << 4) + 15), Math.min(maxZ, (z << 4) + 15));
                    LevelChunkSection section = chunk.getSection(level.getSectionIndex(y << 4));
                    LaserSectionCandidates index = ((LaserSectionAccess) section).ccb$getLaserCandidates();
                    AABB region = new AABB(minimum.getX(), minimum.getY(), minimum.getZ(), maximum.getX() + 1, maximum.getY() + 1, maximum.getZ() + 1);
                    nodes.add(new Node(new BlockPos(x << 4, y << 4, z << 4), minimum, maximum, region, index));
                }
            }
        }
        return new BeamCandidateCursor(level, geometry, cells, beam, nodes);
    }

    @Nullable BlockPos next(double nearest) {
        while (!pending.isEmpty()) {
            Candidate candidate = pending.remove();
            if (candidate.distance() > nearest + BeamSweepSection.CONTACT_TOLERANCE) {
                pending.clear();
                return null;
            }

            BlockPos pos = candidate.pos();
            if (pos != null) {
                return pos;
            }

            Node node = candidate.node();
            if (node == null) {
                throw new IllegalStateException("A laser candidate must contain a block or section.");
            }

            open(node, pending);
        }
        return null;
    }

    private void open(Node node, PriorityQueue<Candidate> pending) {
        LaserSectionCandidates index = node.index();
        BlockPos base = node.base();
        for (int value = index.next(0); value >= 0; value = index.next(value + 1)) {
            BlockPos pos = base.offset(value & 15, value >> 8, value >> 4 & 15);
            if (!cells.contains(pos)) {
                continue;
            }

            double distance = index.isUnbounded(value) ? 0 : lower(new AABB(pos));
            pending.add(new Candidate(distance, pos, null));
        }
    }

    private double lower(AABB box) {
        double x = direction.x >= 0 ? box.minX : box.maxX;
        double y = direction.y >= 0 ? box.minY : box.maxY;
        double z = direction.z >= 0 ? box.minZ : box.maxZ;
        return Math.max(0, (x - origin.x) * direction.x + (y - origin.y) * direction.y + (z - origin.z) * direction.z - BeamSweepSection.CONTACT_TOLERANCE);
    }

    private record Node(BlockPos base, BlockPos minimum, BlockPos maximum, AABB bounds, LaserSectionCandidates index) {}

    private record Candidate(double distance, @Nullable BlockPos pos, @Nullable Node node) {}
}
