package net.ty.createcraftedbeginning.content.opticalpower.network;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkCache.CachedNetwork;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class OpticalPowerRefreshSchedule {
    private static final int DYNAMIC_REFRESH_INTERVAL_TICKS = 20;

    private final List<Set<CachedNetwork>> dynamicRefreshBuckets = new ArrayList<>(DYNAMIC_REFRESH_INTERVAL_TICKS);
    private final Map<CachedNetwork, Set<CachedNetwork>> bucketByNetwork = new IdentityHashMap<>();
    private int nextDynamicRefreshBucket;

    OpticalPowerRefreshSchedule() {
        for (int i = 0; i < DYNAMIC_REFRESH_INTERVAL_TICKS; i++) {
            dynamicRefreshBuckets.add(new ObjectOpenHashSet<>());
        }
    }

    void add(CachedNetwork cached) {
        if (!cached.network.hasDynamicSources()) {
            return;
        }

        Set<CachedNetwork> bucket = dynamicRefreshBuckets.get(nextDynamicRefreshBucket);
        nextDynamicRefreshBucket = (nextDynamicRefreshBucket + 1) % DYNAMIC_REFRESH_INTERVAL_TICKS;
        bucket.add(cached);
        bucketByNetwork.put(cached, bucket);
    }

    void remove(CachedNetwork cached) {
        Set<CachedNetwork> bucket = bucketByNetwork.remove(cached);
        if (bucket == null) {
            return;
        }

        bucket.remove(cached);
    }

    List<CachedNetwork> dueNetworks(long gameTime) {
        int bucketIndex = Math.floorMod(gameTime, DYNAMIC_REFRESH_INTERVAL_TICKS);
        Set<CachedNetwork> bucket = dynamicRefreshBuckets.get(bucketIndex);
        if (bucket.isEmpty()) {
            return List.of();
        }

        return List.copyOf(bucket);
    }
}
