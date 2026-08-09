package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasContents;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerInventoryTracker.ScanResult;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerPendingGas.InsertionResult;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerRequestProcessor.Result;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasPackagerController {
    private final GasPackagerBlockEntity blockEntity;
    private final GasPackagerInventoryTracker inventoryTracker;
    private final GasPackagerPendingGas pendingGas;

    GasPackagerController(GasPackagerBlockEntity blockEntity, GasPackagerInventoryTracker inventoryTracker, GasPackagerPendingGas pendingGas) {
        this.blockEntity = blockEntity;
        this.inventoryTracker = inventoryTracker;
        this.pendingGas = pendingGas;
    }

    InventorySummary getAvailableItems() {
        InventoryIdentifier identifier = blockEntity.getGasInventoryIdentifier();
        if (identifier == null) {
            return inventoryTracker.clearAvailableItems();
        }

        IGasHandler handler = blockEntity.gasHandlerForController();
        if (handler == null) {
            return inventoryTracker.clearAvailableItems();
        }

        long currentTick = blockEntity.getLevel() == null ? Long.MIN_VALUE : blockEntity.getLevel().getGameTime();
        ScanResult scan = inventoryTracker.scan(identifier, handler, currentTick);
        if (scan.changed()) {
            GasPackagerLogistics.submitNewGasArrivals(blockEntity.getLevel(), blockEntity.getBlockPos(), scan.previous(), identifier, scan.summary());
        }
        return scan.summary();
    }

    boolean unwrapBox(ItemStack box, boolean simulate) {
        if (blockEntity.isGasPackageAnimationActive() || !BalloonUtils.containsGasContents(box)) {
            return false;
        }

        IGasHandler handler = blockEntity.gasHandlerForController();
        if (handler == null || !pendingGas.canStage(box, handler)) {
            return false;
        }

        if (simulate) {
            return true;
        }

        pendingGas.stage(box);
        blockEntity.beginGasPackageInsertion(box);
        blockEntity.emitGasPackageReceivedEvent(box);
        blockEntity.notifyGasPackageUpdate();
        return true;
    }

    void attemptToSend(List<PackagingRequest> queuedRequests) {
        if (queuedRequests.isEmpty()) {
            return;
        }

        if (blockEntity.getGasInventoryIdentifier() == null) {
            queuedRequests.removeFirst();
            return;
        }

        IGasHandler handler = blockEntity.gasHandlerForController();
        long capacity = BalloonUtils.getCapacity();
        if (handler == null || capacity <= 0) {
            return;
        }

        Result result = GasPackagerRequestProcessor.process(queuedRequests, handler, capacity);
        if (result == null) {
            return;
        }

        GasPackagerLogistics.deductFromAccurateGasSummary(blockEntity.getLevel(), blockEntity.getBlockPos(), result.deductions());
        blockEntity.enqueueCreatedGasBalloon(result.balloon());
        blockEntity.markGasInventoryChanged();
        blockEntity.notifyGasPackageUpdate();
    }

    void attemptToPackageAnyGas() {
        if (!blockEntity.canStartGasPackage()) {
            return;
        }

        IGasHandler handler = blockEntity.gasHandlerForController();
        if (handler == null) {
            return;
        }

        BalloonGasContents drained = GasPackagerUtils.drainContents(handler, BalloonUtils.getCapacity());
        if (drained.isEmpty()) {
            return;
        }

        ItemStack balloon = BalloonUtils.containing(drained);
        PackageItem.clearAddress(balloon);
        String address = blockEntity.signAddressForGasPackage();
        if (!address.isBlank()) {
            PackageItem.addAddress(balloon, address);
        }

        blockEntity.enqueueCreatedGasBalloon(balloon);
        blockEntity.markGasInventoryChanged();
        blockEntity.notifyGasPackageUpdate();
    }

    void performPendingGasInsertion() {
        InsertionResult result = pendingGas.insertInto(blockEntity.gasHandlerForController(), blockEntity.pendingUnwrappedPackage());
        if (!result.returnedPackage().isEmpty()) {
            blockEntity.enqueueReturnedGasBalloon(result.returnedPackage());
        }
        pendingGas.clear();

        if (result.inventoryChanged()) {
            blockEntity.markGasInventoryChanged();
        }
        else {
            blockEntity.requestGasStockCheck();
        }
        blockEntity.notifyGasPackageUpdate();
    }

    void invalidateInventoryCache() {
        inventoryTracker.invalidate();
    }
}
