package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandler;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightAssemblyDriverCore {
    public static final int MAX_LEVEL = AirtightEngineHandler.MAX_LEVEL;

    private static final String COMPOUND_KEY_FLOW_METER = "FlowMeter";
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";
    private static final String COMPOUND_KEY_LEVEL_CALCULATOR = "LevelCalculator";
    private static final String COMPOUND_KEY_RESIDUE_MANAGER = "ResidueManager";

    private final AirtightAssemblyDriverFlowMeter flowMeter;
    private final AirtightAssemblyDriverLevelCalculator levelCalculator;
    private final AirtightAssemblyDriverResidueManager residueManager;
    private final AirtightAssemblyDriverStructureManager structureManager;
    private final AirtightAssemblyDriverTooltipBuilder tooltipBuilder;
    private final AirtightEngineGasHandler gasHandler;

    private boolean saveDirty;
    private boolean clientDirty;
    private boolean activeState;
    private boolean activeStateInitialized;

    public AirtightAssemblyDriverCore() {
        flowMeter = new AirtightAssemblyDriverFlowMeter(this);
        residueManager = new AirtightAssemblyDriverResidueManager(this);
        structureManager = new AirtightAssemblyDriverStructureManager(this);
        tooltipBuilder = new AirtightAssemblyDriverTooltipBuilder(this);
        levelCalculator = new AirtightAssemblyDriverLevelCalculator(this);
        gasHandler = new AirtightEngineGasHandler();
    }

    public boolean addToGoggleTooltip(List<Component> tooltip) {
        if (!structureManager.isAssembled()) {
            return false;
        }

        tooltipBuilder.addToGoggleTooltip(tooltip);
        return true;
    }

    public void tick(AirtightTankBlockEntity controller) {
        Level level = controller.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        structureManager.tick(controller);
        if (structureManager.isEvaluationRequired()) {
            flushDirtyState(controller);
            return;
        }

        boolean isActive = structureManager.isActive();
        updateActiveState(isActive);
        if (isActive) {
            consumeBufferedGas(controller.getTankInventory());
            flowMeter.tick(level);
            residueManager.tick(level);
        }

        flushDirtyState(controller);
    }

    public AirtightAssemblyDriverFlowMeter getFlowMeter() {
        return flowMeter;
    }

    public AirtightAssemblyDriverStructureManager getStructureManager() {
        return structureManager;
    }

    public AirtightAssemblyDriverLevelCalculator getLevelCalculator() {
        return levelCalculator;
    }

    public AirtightAssemblyDriverResidueManager getResidueManager() {
        return residueManager;
    }

    public IGasHandler getGasHandler() {
        return gasHandler;
    }

    public void markForSave() {
        saveDirty = true;
    }

    public void markForClientSync() {
        clientDirty = true;
    }

    public void markForSaveAndClientSync() {
        saveDirty = true;
        clientDirty = true;
    }

    public void reset() {
        activeState = false;
        activeStateInitialized = true;
        flowMeter.reset(true);
        structureManager.reset();
        levelCalculator.reset();
        residueManager.reset();
    }

    public CompoundTag write(Provider provider, boolean clientPacket) {
        CompoundTag tag = new CompoundTag();
        tag.put(COMPOUND_KEY_FLOW_METER, flowMeter.write(provider, clientPacket));
        tag.put(COMPOUND_KEY_LEVEL_CALCULATOR, levelCalculator.write(clientPacket));
        if (clientPacket) {
            tag.put(COMPOUND_KEY_STRUCTURE_MANAGER, structureManager.writeClient());
        }
        else {
            tag.put(COMPOUND_KEY_RESIDUE_MANAGER, residueManager.writePersistent());
        }
        return tag;
    }

    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            readClient(compoundTag, provider);
        }
        else {
            readPersistent(compoundTag, provider);
        }

        saveDirty = false;
        clientDirty = false;
    }

    private void readClient(CompoundTag compoundTag, Provider provider) {
        if (compoundTag.contains(COMPOUND_KEY_FLOW_METER)) {
            flowMeter.read(compoundTag.getCompound(COMPOUND_KEY_FLOW_METER), provider, true);
        }
        if (compoundTag.contains(COMPOUND_KEY_STRUCTURE_MANAGER)) {
            structureManager.readClient(compoundTag.getCompound(COMPOUND_KEY_STRUCTURE_MANAGER));
        }
        if (!compoundTag.contains(COMPOUND_KEY_LEVEL_CALCULATOR)) {
            return;
        }

        levelCalculator.read(compoundTag.getCompound(COMPOUND_KEY_LEVEL_CALCULATOR), true);
    }

    private void readPersistent(CompoundTag compoundTag, Provider provider) {
        CompoundTag levelTag = compoundTag.contains(COMPOUND_KEY_LEVEL_CALCULATOR) ? compoundTag.getCompound(COMPOUND_KEY_LEVEL_CALCULATOR) : new CompoundTag();
        levelCalculator.read(levelTag, false);
        if (compoundTag.contains(COMPOUND_KEY_FLOW_METER)) {
            flowMeter.read(compoundTag.getCompound(COMPOUND_KEY_FLOW_METER), provider, false);
        }
        else {
            flowMeter.loadEmptyState();
        }
        if (compoundTag.contains(COMPOUND_KEY_RESIDUE_MANAGER)) {
            residueManager.readPersistent(compoundTag.getCompound(COMPOUND_KEY_RESIDUE_MANAGER));
        }
        else {
            residueManager.loadEmptyPersistentState();
        }

        structureManager.invalidateForServerLoad();
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

        flowMeter.reset(true);
    }

    private void consumeBufferedGas(GasTank buffer) {
        GasStack storedGas = buffer.getGasStack();
        if (storedGas.isEmpty()) {
            return;
        }

        IGasHandler handler = getGasHandler();
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

    public class AirtightEngineGasHandler implements IGasHandler {
        @Override
        public boolean isGasValid(int tank, GasStack gasStack) {
            if (gasStack.isEmpty()) {
                return false;
            }

            AirtightEngineHandler handler = AirtightEngineHandlerUtils.of(gasStack);
            double workFactor = handler.getWorkFactor();
            return Double.isFinite(workFactor) && workFactor > 0 && handler.getMaxLevel() > 0;
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return GasStack.EMPTY;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return isGasValid(0, resource) ? flowMeter.fill(resource, action) : 0;
        }

        @Override
        public long getTankCapacity(int tank) {
            return CCBConfig.server().airtights.maxAirtightTankCapacityPerBlock.get() * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
        }
    }
}
