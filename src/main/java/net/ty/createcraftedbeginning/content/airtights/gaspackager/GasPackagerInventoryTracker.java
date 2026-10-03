package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerGasTransfer.PackagingTankSnapshot;
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
    private GasHandler availableItemsHandler;
    private List<PackagingTankSnapshot> availableTankSnapshot = List.of();
    private boolean availablePackagingPermitted;
    private long availableItemsScanTick = Long.MIN_VALUE;

    private static InventorySummary createGasInventorySummary(List<PackagingTankSnapshot> tankSnapshot, boolean packagingPermitted) {
        InventorySummary inventorySummary = new InventorySummary();
        if (!packagingPermitted) {
            return inventorySummary;
        }

        for (PackagingTankSnapshot tank : tankSnapshot) {
            GasStack tankGas = tank.gas();
            int amount = (int) Mth.clamp(tank.packageableAmount(), 0L, BigItemStack.INF);
            if (tankGas.isEmpty() || amount <= 0) {
                continue;
            }

            ItemStack virtualItem = VirtualGasItems.createVirtualItem(tankGas.copyWithAmount(1));
            if (virtualItem.isEmpty()) {
                continue;
            }

            inventorySummary.add(virtualItem, amount);
        }
        return inventorySummary;
    }

    ScanResult scan(@Nullable InventoryIdentifier identifier, @Nullable GasHandler handler, long currentTick, boolean packagingPermitted) {
        if (identifier == null || handler == null) {
            return new ScanResult(clear(), null, false);
        }

        boolean isSameSource = handler == availableItemsHandler && identifier.equals(availableItemsIdentifier);
        if (isSameSource && availableItemsScanTick == currentTick && packagingPermitted == availablePackagingPermitted) {
            return new ScanResult(availableItems, null, false);
        }

        availableItemsScanTick = currentTick;
        List<PackagingTankSnapshot> tankSnapshot = GasPackagerGasTransfer.snapshotPackagingTanks(handler);
        boolean sameTankSnapshot = isSameSource && GasPackagerGasTransfer.matchesPackagingTankSnapshots(tankSnapshot, availableTankSnapshot);
        if (sameTankSnapshot && packagingPermitted == availablePackagingPermitted) {
            return new ScanResult(availableItems, null, false);
        }

        InventorySummary previousSummary = isSameSource ? availableItems : null;
        InventorySummary currentSummary = createGasInventorySummary(tankSnapshot, packagingPermitted);
        availableItems = currentSummary;
        availableItemsIdentifier = identifier;
        availableItemsHandler = handler;
        availableTankSnapshot = tankSnapshot;
        availablePackagingPermitted = packagingPermitted;
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
        availablePackagingPermitted = false;
        availableItemsScanTick = Long.MIN_VALUE;
        return availableItems;
    }

    record ScanResult(InventorySummary summary, @Nullable InventorySummary previous, boolean changed) {}
}
