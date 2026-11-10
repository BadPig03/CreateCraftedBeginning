package net.ty.createcraftedbeginning.content.opticalpower.network;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetwork.RefreshResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.BooleanSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class OpticalPowerNetworkCache {
    private static final int MAX_TOPOLOGY_REBUILDS_PER_TICK = 8;

    private final Long2ObjectOpenHashMap<CachedNetwork> cachedNetworkByConsumer = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet consumersPendingRebuild = new LongOpenHashSet();
    private final ObjectOpenHashSet<CachedNetwork> networksPendingInvalidation = new ObjectOpenHashSet<>();
    private final OpticalPowerNetworkSpatialIndex spatialIndex = new OpticalPowerNetworkSpatialIndex();
    private final OpticalPowerRefreshSchedule refreshSchedule = new OpticalPowerRefreshSchedule();
    private double observedMaxNetworkFluxLm = -1;
    private double observedMaxConsumerFluxLm = -1;

    private static void applyAllocation(ServerLevel level, BlockPos consumerPos, double fluxLm, CachedNetwork cached) {
        if (!level.isLoaded(consumerPos) || !(level.getBlockEntity(consumerPos) instanceof OpticalPowerConsumerBlockEntity consumer)) {
            return;
        }

        consumer.applyNetworkAllocation(fluxLm, cached.allocationValidity);
    }

    void registerConsumer(ServerLevel level, BlockPos consumerPos) {
        long consumerKey = consumerPos.asLong();
        CachedNetwork cached = cachedNetworkByConsumer.get(consumerKey);
        if (cached != null && cached.isValid()) {
            applyAllocation(level, consumerPos, cached.network.getAllocatedFluxLm(consumerPos), cached);
            return;
        }

        consumersPendingRebuild.add(consumerKey);
    }

    void requestConsumerRebuildIfUnassigned(BlockPos consumerPos) {
        long consumerKey = consumerPos.asLong();
        CachedNetwork cached = cachedNetworkByConsumer.get(consumerKey);
        if (cached != null && cached.isValid()) {
            return;
        }

        consumersPendingRebuild.add(consumerKey);
    }

    void invalidateNetworksAt(ServerLevel level, BlockPos pos) {
        for (CachedNetwork network : spatialIndex.networksAt(pos)) {
            invalidateNetwork(level, network);
        }
    }

    void markNetworksNearChunkForInvalidation(ChunkPos chunkPos) {
        spatialIndex.collectNetworksNearChunk(chunkPos, networksPendingInvalidation);
    }

    void tick(ServerLevel level) {
        List<CachedNetwork> changedNetworks = List.copyOf(networksPendingInvalidation);
        networksPendingInvalidation.clear();
        for (CachedNetwork network : changedNetworks) {
            invalidateNetwork(level, network);
        }
        refreshAllocationsForConfigChanges(level);
        refreshDynamicNetworks(level);
        rebuildPendingNetworks(level);
    }

    @Nullable OpticalPowerNetwork findNetwork(BlockPos consumerPos) {
        CachedNetwork cached = cachedNetworkByConsumer.get(consumerPos.asLong());
        if (cached == null || !cached.isValid()) {
            return null;
        }

        return cached.network;
    }

    private void pushAllocations(ServerLevel level, CachedNetwork cached) {
        OpticalPowerNetwork network = cached.network;
        double fluxLm = network.calculateFluxPerConsumerLm();
        for (BlockPos consumer : network.getConsumers()) {
            if (cachedNetworkByConsumer.get(consumer.asLong()) != cached) {
                continue;
            }

            applyAllocation(level, consumer, fluxLm, cached);
        }
    }

    private void refreshAllocationsForConfigChanges(ServerLevel level) {
        double maxNetworkFluxLm = OpticalPowerNetwork.getMaxNetworkFluxLm();
        double maxConsumerFluxLm = OpticalPowerNetwork.getMaxConsumerFluxLm();
        if (maxNetworkFluxLm == observedMaxNetworkFluxLm && maxConsumerFluxLm == observedMaxConsumerFluxLm) {
            return;
        }

        observedMaxNetworkFluxLm = maxNetworkFluxLm;
        observedMaxConsumerFluxLm = maxConsumerFluxLm;
        ObjectOpenHashSet<CachedNetwork> uniqueNetworks = new ObjectOpenHashSet<>();
        for (CachedNetwork cached : cachedNetworkByConsumer.values()) {
            if (!cached.isValid()) {
                continue;
            }

            uniqueNetworks.add(cached);
        }
        for (CachedNetwork cached : uniqueNetworks) {
            pushAllocations(level, cached);
        }
    }

    private void refreshDynamicNetworks(ServerLevel level) {
        for (CachedNetwork cached : refreshSchedule.dueNetworks(level.getGameTime())) {
            if (!cached.isValid()) {
                continue;
            }

            RefreshResult result = cached.network.refreshDynamicSourceFluxes(level);
            if (result == RefreshResult.RESCAN_REQUIRED) {
                invalidateNetwork(level, cached);
                continue;
            }

            if (result != RefreshResult.TOTAL_CHANGED) {
                continue;
            }

            pushAllocations(level, cached);
        }
    }

    private void rebuildPendingNetworks(ServerLevel level) {
        int rebuilds = 0;
        while (!consumersPendingRebuild.isEmpty() && rebuilds < MAX_TOPOLOGY_REBUILDS_PER_TICK) {
            LongIterator iterator = consumersPendingRebuild.iterator();
            long consumerKey = iterator.nextLong();
            iterator.remove();

            BlockPos consumerPos = BlockPos.of(consumerKey);
            if (!level.isLoaded(consumerPos) || !(level.getBlockState(consumerPos).getBlock() instanceof OpticalPowerConsumer)) {
                continue;
            }

            CachedNetwork existing = cachedNetworkByConsumer.get(consumerKey);
            if (existing != null && existing.isValid()) {
                applyAllocation(level, consumerPos, existing.network.getAllocatedFluxLm(consumerPos), existing);
                continue;
            }

            OpticalPowerNetwork network = OpticalPowerNetworkScanner.scanNetwork(level, consumerPos);
            CachedNetwork cached = new CachedNetwork(network);
            registerCachedNetwork(cached);
            pushAllocations(level, cached);
            rebuilds++;
        }
    }

    private void registerCachedNetwork(CachedNetwork cached) {
        for (BlockPos consumer : cached.network.getConsumers()) {
            long consumerKey = consumer.asLong();
            CachedNetwork previous = cachedNetworkByConsumer.get(consumerKey);
            if (previous != null && previous.isValid()) {
                continue;
            }

            cachedNetworkByConsumer.put(consumerKey, cached);
            consumersPendingRebuild.remove(consumerKey);
        }

        spatialIndex.add(cached);
        refreshSchedule.add(cached);
    }

    private void invalidateNetwork(ServerLevel level, CachedNetwork cached) {
        if (!cached.isValid()) {
            return;
        }

        cached.allocationValidity.valid = false;
        for (BlockPos consumer : cached.network.getConsumers()) {
            long consumerKey = consumer.asLong();
            if (cachedNetworkByConsumer.get(consumerKey) != cached) {
                continue;
            }

            cachedNetworkByConsumer.remove(consumerKey);
            applyAllocation(level, consumer, 0, cached);
            consumersPendingRebuild.add(consumerKey);
        }

        spatialIndex.remove(cached);
        refreshSchedule.remove(cached);
    }

    static final class CachedNetwork {
        final OpticalPowerNetwork network;
        private final AllocationValidity allocationValidity = new AllocationValidity();

        private CachedNetwork(OpticalPowerNetwork network) {
            this.network = network;
        }

        private boolean isValid() {
            return allocationValidity.valid;
        }
    }

    private static final class AllocationValidity implements BooleanSupplier {
        private boolean valid = true;

        @Override
        public boolean getAsBoolean() {
            return valid;
        }
    }
}
