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
        long total = 0;
        Set<InventoryIdentifier> processedInventories = new HashSet<>();
        for (LogisticallyLinkedBehaviour link : LogisticallyLinkedBehaviour.getAllPresent(network, false)) {
            InventoryIdentifier identifier = getGasInventoryIdentifier(link);
            if (identifier != null && !processedInventories.add(identifier)) {
                continue;
            }

            total += Math.max(0, link.getSummary(ignoredInventory).getCountOf(stack));
            if (total < BigItemStack.INF) {
                continue;
            }

            return BigItemStack.INF;
        }
        return (int) total;
    }

    public static void submitNewArrivals(Collection<RequestPromiseQueue> queues, InventoryIdentifier inventory, @Nullable InventorySummary localPrevious, InventorySummary current) {
        if (queues.isEmpty()) {
            return;
        }

        List<ArrivalDelivery> deliveries = new ArrayList<>();
        InventorySummary currentSnapshot = copySummary(current);
        synchronized (ARRIVAL_SNAPSHOTS) {
            Map<InventorySummary, List<BigItemStack>> increasesByPrevious = new IdentityHashMap<>();
            for (RequestPromiseQueue queue : queues) {
                Map<InventoryIdentifier, InventorySummary> snapshots = ARRIVAL_SNAPSHOTS.computeIfAbsent(queue, $ -> new HashMap<>());
                InventorySummary previous = snapshots.get(inventory);
                if (previous == null) {
                    previous = localPrevious;
                }

                snapshots.put(inventory, currentSnapshot);
                if (previous == null) {
                    continue;
                }

                List<BigItemStack> arrivals = increasesByPrevious.computeIfAbsent(previous, key -> findIncreases(key, currentSnapshot));
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
        InventorySummary copy = new InventorySummary();
        for (BigItemStack entry : source.getStacks()) {
            copy.add(entry.stack.copy(), entry.count);
        }
        return copy;
    }

    private static List<BigItemStack> findIncreases(InventorySummary previous, InventorySummary current) {
        List<BigItemStack> increases = new ArrayList<>();
        for (BigItemStack entry : current.getStacks()) {
            int increase = entry.count - previous.getCountOf(entry.stack);
            if (increase <= 0) {
                continue;
            }

            increases.add(new BigItemStack(entry.stack.copyWithCount(1), increase));
        }
        return increases;
    }

    private record ArrivalDelivery(RequestPromiseQueue queue, List<BigItemStack> arrivals) {}
}
