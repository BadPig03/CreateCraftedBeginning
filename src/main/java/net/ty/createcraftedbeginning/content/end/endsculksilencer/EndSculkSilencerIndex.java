package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class EndSculkSilencerIndex {
    private final Map<String, Map<BlockPos, EndSculkSilencerInstance>> instancesByDimension = new HashMap<>();
    private final Map<String, Map<Long, Integer>> coveredChunksByDimension = new HashMap<>();

    public synchronized boolean isCovered(BlockPos blockPos, String dimension) {
        Map<Long, Integer> coveredChunks = coveredChunksByDimension.get(dimension);
        return coveredChunks != null && coveredChunks.containsKey(EndSculkSilencerInstance.chunkKey(blockPos));
    }

    public synchronized boolean update(BlockPos blockPos, String dimension, short range) {
        if (range <= 0) {
            return remove(blockPos, dimension);
        }

        BlockPos immutablePos = blockPos.immutable();
        Map<BlockPos, EndSculkSilencerInstance> instances = instancesByDimension.computeIfAbsent(dimension, ignored -> new HashMap<>());
        EndSculkSilencerInstance previous = instances.get(immutablePos);
        if (previous != null && previous.range() == range) {
            return false;
        }

        if (previous != null) {
            unindex(previous);
        }
        EndSculkSilencerInstance updated = new EndSculkSilencerInstance(immutablePos, dimension, range);
        instances.put(immutablePos, updated);
        index(updated);
        return true;
    }

    public synchronized boolean remove(BlockPos blockPos, String dimension) {
        Map<BlockPos, EndSculkSilencerInstance> instances = instancesByDimension.get(dimension);
        if (instances == null) {
            return false;
        }

        EndSculkSilencerInstance removed = instances.remove(blockPos);
        if (removed == null) {
            return false;
        }

        unindex(removed);
        if (instances.isEmpty()) {
            instancesByDimension.remove(dimension);
        }
        return true;
    }

    public synchronized List<EndSculkSilencerInstance> removeDimension(String dimension) {
        Map<BlockPos, EndSculkSilencerInstance> removed = instancesByDimension.remove(dimension);
        coveredChunksByDimension.remove(dimension);
        return removed == null ? List.of() : List.copyOf(removed.values());
    }

    public synchronized List<EndSculkSilencerInstance> getInstances(String dimension) {
        Map<BlockPos, EndSculkSilencerInstance> instances = instancesByDimension.get(dimension);
        return instances == null ? List.of() : List.copyOf(instances.values());
    }

    public synchronized void clear() {
        instancesByDimension.clear();
        coveredChunksByDimension.clear();
    }

    private void index(EndSculkSilencerInstance instance) {
        Map<Long, Integer> coveredChunks = coveredChunksByDimension.computeIfAbsent(instance.dimension(), ignored -> new HashMap<>());
        instance.forEachCoveredChunk(chunkKey -> coveredChunks.merge(chunkKey, 1, Integer::sum));
    }

    private void unindex(EndSculkSilencerInstance instance) {
        Map<Long, Integer> coveredChunks = coveredChunksByDimension.get(instance.dimension());
        if (coveredChunks == null) {
            return;
        }

        instance.forEachCoveredChunk(chunkKey -> coveredChunks.computeIfPresent(chunkKey, (ignored, count) -> count > 1 ? count - 1 : null));
        if (!coveredChunks.isEmpty()) {
            return;
        }

        coveredChunksByDimension.remove(instance.dimension());
    }
}
