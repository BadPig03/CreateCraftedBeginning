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

    private static InventorySummary createGasInventorySummary(List<GasStack> tankSnapshot) {
        InventorySummary inventorySummary = new InventorySummary();
        for (GasStack tankGas : tankSnapshot) {
            int amount = GasRequestUtils.toLogisticsAmount(tankGas.getAmount());
            if (amount <= 0) {
                continue;
            }

            ItemStack virtualItem = GasVirtualUtils.createVirtualItem(tankGas.copyWithAmount(1));
            if (virtualItem.isEmpty()) {
                continue;
            }

            inventorySummary.add(virtualItem, amount);
        }
        return inventorySummary;
    }

    ScanResult scan(@Nullable InventoryIdentifier identifier, @Nullable IGasHandler handler, long currentTick) {
        if (identifier == null || handler == null) {
            return new ScanResult(clear(), null, false);
        }

        boolean isSameSource = handler == availableItemsHandler && identifier.equals(availableItemsIdentifier);
        if (isSameSource && availableItemsScanTick == currentTick) {
            return new ScanResult(availableItems, null, false);
        }

        availableItemsScanTick = currentTick;
        if (isSameSource && GasPackagerUtils.matchesTankSnapshot(handler, availableTankSnapshot)) {
            return new ScanResult(availableItems, null, false);
        }

        InventorySummary previousSummary = isSameSource ? availableItems : null;
        List<GasStack> tankSnapshot = GasPackagerUtils.snapshotTanks(handler);
        InventorySummary currentSummary = createGasInventorySummary(tankSnapshot);
        availableItems = currentSummary;
        availableItemsIdentifier = identifier;
        availableItemsHandler = handler;
        availableTankSnapshot = tankSnapshot;
        return new ScanResult(currentSummary, previousSummary, true);
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
