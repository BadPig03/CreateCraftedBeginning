package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.LocalRegion;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorControlTargets {
    private static final int MAX_SCAN_RANGE = 16;
    private static final Comparator<AmethystMirrorBlockEntity> SCAN_ORDER = Comparator.comparingInt((AmethystMirrorBlockEntity mirror) -> mirror.getBlockPos().getZ()).thenComparingInt(mirror -> mirror.getBlockPos().getY()).thenComparingInt(mirror -> mirror.getBlockPos().getX());

    private final List<AmethystMirrorBlockEntity> scanned = new ArrayList<>();
    private final List<AmethystMirrorBlockEntity> mirrors = new ArrayList<>();
    private final List<AmethystMirrorBlockEntity> view = Collections.unmodifiableList(mirrors);

    void clear() {
        scanned.clear();
        mirrors.clear();
    }

    List<AmethystMirrorBlockEntity> getMirrors() {
        return view;
    }

    void collect(Level level, Player player) {
        int count = 0;
        boolean changed = false;
        int range = Math.min(MAX_SCAN_RANGE, (int) Math.ceil(player.blockInteractionRange() + 1));
        BlockPos origin = player.blockPosition();
        int originX = origin.getX();
        int originY = origin.getY();
        int originZ = origin.getZ();
        AABB search = new AABB(originX - range, originY - range, originZ - range, originX + range + 1, originY + range + 1, originZ + range + 1);
        ChunkSource chunks = level.getChunkSource();
        Set<AmethystMirrorBlockEntity> visited = new HashSet<>();
        for (LocalRegion region : SubLevelBridge.localRegions(level, search)) {
            AABB bounds = region.bounds();
            int minX = Mth.floor(bounds.minX);
            int minY = Mth.floor(bounds.minY);
            int minZ = Mth.floor(bounds.minZ);
            int maxX = Mth.ceil(bounds.maxX) - 1;
            int maxY = Mth.ceil(bounds.maxY) - 1;
            int maxZ = Mth.ceil(bounds.maxZ) - 1;
            for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
                for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                    LevelChunk chunk = chunks.getChunkNow(chunkX, chunkZ);
                    if (chunk == null) {
                        continue;
                    }

                    for (BlockEntity entity : chunk.getBlockEntities().values()) {
                        if (!(entity instanceof AmethystMirrorBlockEntity mirror)) {
                            continue;
                        }

                        BlockPos pos = mirror.getBlockPos();
                        int x = pos.getX();
                        int y = pos.getY();
                        int z = pos.getZ();
                        if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ || mirror.isRemoved() || mirror.getLevel() != level || !AmethystMirrorControlPose.canInteract(player, pos) || !visited.add(mirror)) {
                            continue;
                        }

                        if (count < scanned.size()) {
                            changed |= scanned.set(count, mirror) != mirror;
                        }
                        else {
                            scanned.add(mirror);
                            changed = true;
                        }
                        count++;
                    }
                }
            }
        }
        if (count < scanned.size()) {
            scanned.subList(count, scanned.size()).clear();
            changed = true;
        }
        if (!changed) {
            return;
        }

        mirrors.clear();
        mirrors.addAll(scanned);
        mirrors.sort(SCAN_ORDER);
    }
}
