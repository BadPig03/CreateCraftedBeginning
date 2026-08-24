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
public final class GasPackagerController {
    private final GasPackagerBlockEntity blockEntity;
    private final GasPackagerInventoryTracker inventoryTracker;
    private final GasPackagerPendingGas pendingGas;

    public GasPackagerController(GasPackagerBlockEntity blockEntity, GasPackagerInventoryTracker inventoryTracker, GasPackagerPendingGas pendingGas) {
        this.blockEntity = blockEntity;
        this.inventoryTracker = inventoryTracker;
        this.pendingGas = pendingGas;
    }

    public InventorySummary getAvailableItems() {
        InventoryIdentifier inventoryIdentifier = blockEntity.getGasInventoryIdentifier();
        if (inventoryIdentifier == null) {
            return inventoryTracker.clearAvailableItems();
        }

        IGasHandler gasHandler = blockEntity.gasHandlerForController();
        if (gasHandler == null) {
            return inventoryTracker.clearAvailableItems();
        }

        long gameTime = blockEntity.getLevel() == null ? Long.MIN_VALUE : blockEntity.getLevel().getGameTime();
        ScanResult scanResult = inventoryTracker.scan(inventoryIdentifier, gasHandler, gameTime);
        if (scanResult.changed()) {
            GasPackagerLogistics.submitNewGasArrivals(blockEntity.getLevel(), blockEntity.getBlockPos(), scanResult.previous(), inventoryIdentifier, scanResult.summary());
        }
        return scanResult.summary();
    }

    public boolean unwrapBox(ItemStack box, boolean simulate) {
        if (blockEntity.isGasPackageAnimationActive() || !BalloonUtils.containsGasContents(box)) {
            return false;
        }

        IGasHandler gasHandler = blockEntity.gasHandlerForController();
        if (gasHandler == null || !pendingGas.canStage(box, gasHandler)) {
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

    public void attemptToSend(List<PackagingRequest> queuedRequests) {
        if (queuedRequests.isEmpty()) {
            return;
        }

        if (blockEntity.getGasInventoryIdentifier() == null) {
            queuedRequests.removeFirst();
            return;
        }

        IGasHandler gasHandler = blockEntity.gasHandlerForController();
        long balloonCapacity = BalloonUtils.getCapacity();
        if (gasHandler == null || balloonCapacity <= 0) {
            return;
        }

        Result packagingResult = GasPackagerRequestProcessor.process(queuedRequests, gasHandler, balloonCapacity);
        if (packagingResult == null) {
            return;
        }

        GasPackagerLogistics.deductFromAccurateGasSummary(blockEntity.getLevel(), blockEntity.getBlockPos(), packagingResult.deductions());
        blockEntity.enqueueCreatedGasBalloon(packagingResult.balloon());
        blockEntity.markGasInventoryChanged();
        blockEntity.notifyGasPackageUpdate();
    }

    public void attemptToPackageAnyGas() {
        if (!blockEntity.canStartGasPackage()) {
            return;
        }

        IGasHandler gasHandler = blockEntity.gasHandlerForController();
        if (gasHandler == null) {
            return;
        }

        BalloonGasContents drainedContents = GasPackagerUtils.drainContents(gasHandler, BalloonUtils.getCapacity());
        if (drainedContents.isEmpty()) {
            return;
        }

        ItemStack balloon = BalloonUtils.containing(drainedContents);
        PackageItem.clearAddress(balloon);
        String outputAddress = blockEntity.signAddressForGasPackage();
        if (!outputAddress.isBlank()) {
            PackageItem.addAddress(balloon, outputAddress);
        }

        blockEntity.enqueueCreatedGasBalloon(balloon);
        blockEntity.markGasInventoryChanged();
        blockEntity.notifyGasPackageUpdate();
    }

    public void performPendingGasInsertion() {
        InsertionResult insertionResult = pendingGas.insertInto(blockEntity.gasHandlerForController(), blockEntity.pendingUnwrappedPackage());
        if (!insertionResult.returnedPackage().isEmpty()) {
            blockEntity.enqueueReturnedGasBalloon(insertionResult.returnedPackage());
        }
        pendingGas.clear();

        if (insertionResult.inventoryChanged()) {
            blockEntity.markGasInventoryChanged();
        }
        else {
            blockEntity.requestGasStockCheck();
        }
        blockEntity.notifyGasPackageUpdate();
    }

    public void invalidateInventoryCache() {
        inventoryTracker.invalidate();
    }
}
