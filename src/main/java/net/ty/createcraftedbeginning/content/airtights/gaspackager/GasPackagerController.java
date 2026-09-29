package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonFactory;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPackingLimits;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPressureSemantics;
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
        InventoryIdentifier inventoryIdentifier = blockEntity.getGasInventoryIdentifier();
        if (inventoryIdentifier == null) {
            return inventoryTracker.clearAvailableItems();
        }

        GasHandler gasHandler = blockEntity.gasHandlerForController();
        if (gasHandler == null) {
            return inventoryTracker.clearAvailableItems();
        }

        Level level = blockEntity.getLevel();
        long gameTime = level == null ? Long.MIN_VALUE : level.getGameTime();
        boolean packagingPermitted = level != null && BalloonPackingLimits.getLocalPackingLimit(level, blockEntity.getBlockPos()) > 0;
        ScanResult scanResult = inventoryTracker.scan(inventoryIdentifier, gasHandler, gameTime, packagingPermitted);
        if (scanResult.changed()) {
            GasPackagerLogistics.submitNewGasArrivals(blockEntity.getLevel(), blockEntity.getBlockPos(), scanResult.previous(), inventoryIdentifier, scanResult.summary());
        }
        return scanResult.summary();
    }

    boolean unwrapBox(ItemStack box, boolean simulate) {
        if (blockEntity.isGasPackageAnimationActive() || !BalloonItem.containsGas(box)) {
            return false;
        }

        Level level = blockEntity.getLevel();
        if (level == null) {
            return false;
        }

        long ambientPressurePa = BalloonPressureSemantics.ambientPressurePa(level, blockEntity.getBlockPos());
        GasHandler gasHandler = blockEntity.gasHandlerForController();
        if (gasHandler == null || !pendingGas.canStage(box, gasHandler, ambientPressurePa)) {
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

        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        long ambientPressurePa = BalloonPressureSemantics.ambientPressurePa(level, blockEntity.getBlockPos());
        long balloonMaxAmount = BalloonPackingLimits.getLocalPackingLimit(ambientPressurePa);
        GasHandler gasHandler = blockEntity.gasHandlerForController();
        if (gasHandler == null || balloonMaxAmount <= 0) {
            return;
        }

        Result packagingResult = new GasPackagerRequestProcessor(queuedRequests, gasHandler).process(balloonMaxAmount);
        if (packagingResult == null) {
            return;
        }

        GasPackagerLogistics.deductFromAccurateGasSummary(blockEntity.getLevel(), blockEntity.getBlockPos(), packagingResult.deductions());
        blockEntity.enqueueCreatedGasBalloon(packagingResult.balloon());
        blockEntity.markGasInventoryChanged();
        blockEntity.notifyGasPackageUpdate();
    }

    void attemptToPackageAnyGas() {
        Level level = blockEntity.getLevel();
        if (!blockEntity.canStartGasPackage() || level == null) {
            return;
        }

        long ambientPressurePa = BalloonPressureSemantics.ambientPressurePa(level, blockEntity.getBlockPos());
        long balloonMaxAmount = BalloonPackingLimits.getLocalPackingLimit(ambientPressurePa);
        if (balloonMaxAmount <= 0) {
            return;
        }

        GasHandler gasHandler = blockEntity.gasHandlerForController();
        if (gasHandler == null) {
            return;
        }

        GasStack drainedGas = GasPackagerGasTransfer.drainGas(gasHandler, balloonMaxAmount);
        if (drainedGas.isEmpty()) {
            return;
        }

        String outputAddress = blockEntity.signAddressForGasPackage();
        ItemStack balloon = BalloonFactory.create(drainedGas, outputAddress);
        if (balloon.isEmpty()) {
            return;
        }

        blockEntity.enqueueCreatedGasBalloon(balloon);
        blockEntity.markGasInventoryChanged();
        blockEntity.notifyGasPackageUpdate();
    }

    void performPendingGasInsertion() {
        Level level = blockEntity.getLevel();
        long ambientPressurePa = level == null ? GasPressure.VACUUM_PA : BalloonPressureSemantics.ambientPressurePa(level, blockEntity.getBlockPos());
        InsertionResult insertionResult = pendingGas.insertInto(blockEntity.gasHandlerForController(), blockEntity.pendingUnwrappedPackage(), ambientPressurePa);
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

    void invalidateInventoryCache() {
        inventoryTracker.invalidate();
    }
}
