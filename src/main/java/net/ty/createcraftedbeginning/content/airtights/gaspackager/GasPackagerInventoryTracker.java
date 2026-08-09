package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasPackagerInventoryTracker {
    private InventorySummary availableItems = new InventorySummary();
    @Nullable
    private InventoryIdentifier availableItemsIdentifier;
    @Nullable
    private IGasHandler availableItemsHandler;
    private List<GasStack> availableTankSnapshot = List.of();
    private long availableItemsScanTick = Long.MIN_VALUE;

    private static InventorySummary createGasInventorySummary(List<GasStack> snapshot) {
        InventorySummary summary = new InventorySummary();
        for (GasStack gas : snapshot) {
            int amount = GasRequestUtils.toLogisticsAmount(gas.getAmount());
            if (amount <= 0) {
                continue;
            }

            ItemStack virtualItem = GasVirtualUtils.createVirtualItem(gas.copyWithAmount(1));
            if (virtualItem.isEmpty()) {
                continue;
            }

            summary.add(virtualItem, amount);
        }
        return summary;
    }

    ScanResult scan(@Nullable InventoryIdentifier identifier, @Nullable IGasHandler handler, long currentTick) {
        if (identifier == null || handler == null) {
            return new ScanResult(clear(), null, false);
        }

        boolean sameSource = handler == availableItemsHandler && identifier.equals(availableItemsIdentifier);
        if (sameSource && availableItemsScanTick == currentTick) {
            return new ScanResult(availableItems, null, false);
        }

        availableItemsScanTick = currentTick;
        if (sameSource && GasPackagerUtils.matchesTankSnapshot(handler, availableTankSnapshot)) {
            return new ScanResult(availableItems, null, false);
        }

        InventorySummary previous = sameSource ? availableItems : null;
        List<GasStack> snapshot = GasPackagerUtils.snapshotTanks(handler);
        InventorySummary summary = createGasInventorySummary(snapshot);
        availableItems = summary;
        availableItemsIdentifier = identifier;
        availableItemsHandler = handler;
        availableTankSnapshot = snapshot;
        return new ScanResult(summary, previous, true);
    }

    void invalidate() {
        availableItemsScanTick = Long.MIN_VALUE;
    }

    InventorySummary clearAvailableItems() {
        return clear();
    }

    private InventorySummary clear() {
        if (availableItemsIdentifier != null || availableItemsHandler != null || !availableTankSnapshot.isEmpty()) {
            availableItems = new InventorySummary();
        }
        availableItemsIdentifier = null;
        availableItemsHandler = null;
        availableTankSnapshot = List.of();
        availableItemsScanTick = Long.MIN_VALUE;
        return availableItems;
    }

    record ScanResult(InventorySummary summary, @Nullable InventorySummary previous, boolean changed) {}
}
