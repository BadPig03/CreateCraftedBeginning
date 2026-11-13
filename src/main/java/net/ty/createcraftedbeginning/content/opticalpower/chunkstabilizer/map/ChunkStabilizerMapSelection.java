package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public record ChunkStabilizerMapSelection(@Nullable ChunkStabilizerMapLayer current, List<ChunkStabilizerMapLayer> layers, List<Long> retained) {
    static final ChunkStabilizerMapSelection EMPTY = new ChunkStabilizerMapSelection(null, List.of(), List.of());
    private static final int MAX_CHUNKS = 81;

    public ChunkStabilizerMapSelection {
        int layerCount = layers.size();
        int maxLayers = ChunkStabilizerMapLayer.MAX_CACHED_LAYERS;
        if (layerCount > maxLayers) {
            throw new IllegalArgumentException("Amethyst map layer count must be at most " + maxLayers + "; got " + layerCount + '.');
        }

        int chunkCount = retained.size();
        if (chunkCount > MAX_CHUNKS) {
            throw new IllegalArgumentException("Retained amethyst map chunk count must be at most " + MAX_CHUNKS + "; got " + chunkCount + '.');
        }

        if (current != null && !layers.contains(current)) {
            throw new IllegalArgumentException("Current amethyst map layer must be in the supplied layers; got " + current + '.');
        }

        layers = List.copyOf(layers);
        retained = List.copyOf(retained);
    }

    public static ChunkStabilizerMapSelection read(FriendlyByteBuf buffer) {
        ChunkStabilizerMapLayer current = buffer.readNullable(ChunkStabilizerMapLayer::read);
        List<ChunkStabilizerMapLayer> layers = buffer.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, ChunkStabilizerMapLayer.MAX_CACHED_LAYERS), ChunkStabilizerMapLayer::read);
        List<Long> retained = buffer.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, MAX_CHUNKS), FriendlyByteBuf::readLong);
        return new ChunkStabilizerMapSelection(current, layers, retained);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeNullable(current, (output, layer) -> Objects.requireNonNull(layer).write(output));
        buffer.writeCollection(layers, (output, layer) -> layer.write(output));
        buffer.writeCollection(retained, FriendlyByteBuf::writeLong);
    }
}
