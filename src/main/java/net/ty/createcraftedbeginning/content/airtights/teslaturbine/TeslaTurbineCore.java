package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlerUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineCore {
    private static final String COMPOUND_KEY_FLOW_METER = "FlowMeter";
    private static final String COMPOUND_KEY_LEVEL_CALCULATOR = "LevelCalculator";
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";

    private final IGasHandler clockwiseHandler;
    private final IGasHandler counterClockwiseHandler;
    private final TeslaTurbineBlockEntity turbine;
    private final TeslaTurbineStructureManager structureManager;
    private final TeslaTurbineTooltipBuilder tooltipBuilder;
    private final TeslaTurbineLevelCalculator levelCalculator;
    private final TeslaTurbineFlowMeter flowMeter;

    private boolean saveDirty;
    private boolean clientDirty;

    public TeslaTurbineCore(TeslaTurbineBlockEntity turbine) {
        this.turbine = turbine;
        structureManager = new TeslaTurbineStructureManager(this, turbine);
        levelCalculator = new TeslaTurbineLevelCalculator(this, turbine);
        flowMeter = new TeslaTurbineFlowMeter(this, turbine);
        tooltipBuilder = new TeslaTurbineTooltipBuilder(this);
        clockwiseHandler = new TeslaTurbineGasHandler(true);
        counterClockwiseHandler = new TeslaTurbineGasHandler(false);
    }

    private static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key) ? tag.getCompound(key) : new CompoundTag();
    }

    public void tick() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        flowMeter.tick();
        flushDirtyState();
    }

    public void lazyTick() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        structureManager.tick();
    }

    public boolean addToGoggleTooltip(List<Component> tooltip) {
        if (!structureManager.isActive()) {
            return false;
        }

        tooltipBuilder.addToGoggleTooltip(tooltip);
        return true;
    }

    public TeslaTurbineStructureManager getStructureManager() {
        return structureManager;
    }

    public TeslaTurbineLevelCalculator getLevelCalculator() {
        return levelCalculator;
    }

    public TeslaTurbineFlowMeter getFlowMeter() {
        return flowMeter;
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

    public CompoundTag write(Provider provider, boolean clientPacket) {
        CompoundTag tag = new CompoundTag();
        tag.put(COMPOUND_KEY_FLOW_METER, flowMeter.write(provider, clientPacket));
        if (!clientPacket) {
            return tag;
        }

        tag.put(COMPOUND_KEY_LEVEL_CALCULATOR, levelCalculator.write(true));
        tag.put(COMPOUND_KEY_STRUCTURE_MANAGER, structureManager.writeClient());
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

    public IGasHandler createGasHandler(boolean clockwise) {
        return clockwise ? clockwiseHandler : counterClockwiseHandler;
    }

    public TeslaTurbineBlockEntity getTurbine() {
        return turbine;
    }

    private void readClient(CompoundTag tag, Provider provider) {
        flowMeter.read(getCompoundOrEmpty(tag, COMPOUND_KEY_FLOW_METER), provider, true);
        levelCalculator.read(getCompoundOrEmpty(tag, COMPOUND_KEY_LEVEL_CALCULATOR), true);
        structureManager.readClient(getCompoundOrEmpty(tag, COMPOUND_KEY_STRUCTURE_MANAGER));
    }

    private void readPersistent(CompoundTag compoundTag, Provider provider) {
        levelCalculator.read(new CompoundTag(), false);
        if (compoundTag.contains(COMPOUND_KEY_FLOW_METER)) {
            flowMeter.read(compoundTag.getCompound(COMPOUND_KEY_FLOW_METER), provider, false);
        }
        else {
            flowMeter.loadEmptyState();
        }
        structureManager.invalidateForServerLoad();
    }

    private void flushDirtyState() {
        if (saveDirty) {
            turbine.setChanged();
            saveDirty = false;
        }
        if (!clientDirty) {
            return;
        }

        turbine.sendData();
        clientDirty = false;
    }

    private class TeslaTurbineGasHandler implements IGasHandler {
        private final boolean clockwise;

        private TeslaTurbineGasHandler(boolean clockwise) {
            this.clockwise = clockwise;
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return !stack.isEmpty() && AirtightTurbineHandlerUtils.of(stack).getMaxLevel() > 0;
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
            return isGasValid(0, resource) ? flowMeter.fill(resource, action, clockwise) : 0;
        }

        @Override
        public long getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }
    }
}
