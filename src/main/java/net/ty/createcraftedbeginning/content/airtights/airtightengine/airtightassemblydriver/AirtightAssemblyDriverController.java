package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;

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

    void tick(AirtightTankBlockEntity controller) {
        Level level = controller.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        AirtightAssemblyDriverStructureManager structureManager = driverCore.getStructureManager();
        structureManager.tick(controller);
        if (structureManager.isEvaluationRequired()) {
            flushDirtyState(controller);
            return;
        }

        boolean active = structureManager.isActive();
        updateActiveState(active);
        if (active) {
            consumeBufferedGas(controller.getTankInventory());
            driverCore.getFlowMeter().tick(level);
            driverCore.getResidueManager().tick(level);
        }

        flushDirtyState(controller);
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

    private void consumeBufferedGas(GasTank buffer) {
        GasStack storedGas = buffer.getGasStack();
        if (storedGas.isEmpty()) {
            return;
        }

        IGasHandler handler = driverCore.getGasHandler();
        long acceptedAmount = handler.fill(storedGas, GasAction.SIMULATE);
        if (acceptedAmount <= 0) {
            return;
        }

        GasStack drainableGas = buffer.drain(acceptedAmount, GasAction.SIMULATE);
        if (drainableGas.isEmpty()) {
            return;
        }

        long consumedAmount = handler.fill(drainableGas, GasAction.EXECUTE);
        if (consumedAmount <= 0) {
            return;
        }

        buffer.drain(consumedAmount, GasAction.EXECUTE);
    }

    private void flushDirtyState(AirtightTankBlockEntity controller) {
        if (saveDirty) {
            controller.setChanged();
            saveDirty = false;
        }

        if (!clientDirty) {
            return;
        }

        controller.sendData();
        clientDirty = false;
    }
}
