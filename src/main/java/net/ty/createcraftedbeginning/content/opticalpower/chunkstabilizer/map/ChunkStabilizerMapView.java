package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerLocation;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapChunk.Version;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapSnapshot.State;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class ChunkStabilizerMapView {
    private final ChunkStabilizerMapMotion motion = new ChunkStabilizerMapMotion();
    private final UUID request;
    private final UUID identity;
    private final Map<ChunkStabilizerMapLayer, CachedLayer> layers = new HashMap<>();
    private List<Long> window = List.of();
    private long generation = -1;
    private long sequence = -1;
    @Nullable
    private ChunkStabilizerMapLayer layer;
    @Nullable
    private CachedLayer current;
    @Nullable
    private ChunkStabilizerLocation location;
    @Nullable
    private ChunkStabilizerMapSnapshot snapshot;
    private boolean assemblyUnavailable;

    public ChunkStabilizerMapView(UUID request, UUID identity) {
        this.request = request;
        this.identity = identity;
    }

    public void acceptPosition(UUID incomingRequest, UUID incomingIdentity, long incomingGeneration, long incomingSequence, long tick, @Nullable ChunkStabilizerLocation next, ChunkStabilizerMapSelection selection) {
        if (!request.equals(incomingRequest) || !identity.equals(incomingIdentity) || incomingSequence <= sequence || incomingGeneration < generation) {
            return;
        }

        ChunkStabilizerMapLayer nextLayer = selection.current();
        List<ChunkStabilizerMapLayer> preparedLayers = selection.layers();
        boolean reset = next == null || location == null || next.dimension() != location.dimension() || !next.host().equals(location.host()) || next.part() != location.part();
        boolean switched = reset || incomingGeneration != generation || !Objects.equals(layer, nextLayer);
        if (reset) {
            layers.clear();
            window = List.of();
        }
        motion.accept(next, tick, System.nanoTime(), reset || assemblyUnavailable);
        generation = incomingGeneration;
        sequence = incomingSequence;
        location = next;
        layer = nextLayer;
        layers.keySet().retainAll(preparedLayers);
        for (ChunkStabilizerMapLayer prepared : preparedLayers) {
            layers.computeIfAbsent(prepared, ignored -> new CachedLayer());
        }
        current = layers.get(layer);
        if (next == null) {
            assemblyUnavailable = false;
            snapshot = new ChunkStabilizerMapSnapshot(identity, State.UNAVAILABLE, null, List.of());
            return;
        }

        GlobalPos position = next.blockPosition();
        List<Long> currentWindow = ChunkStabilizerMapSnapshot.bufferedWindow(position.pos());
        boolean changedWindow = !window.equals(currentWindow);
        window = currentWindow;
        for (CachedLayer cached : layers.values()) {
            cached.chunks.keySet().retainAll(window);
        }
        if (switched && current != null) {
            current.chunks.keySet().retainAll(selection.retained());
        }
        if (switched || changedWindow || snapshot == null || snapshot.state() != State.READY) {
            snapshot = assemble(next, current == null ? Map.of() : current.chunks);
            return;
        }

        snapshot = new ChunkStabilizerMapSnapshot(identity, State.READY, position, snapshot.columns());
    }

    public void acceptTerrain(UUID incomingRequest, UUID incomingIdentity, long incomingGeneration, long incomingSequence, ResourceKey<Level> dimension, ChunkStabilizerMapLayer incomingLayer, List<ChunkStabilizerMapChunk> chunks) {
        if (!request.equals(incomingRequest) || !identity.equals(incomingIdentity) || incomingGeneration != generation || incomingSequence != sequence || location == null || location.dimension() != dimension) {
            return;
        }

        CachedLayer cached = layers.get(incomingLayer);
        if (cached == null || incomingSequence <= cached.sequence) {
            return;
        }

        cached.sequence = incomingSequence;
        boolean changed = false;
        for (ChunkStabilizerMapChunk update : chunks) {
            long chunk = update.chunk();
            if (!window.contains(chunk)) {
                continue;
            }

            ChunkStabilizerMapChunk previous = cached.chunks.get(chunk);
            Version version = update.version();
            if (version == null) {
                for (CachedLayer other : layers.values()) {
                    boolean removed = other.chunks.remove(chunk) != null;
                    changed |= removed && other == current;
                }
                continue;
            }

            Version old = previous == null ? null : previous.version();
            if (version.equals(old) || old != null && old.incarnation().equals(version.incarnation()) && old.revision() > version.revision()) {
                continue;
            }

            cached.chunks.put(chunk, update);
            changed |= cached == current;
        }
        if (!changed || current == null) {
            return;
        }

        snapshot = assemble(location, current.chunks);
    }

    public @Nullable Vec3 point(long nanos) {
        return assemblyUnavailable ? null : motion.point(nanos);
    }

    public @Nullable Vec3 origin(long nanos) {
        return assemblyUnavailable ? null : motion.origin(nanos);
    }

    public @Nullable ChunkStabilizerMapSnapshot snapshot() {
        return snapshot;
    }

    public @Nullable ChunkStabilizerLocation location() {
        return assemblyUnavailable ? null : location;
    }

    private ChunkStabilizerMapSnapshot assemble(ChunkStabilizerLocation location, Map<Long, ChunkStabilizerMapChunk> chunks) {
        try {
            ChunkStabilizerMapSnapshot assembled = ChunkStabilizerMapSnapshot.assemble(identity, location, chunks);
            assemblyUnavailable = false;
            return assembled;
        }
        catch (IllegalArgumentException failure) {
            if (!assemblyUnavailable) {
                assemblyUnavailable = true;
                CCBAPI.LOGGER.warn("Failed to assemble the chunk stabilizer map for loader '{}' at {} in dimension '{}'; hiding the terrain.", identity, location.position(), location.dimension().location(), failure);
            }
            return new ChunkStabilizerMapSnapshot(identity, State.UNAVAILABLE, null, List.of());
        }
    }

    private static final class CachedLayer {
        private final Map<Long, ChunkStabilizerMapChunk> chunks = new HashMap<>();
        private long sequence = -1;
    }
}
