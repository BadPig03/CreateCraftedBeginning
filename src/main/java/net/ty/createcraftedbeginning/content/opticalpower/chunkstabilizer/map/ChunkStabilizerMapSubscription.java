package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerLocation;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapChunk.Version;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class ChunkStabilizerMapSubscription {
    private static final int MODE_SETTLE_TICKS = 10;
    private static final int PREDICTION_TICKS = 12;
    private static final int MAX_PREFETCH_CHUNKS = 8;
    private static final int MAX_PREDICTION_DISTANCE_SQUARED = 1024;
    private static final int[] ADJACENT_OFFSETS = {ChunkStabilizerMapLayer.STEP, -ChunkStabilizerMapLayer.STEP};
    private final Map<ChunkStabilizerMapLayer, Map<Long, Version>> sent = new LinkedHashMap<>(8, 0.75F, true);
    private List<ChunkStabilizerMapLayer> prepared = List.of();
    @Nullable
    private Target target;
    @Nullable
    private ChunkStabilizerLocation location;
    @Nullable
    private ChunkStabilizerMapLayer layer;
    @Nullable
    private ChunkStabilizerMapMode pendingMode;
    private long pendingSince;
    private long generation;
    private long sequence;
    private long sampledTick;

    public Update refresh(MinecraftServer server, @Nullable Target current, @Nullable ChunkStabilizerLocation position, boolean terrain) {
        ChunkStabilizerLocation lastLocation = location;
        location = position;
        ServerLevel level = location == null ? null : server.getLevel(location.dimension());
        Target previousTarget = target;
        boolean changed = !Objects.equals(previousTarget, current);
        ChunkStabilizerMapLayer previous = changed ? null : layer;
        ChunkStabilizerMapLayer selected = level == null ? null : ChunkStabilizerMapLayers.at(level, location.position(), previous);
        ChunkStabilizerMapLayer candidate = selected;
        long tick = server.overworld().getGameTime();
        ResourceKey<Level> dimension = level == null ? null : level.dimension();
        boolean interiorDimension = dimension == Level.OVERWORLD || dimension == Level.END;
        ChunkStabilizerMapMode selectedMode = selected == null ? null : selected.mode();
        if (!changed && interiorDimension && layer != null && selectedMode != null && layer.mode() != selectedMode) {
            if (pendingMode != selectedMode) {
                pendingMode = selectedMode;
                pendingSince = tick;
            }
            if (tick - pendingSince < MODE_SETTLE_TICKS) {
                selected = layer;
            }
        }
        else {
            pendingMode = null;
        }
        if (selected == null && level != null && previousTarget != null && previousTarget.level() == level && !sent.isEmpty()) {
            Set<Long> knownChunks = new HashSet<>();
            for (Map<Long, Version> versions : sent.values()) {
                knownChunks.addAll(versions.keySet());
            }
            ChunkStabilizerMapCache.discardUnloadedChunks(level, knownChunks);
        }
        boolean switched = changed || !Objects.equals(layer, selected);
        if (switched) {
            layer = selected;
            terrain = true;
            generation++;
        }
        if (changed || layer == null) {
            sent.clear();
            pendingMode = null;
        }
        target = current;
        sequence++;
        if (level == null) {
            location = null;
            prepared = List.of();
            return new Update(generation, sequence, tick, null, ChunkStabilizerMapSelection.EMPTY, List.of(), List.of());
        }

        if (layer == null) {
            prepared = List.of();
            return new Update(generation, sequence, tick, location, ChunkStabilizerMapSelection.EMPTY, List.of(), List.of());
        }

        prepare(level, location, layer, candidate, changed ? null : lastLocation, tick);
        List<Long> window = ChunkStabilizerMapSnapshot.bufferedWindow(location.blockPosition().pos());
        for (Map<Long, Version> versions : sent.values()) {
            versions.keySet().retainAll(window);
        }
        Map<Long, Version> versions = sent.get(layer);
        List<Long> retained = new ArrayList<>();
        List<ChunkStabilizerMapChunk> chunks = new ArrayList<>();
        for (long chunk : window) {
            Version known = versions.get(chunk);
            if (!terrain && (known == null || level.getChunkSource().getChunkNow(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)) != null)) {
                continue;
            }

            ChunkStabilizerMapChunk update = ChunkStabilizerMapCache.read(level, chunk, layer, known);
            if (update == null) {
                if (switched && known != null) {
                    retained.add(chunk);
                }
                continue;
            }

            chunks.add(update);
            Version version = update.version();
            if (version == null) {
                versions.remove(chunk);
                continue;
            }

            versions.put(chunk, version);
        }
        List<Terrain> prefetched = new ArrayList<>();
        int remaining = terrain ? MAX_PREFETCH_CHUNKS : 0;
        for (ChunkStabilizerMapLayer next : prepared) {
            if (next.equals(layer) || remaining == 0) {
                continue;
            }

            Map<Long, Version> known = sent.get(next);
            List<ChunkStabilizerMapChunk> updates = new ArrayList<>();
            for (long chunk : window) {
                ChunkStabilizerMapChunk update = ChunkStabilizerMapCache.read(level, chunk, next, known.get(chunk), true);
                if (update == null) {
                    continue;
                }

                updates.add(update);
                Version version = update.version();
                if (version == null) {
                    known.remove(chunk);
                }
                else {
                    known.put(chunk, version);
                }
                if (--remaining == 0) {
                    break;
                }
            }
            if (updates.isEmpty()) {
                continue;
            }

            prefetched.add(new Terrain(next, List.copyOf(updates)));
        }
        ChunkStabilizerMapSelection selection = new ChunkStabilizerMapSelection(layer, List.copyOf(sent.keySet()), retained);
        return new Update(generation, sequence, tick, location, selection, List.copyOf(chunks), List.copyOf(prefetched));
    }

    public @Nullable ChunkStabilizerLocation location() {
        return location;
    }

    public @Nullable ChunkStabilizerMapLayer layer() {
        return layer;
    }

    public List<ChunkStabilizerMapLayer> prepared() {
        return prepared;
    }

    private void prepare(ServerLevel level, ChunkStabilizerLocation location, ChunkStabilizerMapLayer layer, @Nullable ChunkStabilizerMapLayer candidate, @Nullable ChunkStabilizerLocation lastLocation, long tick) {
        Vec3 position = location.position();
        LinkedHashSet<ChunkStabilizerMapLayer> wanted = new LinkedHashSet<>();
        wanted.add(layer);
        if (candidate != null) {
            wanted.add(candidate);
        }
        if (lastLocation != null && tick > sampledTick && tick - sampledTick <= MODE_SETTLE_TICKS) {
            Vec3 movement = position.subtract(lastLocation.position());
            double distance = movement.lengthSqr();
            if (distance > 0 && distance <= MAX_PREDICTION_DISTANCE_SQUARED) {
                Vec3 prediction = position.add(movement.scale((double) PREDICTION_TICKS / (tick - sampledTick)));
                ChunkStabilizerMapLayer ahead = ChunkStabilizerMapLayers.at(level, prediction, layer);
                if (ahead != null) {
                    wanted.add(ahead);
                }
            }
        }
        sampledTick = tick;
        double height = layer.mode() == ChunkStabilizerMapMode.SURFACE ? position.y : layer.height();
        for (int offset : ADJACENT_OFFSETS) {
            ChunkStabilizerMapLayer adjacent = ChunkStabilizerMapLayers.at(level, new Vec3(position.x, height + offset, position.z), null);
            if (adjacent == null) {
                continue;
            }

            wanted.add(adjacent);
        }
        prepared = List.copyOf(wanted);
        for (ChunkStabilizerMapLayer next : prepared) {
            sent.computeIfAbsent(next, ignored -> new HashMap<>());
        }
        while (sent.size() > ChunkStabilizerMapLayer.MAX_CACHED_LAYERS) {
            sent.remove(sent.keySet().iterator().next());
        }
    }

    public record Update(long generation, long sequence, long tick, @Nullable ChunkStabilizerLocation location, ChunkStabilizerMapSelection selection, List<ChunkStabilizerMapChunk> chunks, List<Terrain> prefetched) {}

    public record Terrain(ChunkStabilizerMapLayer layer, List<ChunkStabilizerMapChunk> chunks) {}

    public record Target(ServerLevel level, GlobalPos registered, boolean train, boolean moving, boolean physical, boolean assembled, UUID host, int part) {}
}
