package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.storage.GasTank;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightAssemblyDriverController {
    private final AirtightAssemblyDriverCore driverCore;

    private boolean saveDirty;
    private boolean clientDirty;
    private boolean activeState;
    private boolean activeStateInitialized;

    AirtightAssemblyDriverController(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    void tick(AirtightTankBlockEntity tankController) {
        Level level = tankController.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        AirtightAssemblyDriverStructureManager structureManager = driverCore.getStructureManager();
        structureManager.tick(tankController);
        if (structureManager.isEvaluationRequired()) {
            flushDirtyState(tankController);
            return;
        }

        boolean isActive = structureManager.isActive();
        updateActiveState(isActive);
        if (isActive) {
            consumeBufferedGas(tankController.getTankInventory());
            driverCore.getFlowMeter().tick(level);
            driverCore.getResidueManager().tick(level);
        }

        flushDirtyState(tankController);
    }

    void markForSave() {
        saveDirty = true;
    }

    void markForClientSync() {
        clientDirty = true;
    }

    void markForSaveAndClientSync() {
        saveDirty = true;
        clientDirty = true;
    }

    void reset() {
        activeState = false;
        activeStateInitialized = true;
        driverCore.getFlowMeter().reset();
        driverCore.getStructureManager().reset();
        driverCore.getLevelCalculator().reset();
        driverCore.getResidueManager().reset();
    }

    void onReadComplete() {
        saveDirty = false;
        clientDirty = false;
    }

    void onPersistentLoaded() {
        activeState = false;
        activeStateInitialized = false;
    }

    private void updateActiveState(boolean active) {
        if (activeStateInitialized && activeState == active) {
            return;
        }

        activeState = active;
        activeStateInitialized = true;
        if (active) {
            return;
        }

        driverCore.getFlowMeter().reset();
    }

    private void consumeBufferedGas(GasTank gasBuffer) {
        GasStack storedGas = gasBuffer.getGasStack();
        if (storedGas.isEmpty()) {
            return;
        }

        long sourcePressurePa = gasBuffer.getPressurePa();
        GasPressureBoundary gasHandler = driverCore.getGasHandler();
        long acceptedAmount = gasHandler.fillFromPressure(storedGas, sourcePressurePa, GasAction.SIMULATE);
        if (acceptedAmount <= 0) {
            return;
        }

        GasStack drainableGas = gasBuffer.drain(acceptedAmount, GasAction.SIMULATE);
        if (drainableGas.isEmpty()) {
            return;
        }

        long consumedAmount = gasHandler.fillFromPressure(drainableGas, sourcePressurePa, GasAction.EXECUTE);
        if (consumedAmount <= 0) {
            return;
        }

        gasBuffer.drain(consumedAmount, GasAction.EXECUTE);
    }

    private void flushDirtyState(AirtightTankBlockEntity tankController) {
        if (saveDirty) {
            tankController.setChanged();
            saveDirty = false;
        }

        if (!clientDirty) {
            return;
        }

        tankController.sendData();
        clientDirty = false;
    }
}
