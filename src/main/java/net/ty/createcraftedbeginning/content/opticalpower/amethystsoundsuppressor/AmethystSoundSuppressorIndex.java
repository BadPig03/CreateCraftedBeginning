package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystSoundSuppressorIndex {
    private final Map<ResourceLocation, Map<AmethystSoundSuppressorIdentity, AmethystSoundSuppressorInstance>> instancesByDimension = new HashMap<>();
    private final Map<ResourceLocation, Long2ObjectOpenHashMap<Set<AmethystSoundSuppressorIdentity>>> coveredChunksByDimension = new HashMap<>();

    synchronized short getRange(AmethystSoundSuppressorIdentity identity, ResourceLocation dimension) {
        Map<AmethystSoundSuppressorIdentity, AmethystSoundSuppressorInstance> instances = instancesByDimension.get(dimension);
        if (instances == null) {
            return AmethystSoundSuppressorBlockEntity.BASE_WORKING_RANGE;
        }

        AmethystSoundSuppressorInstance instance = instances.get(identity);
        if (instance == null) {
            return AmethystSoundSuppressorBlockEntity.BASE_WORKING_RANGE;
        }

        return instance.range();
    }

    synchronized boolean hasCoverage(ResourceLocation dimension) {
        Long2ObjectOpenHashMap<Set<AmethystSoundSuppressorIdentity>> coveredChunks = coveredChunksByDimension.get(dimension);
        return coveredChunks != null && !coveredChunks.isEmpty();
    }

    synchronized boolean isCovered(BlockPos blockPos, ResourceLocation dimension) {
        Long2ObjectOpenHashMap<Set<AmethystSoundSuppressorIdentity>> coveredChunks = coveredChunksByDimension.get(dimension);
        return coveredChunks != null && coveredChunks.containsKey(AmethystSoundSuppressorInstance.chunkKey(blockPos));
    }

    synchronized @Nullable AmethystSoundSuppressorInstance findCoveringSuppressor(BlockPos blockPos, ResourceLocation dimension, Predicate<AmethystSoundSuppressorIdentity> filter) {
        Long2ObjectOpenHashMap<Set<AmethystSoundSuppressorIdentity>> coveredChunks = coveredChunksByDimension.get(dimension);
        if (coveredChunks == null) {
            return null;
        }

        Set<AmethystSoundSuppressorIdentity> identities = coveredChunks.get(AmethystSoundSuppressorInstance.chunkKey(blockPos));
        if (identities == null) {
            return null;
        }

        AmethystSoundSuppressorIdentity selected = null;
        for (AmethystSoundSuppressorIdentity identity : identities) {
            if (!filter.test(identity)) {
                continue;
            }

            if (selected != null && selected.compareTo(identity) <= 0) {
                continue;
            }

            selected = identity;
        }
        if (selected == null) {
            return null;
        }

        return instancesByDimension.get(dimension).get(selected);
    }

    synchronized boolean update(AmethystSoundSuppressorIdentity identity, BlockPos effectCenter, ResourceLocation dimension, short range, @Nullable AmethystSoundSuppressorBlockEntity nativeOwner) {
        if (range <= 0) {
            return remove(identity, dimension, nativeOwner);
        }

        Map<AmethystSoundSuppressorIdentity, AmethystSoundSuppressorInstance> instances = instancesByDimension.computeIfAbsent(dimension, ignored -> new HashMap<>());
        AmethystSoundSuppressorInstance previousInstance = instances.get(identity);
        if (previousInstance != null && previousInstance.hasSameCoverage(effectCenter, range)) {
            if (previousInstance.nativeOwner() != nativeOwner || !previousInstance.effectCenter().equals(effectCenter)) {
                instances.put(identity, new AmethystSoundSuppressorInstance(identity, effectCenter, dimension, range, nativeOwner));
            }

            return false;
        }

        if (previousInstance != null) {
            unindex(previousInstance);
        }

        AmethystSoundSuppressorInstance updatedInstance = new AmethystSoundSuppressorInstance(identity, effectCenter, dimension, range, nativeOwner);
        instances.put(identity, updatedInstance);
        index(updatedInstance);
        return true;
    }

    synchronized boolean remove(AmethystSoundSuppressorIdentity identity, ResourceLocation dimension, @Nullable AmethystSoundSuppressorBlockEntity nativeOwner) {
        Map<AmethystSoundSuppressorIdentity, AmethystSoundSuppressorInstance> instances = instancesByDimension.get(dimension);
        if (instances == null) {
            return false;
        }

        AmethystSoundSuppressorInstance instance = instances.get(identity);
        if (instance == null || instance.nativeOwner() != nativeOwner) {
            return false;
        }

        instances.remove(identity);
        unindex(instance);
        if (!instances.isEmpty()) {
            return true;
        }

        instancesByDimension.remove(dimension);
        return true;
    }

    synchronized List<AmethystSoundSuppressorInstance> removeDimension(ResourceLocation dimension) {
        Map<AmethystSoundSuppressorIdentity, AmethystSoundSuppressorInstance> removedInstances = instancesByDimension.remove(dimension);
        coveredChunksByDimension.remove(dimension);
        if (removedInstances == null) {
            return List.of();
        }

        return List.copyOf(removedInstances.values());
    }

    synchronized List<AmethystSoundSuppressorInstance> getInstances(ResourceLocation dimension) {
        Map<AmethystSoundSuppressorIdentity, AmethystSoundSuppressorInstance> instances = instancesByDimension.get(dimension);
        if (instances == null) {
            return List.of();
        }

        return List.copyOf(instances.values());
    }

    synchronized void clear() {
        instancesByDimension.clear();
        coveredChunksByDimension.clear();
    }

    private void index(AmethystSoundSuppressorInstance instance) {
        Long2ObjectOpenHashMap<Set<AmethystSoundSuppressorIdentity>> coveredChunks = coveredChunksByDimension.computeIfAbsent(instance.dimension(), ignored -> new Long2ObjectOpenHashMap<>());
        AmethystSoundSuppressorIdentity identity = instance.identity();
        instance.forEachCoveredChunk(chunkKey -> coveredChunks.computeIfAbsent(chunkKey, ignored -> new HashSet<>(1)).add(identity));
    }

    private void unindex(AmethystSoundSuppressorInstance instance) {
        ResourceLocation dimension = instance.dimension();
        Long2ObjectOpenHashMap<Set<AmethystSoundSuppressorIdentity>> coveredChunks = coveredChunksByDimension.get(dimension);
        if (coveredChunks == null) {
            return;
        }

        AmethystSoundSuppressorIdentity identity = instance.identity();
        instance.forEachCoveredChunk(chunkKey -> {
            Set<AmethystSoundSuppressorIdentity> identities = coveredChunks.get(chunkKey);
            if (identities == null) {
                return;
            }

            identities.remove(identity);
            if (!identities.isEmpty()) {
                return;
            }

            coveredChunks.remove(chunkKey);
        });
        if (!coveredChunks.isEmpty()) {
            return;
        }

        coveredChunksByDimension.remove(dimension);
    }
}
