package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasLogisticsUtils {
    private static final Map<RequestPromiseQueue, Map<InventoryIdentifier, InventorySummary>> ARRIVAL_SNAPSHOTS = new WeakHashMap<>();

    private GasLogisticsUtils() {
    }

    public static int getUniqueStockOf(UUID network, ItemStack stack, @Nullable IdentifiedInventory ignoredInventory) {
        long totalStock = 0;
        Set<InventoryIdentifier> processedInventories = new HashSet<>();
        for (LogisticallyLinkedBehaviour link : LogisticallyLinkedBehaviour.getAllPresent(network, false)) {
            InventoryIdentifier gasInventoryIdentifier = getGasInventoryIdentifier(link);
            if (gasInventoryIdentifier != null && !processedInventories.add(gasInventoryIdentifier)) {
                continue;
            }

            totalStock += Math.max(0, link.getSummary(ignoredInventory).getCountOf(stack));
            if (totalStock < BigItemStack.INF) {
                continue;
            }

            return BigItemStack.INF;
        }
        return (int) totalStock;
    }

    static void submitNewArrivals(Collection<RequestPromiseQueue> queues, InventoryIdentifier inventory, @Nullable InventorySummary localPrevious, InventorySummary current) {
        if (queues.isEmpty()) {
            return;
        }

        List<ArrivalDelivery> deliveries = new ArrayList<>();
        InventorySummary currentSnapshot = copySummary(current);
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

                List<BigItemStack> arrivals = increasesByPrevious.computeIfAbsent(previousSnapshot, snapshot -> findIncreases(snapshot, currentSnapshot));
                if (!arrivals.isEmpty()) {
                    deliveries.add(new ArrivalDelivery(queue, arrivals));
                }
            }
        }

        for (ArrivalDelivery delivery : deliveries) {
            for (BigItemStack arrival : delivery.arrivals()) {
                delivery.queue().itemEnteredSystem(arrival.stack, arrival.count);
            }
        }
    }

    @Nullable
    private static InventoryIdentifier getGasInventoryIdentifier(LogisticallyLinkedBehaviour link) {
        if (!(link.blockEntity instanceof PackagerLinkBlockEntity linkEntity) || !(linkEntity.getPackager() instanceof GasPackagerBlockEntity packager)) {
            return null;
        }
        return packager.getGasInventoryIdentifier();
    }

    private static InventorySummary copySummary(InventorySummary source) {
        InventorySummary snapshotCopy = new InventorySummary();
        for (BigItemStack entry : source.getStacks()) {
            snapshotCopy.add(entry.stack.copy(), entry.count);
        }
        return snapshotCopy;
    }

    private static List<BigItemStack> findIncreases(InventorySummary previous, InventorySummary current) {
        List<BigItemStack> stockIncreases = new ArrayList<>();
        for (BigItemStack entry : current.getStacks()) {
            int increasedCount = entry.count - previous.getCountOf(entry.stack);
            if (increasedCount <= 0) {
                continue;
            }

            stockIncreases.add(new BigItemStack(entry.stack.copyWithCount(1), increasedCount));
        }
        return stockIncreases;
    }

    private record ArrivalDelivery(RequestPromiseQueue queue, List<BigItemStack> arrivals) {}
}
