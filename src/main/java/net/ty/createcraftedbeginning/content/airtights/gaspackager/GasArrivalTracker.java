package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasArrivalTracker {
    private static final Map<RequestPromiseQueue, Map<InventoryIdentifier, InventorySummary>> ARRIVAL_SNAPSHOTS = new WeakHashMap<>();

    private GasArrivalTracker() {
    }

    static void submitNewArrivals(Collection<RequestPromiseQueue> queues, InventoryIdentifier inventory, @Nullable InventorySummary localPrevious, InventorySummary current) {
        if (queues.isEmpty()) {
            return;
        }

        List<ArrivalDelivery> deliveries = new ArrayList<>();
        InventorySummary currentSnapshot = new InventorySummary();
        for (BigItemStack entry : current.getStacks()) {
            currentSnapshot.add(entry.stack.copy(), entry.count);
        }
        synchronized (ARRIVAL_SNAPSHOTS) {
            Map<InventorySummary, List<BigItemStack>> increasesByPrevious = new IdentityHashMap<>();
            for (RequestPromiseQueue queue : queues) {
                Map<InventoryIdentifier, InventorySummary> inventorySnapshots = ARRIVAL_SNAPSHOTS.computeIfAbsent(queue, ignoredQueue -> new HashMap<>());
                InventorySummary previousSnapshot = inventorySnapshots.get(inventory);
                if (previousSnapshot == null) {
                    previousSnapshot = localPrevious;
                }

                inventorySnapshots.put(inventory, currentSnapshot);
                if (previousSnapshot == null) {
                    continue;
                }

                List<BigItemStack> arrivals = increasesByPrevious.computeIfAbsent(previousSnapshot, snapshot -> {
                    List<BigItemStack> stockIncreases = new ArrayList<>();
                    for (BigItemStack entry : currentSnapshot.getStacks()) {
                        int increasedCount = entry.count - snapshot.getCountOf(entry.stack);
                        if (increasedCount <= 0) {
                            continue;
                        }

                        stockIncreases.add(new BigItemStack(entry.stack.copyWithCount(1), increasedCount));
                    }
                    return stockIncreases;
                });

                if (arrivals.isEmpty()) {
                    continue;
                }

                deliveries.add(new ArrivalDelivery(queue, arrivals));
            }
        }

        for (ArrivalDelivery delivery : deliveries) {
            for (BigItemStack arrival : delivery.arrivals()) {
                delivery.queue().itemEnteredSystem(arrival.stack, arrival.count);
            }
        }
    }

    private record ArrivalDelivery(RequestPromiseQueue queue, List<BigItemStack> arrivals) {}
}
