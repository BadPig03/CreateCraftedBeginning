package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorSectionIndex {
    private static final int MAX_QUERY_SECTIONS = 64;
    private final IntSet mirrors = new IntOpenHashSet();

    public AmethystMirrorSectionIndex(LevelChunkSection section) {
        if (!section.maybeHas(state -> state.getBlock() instanceof AmethystMirrorBlock)) {
            return;
        }

        for (int cell = 0; cell < 4096; cell++) {
            int x = cell & 15;
            int y = cell >> 8;
            int z = cell >> 4 & 15;
            changed(x, y, z, section.getBlockState(x, y, z));
        }
    }

    public static boolean requiresExpansion(Level level, BlockPos min, BlockPos max) {
        int minX = min.getX() - 1;
        int minY = Math.max(min.getY(), level.getMinBuildHeight());
        int minZ = min.getZ() - 1;
        int maxX = max.getX() + 1;
        int maxY = Math.min(max.getY() + 1, level.getMaxBuildHeight() - 1);
        int maxZ = max.getZ() + 1;
        int startX = minX >> 4;
        int startY = minY >> 4;
        int startZ = minZ >> 4;
        int endX = maxX >> 4;
        int endY = maxY >> 4;
        int endZ = maxZ >> 4;
        if ((long) (endX - startX + 1) * (endY - startY + 1) * (endZ - startZ + 1) > MAX_QUERY_SECTIONS) {
            return true;
        }

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                ChunkAccess chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) {
                    return true;
                }

                for (int y = startY; y <= endY; y++) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(y));
                    AmethystMirrorSectionIndex index = ((AmethystMirrorSectionAccess) section).ccb$getMirrorIndex();
                    if (!index.contains(minX - (x << 4), minY - (y << 4), minZ - (z << 4), maxX - (x << 4), maxY - (y << 4), maxZ - (z << 4))) {
                        continue;
                    }

                    return true;
                }
            }
        }
        return false;
    }

    public void changed(int x, int y, int z, BlockState state) {
        int cell = y << 8 | z << 4 | x;
        if (state.getBlock() instanceof AmethystMirrorBlock) {
            mirrors.add(cell);
            return;
        }

        mirrors.remove(cell);
    }

    boolean contains(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int cell : mirrors) {
            int x = cell & 15;
            int y = cell >> 8;
            int z = cell >> 4 & 15;
            if (x < minX || y < minY || z < minZ || x > maxX || y > maxY || z > maxZ) {
                continue;
            }

            return true;
        }
        return false;
    }
}
