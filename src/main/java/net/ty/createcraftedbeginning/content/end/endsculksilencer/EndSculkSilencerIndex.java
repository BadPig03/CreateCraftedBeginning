package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class EndSculkSilencerIndex {
    private final Map<ResourceLocation, Map<BlockPos, EndSculkSilencerInstance>> instancesByDimension = new HashMap<>();
    private final Map<ResourceLocation, Long2IntOpenHashMap> coveredChunksByDimension = new HashMap<>();

    private static void decrementCoverage(Long2IntOpenHashMap coveredChunks, long chunkKey) {
        int count = coveredChunks.get(chunkKey);
        if (count > 1) {
            coveredChunks.put(chunkKey, count - 1);
        }
        else if (count == 1) {
            coveredChunks.remove(chunkKey);
        }
    }

    public synchronized boolean hasCoverage(ResourceLocation dimension) {
        Long2IntOpenHashMap coveredChunks = coveredChunksByDimension.get(dimension);
        return coveredChunks != null && !coveredChunks.isEmpty();
    }

    public synchronized boolean isCovered(BlockPos blockPos, ResourceLocation dimension) {
        Long2IntOpenHashMap coveredChunks = coveredChunksByDimension.get(dimension);
        return coveredChunks != null && coveredChunks.containsKey(EndSculkSilencerInstance.chunkKey(blockPos));
    }

    public synchronized @Nullable EndSculkSilencerInstance get(BlockPos registrationPos, ResourceLocation dimension) {
        Map<BlockPos, EndSculkSilencerInstance> instances = instancesByDimension.get(dimension);
        return instances == null ? null : instances.get(registrationPos);
    }

    public synchronized boolean update(BlockPos registrationPos, BlockPos effectCenter, ResourceLocation dimension, short range) {
        if (range <= 0) {
            return remove(registrationPos, dimension);
        }

        BlockPos immutableRegistrationPos = registrationPos.immutable();
        Map<BlockPos, EndSculkSilencerInstance> instances = instancesByDimension.computeIfAbsent(dimension, $ -> new HashMap<>());
        EndSculkSilencerInstance previous = instances.get(immutableRegistrationPos);
        if (previous != null && previous.hasSameCoverage(effectCenter, range)) {
            return false;
        }

        if (previous != null) {
            unindex(previous);
        }

        EndSculkSilencerInstance updated = new EndSculkSilencerInstance(immutableRegistrationPos, effectCenter, dimension, range);
        instances.put(immutableRegistrationPos, updated);
        index(updated);
        return true;
    }

    public synchronized boolean remove(BlockPos registrationPos, ResourceLocation dimension) {
        Map<BlockPos, EndSculkSilencerInstance> instances = instancesByDimension.get(dimension);
        if (instances == null) {
            return false;
        }

        EndSculkSilencerInstance removed = instances.remove(registrationPos);
        if (removed == null) {
            return false;
        }

        unindex(removed);
        if (!instances.isEmpty()) {
            return true;
        }

        instancesByDimension.remove(dimension);
        return true;
    }

    public synchronized List<EndSculkSilencerInstance> removeDimension(ResourceLocation dimension) {
        Map<BlockPos, EndSculkSilencerInstance> removed = instancesByDimension.remove(dimension);
        coveredChunksByDimension.remove(dimension);
        return removed == null ? List.of() : List.copyOf(removed.values());
    }

    public synchronized List<EndSculkSilencerInstance> getInstances(ResourceLocation dimension) {
        Map<BlockPos, EndSculkSilencerInstance> instances = instancesByDimension.get(dimension);
        return instances == null ? List.of() : List.copyOf(instances.values());
    }

    public synchronized void clear() {
        instancesByDimension.clear();
        coveredChunksByDimension.clear();
    }

    private void index(EndSculkSilencerInstance instance) {
        Long2IntOpenHashMap coveredChunks = coveredChunksByDimension.computeIfAbsent(instance.dimension(), ignored -> new Long2IntOpenHashMap());
        instance.forEachCoveredChunk(chunkKey -> coveredChunks.addTo(chunkKey, 1));
    }

    private void unindex(EndSculkSilencerInstance instance) {
        Long2IntOpenHashMap coveredChunks = coveredChunksByDimension.get(instance.dimension());
        if (coveredChunks == null) {
            return;
        }

        instance.forEachCoveredChunk(chunkKey -> decrementCoverage(coveredChunks, chunkKey));
        if (!coveredChunks.isEmpty()) {
            return;
        }

        coveredChunksByDimension.remove(instance.dimension());
    }
}
