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
public final class EndSculkSilencerIndex {
    private final Map<ResourceLocation, Map<BlockPos, EndSculkSilencerInstance>> instancesByDimension = new HashMap<>();
    private final Map<ResourceLocation, Long2IntOpenHashMap> coveredChunksByDimension = new HashMap<>();

    private static void decrementCoverage(Long2IntOpenHashMap coverageCounts, long chunkKey) {
        int coverageCount = coverageCounts.get(chunkKey);
        if (coverageCount <= 0) {
            return;
        }

        if (coverageCount == 1) {
            coverageCounts.remove(chunkKey);
            return;
        }

        coverageCounts.put(chunkKey, coverageCount - 1);
    }

    public synchronized boolean hasCoverage(ResourceLocation dimension) {
        Long2IntOpenHashMap coverageCounts = coveredChunksByDimension.get(dimension);
        return coverageCounts != null && !coverageCounts.isEmpty();
    }

    public synchronized boolean isCovered(BlockPos blockPos, ResourceLocation dimension) {
        Long2IntOpenHashMap coverageCounts = coveredChunksByDimension.get(dimension);
        return coverageCounts != null && coverageCounts.containsKey(EndSculkSilencerInstance.chunkKey(blockPos));
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
        Map<BlockPos, EndSculkSilencerInstance> instances = instancesByDimension.computeIfAbsent(dimension, ignored -> new HashMap<>());
        EndSculkSilencerInstance previousInstance = instances.get(immutableRegistrationPos);
        if (previousInstance != null && previousInstance.hasSameCoverage(effectCenter, range)) {
            return false;
        }

        if (previousInstance != null) {
            unindex(previousInstance);
        }

        EndSculkSilencerInstance updatedInstance = new EndSculkSilencerInstance(immutableRegistrationPos, effectCenter, dimension, range);
        instances.put(immutableRegistrationPos, updatedInstance);
        index(updatedInstance);
        return true;
    }

    public synchronized boolean remove(BlockPos registrationPos, ResourceLocation dimension) {
        Map<BlockPos, EndSculkSilencerInstance> instances = instancesByDimension.get(dimension);
        if (instances == null) {
            return false;
        }

        EndSculkSilencerInstance removedInstance = instances.remove(registrationPos);
        if (removedInstance == null) {
            return false;
        }

        unindex(removedInstance);
        if (!instances.isEmpty()) {
            return true;
        }

        instancesByDimension.remove(dimension);
        return true;
    }

    public synchronized List<EndSculkSilencerInstance> removeDimension(ResourceLocation dimension) {
        Map<BlockPos, EndSculkSilencerInstance> removedInstances = instancesByDimension.remove(dimension);
        coveredChunksByDimension.remove(dimension);
        return removedInstances == null ? List.of() : List.copyOf(removedInstances.values());
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
        Long2IntOpenHashMap coverageCounts = coveredChunksByDimension.computeIfAbsent(instance.dimension(), ignored -> new Long2IntOpenHashMap());
        instance.forEachCoveredChunk(chunkKey -> coverageCounts.addTo(chunkKey, 1));
    }

    private void unindex(EndSculkSilencerInstance instance) {
        Long2IntOpenHashMap coverageCounts = coveredChunksByDimension.get(instance.dimension());
        if (coverageCounts == null) {
            return;
        }

        instance.forEachCoveredChunk(chunkKey -> decrementCoverage(coverageCounts, chunkKey));
        if (!coverageCounts.isEmpty()) {
            return;
        }

        coveredChunksByDimension.remove(instance.dimension());
    }
}
