package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserWorldSections {
    private static final int MAX_CHUNKS = 1024;
    private static final int MAX_SECTIONS = 4096;
    private final WeakReference<Level> world;
    private final Long2ObjectOpenHashMap<Chunk> chunks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Section> sections = new Long2ObjectOpenHashMap<>();
    private boolean complete = true;

    LaserWorldSections(Level level) {
        world = new WeakReference<>(level);
    }

    static boolean stable(BlockState state) {
        Block block = state.getBlock();
        return !block.hasDynamicShape() && (block.getClass() == Block.class || block.getClass() == AirBlock.class);
    }

    void include(Level level, BlockPos pos) {
        Section section = section(level, pos);
        if (section == null) {
            return;
        }

        section.include(pos);
        section.unfiltered = true;
    }

    void include(Level level, BlockPos minimum, BlockPos maximum, BeamVolumeCells cells) {
        Section section = section(level, minimum);
        if (section == null) {
            return;
        }

        section.include(minimum);
        section.include(maximum);
        section.observe(cells);
    }

    boolean isComplete() {
        return complete;
    }

    boolean matches(Level level) {
        if (!complete || world.get() != level) {
            return false;
        }

        for (Chunk chunk : chunks.values()) {
            BlockPos pos = chunk.pos();
            if (level.isLoaded(pos) != chunk.loaded()) {
                return false;
            }

            ChunkAccess current = level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
            if (current != chunk.identity().get() || chunk.loaded() && current == null) {
                return false;
            }
        }
        for (Section section : sections.values()) {
            if (!section.matches()) {
                return false;
            }
        }
        return true;
    }

    private @Nullable Section section(Level level, BlockPos pos) {
        if (!complete) {
            return null;
        }

        if (world.get() != level) {
            complete = false;
            return null;
        }

        if (level.isOutsideBuildHeight(pos)) {
            return null;
        }

        int x = pos.getX() >> 4;
        int z = pos.getZ() >> 4;
        long chunkKey = ChunkPos.asLong(x, z);
        Chunk chunk = chunks.get(chunkKey);
        if (chunk == null) {
            if (chunks.size() >= MAX_CHUNKS) {
                complete = false;
                return null;
            }

            ChunkAccess access = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
            chunk = new Chunk(new BlockPos(x << 4, pos.getY(), z << 4), level.isLoaded(pos), new WeakReference<>(access));
            chunks.put(chunkKey, chunk);
        }
        ChunkAccess access = chunk.identity().get();
        if (access == null) {
            return null;
        }

        long key = SectionPos.asLong(x, pos.getY() >> 4, z);
        Section section = sections.get(key);
        if (section == null) {
            if (sections.size() >= MAX_SECTIONS) {
                complete = false;
                return null;
            }

            int index = level.getSectionIndex(pos.getY());
            LevelChunkSection identity = access.getSection(index);
            section = new Section(chunk, index, pos.getY() >> 4, identity);
            sections.put(key, section);
        }
        return section;
    }

    private record Chunk(BlockPos pos, boolean loaded, WeakReference<@Nullable ChunkAccess> identity) {}

    private static final class Section {
        private final Chunk chunk;
        private final int index;
        private final WeakReference<LevelChunkSection> identity;
        private final LaserSectionChanges changes;
        private final int sectionY;
        private @Nullable List<BeamVolumeCells> observations;
        private @Nullable IntPredicate observed;
        private boolean unfiltered;
        private long revision;
        private int minX = 15;
        private int minY = 15;
        private int minZ = 15;
        private int maxX;
        private int maxY;
        private int maxZ;

        private Section(Chunk chunk, int index, int sectionY, LevelChunkSection identity) {
            this.chunk = chunk;
            this.index = index;
            this.identity = new WeakReference<>(identity);
            changes = ((LaserSectionAccess) identity).ccb$getLaserChanges();
            revision = changes.revision();
            this.sectionY = sectionY;
        }

        private void observe(BeamVolumeCells cells) {
            List<BeamVolumeCells> observations = this.observations;
            if (observations == null) {
                observations = new ArrayList<>();
                this.observations = observations;
                List<BeamVolumeCells> retained = observations;
                observed = position -> {
                    BlockPos pos = new BlockPos(chunk.pos().getX() + (position & 15), (sectionY << 4) + (position >> 8 & 15), chunk.pos().getZ() + (position >> 4 & 15));
                    return retained.stream().anyMatch(region -> region.contains(pos));
                };
            }
            observations.add(cells);
        }

        private void include(BlockPos pos) {
            int x = pos.getX() & 15;
            int y = pos.getY() & 15;
            int z = pos.getZ() & 15;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        private boolean matches() {
            ChunkAccess access = chunk.identity().get();
            LevelChunkSection section = identity.get();
            if (access == null || section == null || access.getSection(index) != section || ((LaserSectionAccess) section).ccb$getLaserChanges() != changes || changes.intersects(revision, minX, minY, minZ, maxX, maxY, maxZ, unfiltered ? null : observed)) {
                return false;
            }

            revision = changes.revision();
            return true;
        }
    }
}
