package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderLocation;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapChunk.Version;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSnapshot.State;
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
public final class AmethystLoaderMapView {
    private final AmethystLoaderMapMotion motion = new AmethystLoaderMapMotion();
    private final UUID request;
    private final UUID identity;
    private final Map<AmethystLoaderMapLayer, CachedLayer> layers = new HashMap<>();
    private List<Long> window = List.of();
    private long generation = -1;
    private long sequence = -1;
    @Nullable
    private AmethystLoaderMapLayer layer;
    @Nullable
    private CachedLayer current;
    @Nullable
    private AmethystLoaderLocation location;
    @Nullable
    private AmethystLoaderMapSnapshot snapshot;

    public AmethystLoaderMapView(UUID request, UUID identity) {
        this.request = request;
        this.identity = identity;
    }

    public void acceptPosition(UUID incomingRequest, UUID incomingIdentity, long incomingGeneration, long incomingSequence, long tick, @Nullable AmethystLoaderLocation next, AmethystLoaderMapSelection selection) {
        if (!request.equals(incomingRequest) || !identity.equals(incomingIdentity) || incomingSequence <= sequence || incomingGeneration < generation) {
            return;
        }

        AmethystLoaderMapLayer nextLayer = selection.current();
        List<AmethystLoaderMapLayer> preparedLayers = selection.layers();
        boolean reset = next == null || location == null || next.dimension() != location.dimension() || !next.host().equals(location.host()) || next.part() != location.part();
        boolean switched = reset || incomingGeneration != generation || !Objects.equals(layer, nextLayer);
        if (reset) {
            layers.clear();
            window = List.of();
        }
        motion.accept(next, tick, System.nanoTime(), reset);
        generation = incomingGeneration;
        sequence = incomingSequence;
        location = next;
        layer = nextLayer;
        layers.keySet().retainAll(preparedLayers);
        for (AmethystLoaderMapLayer prepared : preparedLayers) {
            layers.computeIfAbsent(prepared, ignored -> new CachedLayer());
        }
        current = layers.get(layer);
        if (next == null) {
            snapshot = new AmethystLoaderMapSnapshot(identity, State.UNAVAILABLE, null, List.of());
            return;
        }

        GlobalPos position = next.blockPosition();
        List<Long> currentWindow = AmethystLoaderMapSnapshot.bufferedWindow(position.pos());
        boolean changedWindow = !window.equals(currentWindow);
        window = currentWindow;
        for (CachedLayer cached : layers.values()) {
            cached.chunks.keySet().retainAll(window);
        }
        if (switched && current != null) {
            current.chunks.keySet().retainAll(selection.retained());
        }
        if (switched || changedWindow || snapshot == null) {
            snapshot = AmethystLoaderMapSnapshot.assemble(identity, next, current == null ? Map.of() : current.chunks);
            return;
        }

        snapshot = new AmethystLoaderMapSnapshot(identity, State.READY, position, snapshot.columns());
    }

    public void acceptTerrain(UUID incomingRequest, UUID incomingIdentity, long incomingGeneration, long incomingSequence, ResourceKey<Level> dimension, AmethystLoaderMapLayer incomingLayer, List<AmethystLoaderMapChunk> chunks) {
        if (!request.equals(incomingRequest) || !identity.equals(incomingIdentity) || incomingGeneration != generation || incomingSequence != sequence || location == null || location.dimension() != dimension) {
            return;
        }

        CachedLayer cached = layers.get(incomingLayer);
        if (cached == null || incomingSequence <= cached.sequence) {
            return;
        }

        cached.sequence = incomingSequence;
        boolean changed = false;
        for (AmethystLoaderMapChunk update : chunks) {
            long chunk = update.chunk();
            if (!window.contains(chunk)) {
                continue;
            }

            AmethystLoaderMapChunk previous = cached.chunks.get(chunk);
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

        snapshot = AmethystLoaderMapSnapshot.assemble(identity, location, current.chunks);
    }

    public @Nullable Vec3 point(long nanos) {
        return motion.point(nanos);
    }

    public @Nullable Vec3 origin(long nanos) {
        return motion.origin(nanos);
    }

    public @Nullable AmethystLoaderMapSnapshot snapshot() {
        return snapshot;
    }

    public @Nullable AmethystLoaderLocation location() {
        return location;
    }

    private static final class CachedLayer {
        private final Map<Long, AmethystLoaderMapChunk> chunks = new HashMap<>();
        private long sequence = -1;
    }
}
