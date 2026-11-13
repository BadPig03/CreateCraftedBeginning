package net.ty.createcraftedbeginning.content.opticalpower.network;

import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetwork.DynamicSourceFailure;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetwork.RefreshResult;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkScanner.ScanResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class OpticalPowerNetworkCache {
    private static final int MAX_TOPOLOGY_REBUILDS_PER_TICK = 8;

    private final Long2ObjectOpenHashMap<CachedNetwork> cachedNetworkByConsumer = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet consumersPendingRebuild = new LongOpenHashSet();
    private final Long2LongOpenHashMap deferredRebuilds = new Long2LongOpenHashMap();
    private final ObjectOpenHashSet<CachedNetwork> networksPendingInvalidation = new ObjectOpenHashSet<>();
    private final OpticalPowerNetworkSpatialIndex spatialIndex = new OpticalPowerNetworkSpatialIndex();
    private final OpticalPowerNetworkScanner scanner = new OpticalPowerNetworkScanner();
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
            applyCachedAllocation(level, consumerPos, cached);
            return;
        }

        queueRebuild(consumerKey);
    }

    void requestConsumerRebuildIfUnassigned(BlockPos consumerPos) {
        long consumerKey = consumerPos.asLong();
        CachedNetwork cached = cachedNetworkByConsumer.get(consumerKey);
        if (cached != null && cached.isValid()) {
            return;
        }

        queueRebuild(consumerKey);
    }

    void invalidateNetworksAt(ServerLevel level, BlockPos pos) {
        scanner.invalidateSource(level, pos);
        for (CachedNetwork network : spatialIndex.networksAt(pos)) {
            invalidateNetwork(level, network, Long.MIN_VALUE);
        }
    }

    void markNetworksNearChunkForInvalidation(ChunkPos chunkPos) {
        spatialIndex.collectNetworksNearChunk(chunkPos, networksPendingInvalidation);
    }

    void tick(ServerLevel level) {
        List<CachedNetwork> changedNetworks = List.copyOf(networksPendingInvalidation);
        networksPendingInvalidation.clear();
        for (CachedNetwork network : changedNetworks) {
            invalidateNetwork(level, network, Long.MIN_VALUE);
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

    private void applyCachedAllocation(ServerLevel level, BlockPos consumer, CachedNetwork cached) {
        try {
            applyAllocation(level, consumer, cached.network.getAllocatedFluxLm(consumer), cached);
        }
        catch (RuntimeException failure) {
            failNetwork(level, cached, "publish", failure);
        }
    }

    private void pushAllocations(ServerLevel level, CachedNetwork cached) {
        OpticalPowerNetwork network = cached.network;
        try {
            double fluxLm = network.calculateFluxPerConsumerLm();
            for (BlockPos consumer : network.getConsumers()) {
                if (cachedNetworkByConsumer.get(consumer.asLong()) != cached) {
                    continue;
                }

                applyAllocation(level, consumer, fluxLm, cached);
            }
        }
        catch (RuntimeException failure) {
            failNetwork(level, cached, "publish", failure);
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

            RefreshResult result;
            try {
                result = cached.network.refreshDynamicSourceFluxes(level);
            }
            catch (DynamicSourceFailure failure) {
                invalidateNetwork(level, cached, scanner.deferDynamicSource(level, failure));
                continue;
            }
            catch (RuntimeException failure) {
                failNetwork(level, cached, "refresh", failure);
                continue;
            }

            if (result == RefreshResult.RESCAN_REQUIRED) {
                invalidateNetwork(level, cached, Long.MIN_VALUE);
                continue;
            }

            if (result != RefreshResult.TOTAL_CHANGED) {
                continue;
            }

            pushAllocations(level, cached);
        }
    }

    private void rebuildPendingNetworks(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<Entry> waiting = deferredRebuilds.long2LongEntrySet().fastIterator();
        while (waiting.hasNext()) {
            Entry entry = waiting.next();
            if (now < entry.getLongValue()) {
                continue;
            }

            consumersPendingRebuild.add(entry.getLongKey());
            waiting.remove();
        }

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
                applyCachedAllocation(level, consumerPos, existing);
                continue;
            }

            rebuilds++;
            ScanResult result;
            try {
                result = scanner.scanNetwork(level, consumerPos);
            }
            catch (RuntimeException failure) {
                deferRebuild(consumerKey, now + OpticalPowerNetworkScanner.RETRY_DELAY_TICKS);
                CCBAPI.LOGGER.warn("Failed to scan the optical network for the consumer at {} in dimension '{}'; retrying after {} ticks.", consumerPos, level.dimension().location(), OpticalPowerNetworkScanner.RETRY_DELAY_TICKS, failure);
                continue;
            }

            OpticalPowerNetwork network = result.network();
            if (network == null) {
                long retryAt = result.retryAt();
                for (BlockPos consumer : result.consumers()) {
                    long key = consumer.asLong();
                    CachedNetwork cached = cachedNetworkByConsumer.get(key);
                    if (cached != null) {
                        invalidateNetwork(level, cached, retryAt);
                    }

                    deferRebuild(key, retryAt);
                }
                continue;
            }

            CachedNetwork cached = new CachedNetwork(network);
            registerCachedNetwork(cached);
            pushAllocations(level, cached);
        }
    }

    private void queueRebuild(long consumerKey) {
        if (deferredRebuilds.containsKey(consumerKey)) {
            return;
        }

        consumersPendingRebuild.add(consumerKey);
    }

    private void deferRebuild(long consumerKey, long retryAt) {
        consumersPendingRebuild.remove(consumerKey);
        deferredRebuilds.put(consumerKey, Math.max(retryAt, deferredRebuilds.getOrDefault(consumerKey, Long.MIN_VALUE)));
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
            deferredRebuilds.remove(consumerKey);
        }

        spatialIndex.add(cached);
        refreshSchedule.add(cached);
    }

    private void failNetwork(ServerLevel level, CachedNetwork cached, String operation, RuntimeException failure) {
        CCBAPI.LOGGER.warn("Failed to {} the optical network allocation at {} in dimension '{}'; retrying after {} ticks.", operation, cached.network.getConsumers().getFirst(), level.dimension().location(), OpticalPowerNetworkScanner.RETRY_DELAY_TICKS, failure);
        invalidateNetwork(level, cached, level.getGameTime() + OpticalPowerNetworkScanner.RETRY_DELAY_TICKS);
    }

    private void invalidateNetwork(ServerLevel level, CachedNetwork cached, long retryAt) {
        if (!cached.isValid()) {
            return;
        }

        cached.allocationValidity.valid = false;
        spatialIndex.remove(cached);
        refreshSchedule.remove(cached);
        networksPendingInvalidation.remove(cached);
        boolean resetFailureReported = false;
        for (BlockPos consumer : cached.network.getConsumers()) {
            long consumerKey = consumer.asLong();
            if (cachedNetworkByConsumer.get(consumerKey) != cached) {
                continue;
            }

            cachedNetworkByConsumer.remove(consumerKey);
            if (retryAt == Long.MIN_VALUE) {
                queueRebuild(consumerKey);
            }
            else {
                deferRebuild(consumerKey, retryAt);
            }
            try {
                applyAllocation(level, consumer, 0, cached);
            }
            catch (RuntimeException failure) {
                if (resetFailureReported) {
                    continue;
                }

                resetFailureReported = true;
                CCBAPI.LOGGER.warn("Failed to clear the optical allocation for the consumer at {} in dimension '{}'; its old allocation is invalid.", consumer, level.dimension().location(), failure);
            }
        }
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
