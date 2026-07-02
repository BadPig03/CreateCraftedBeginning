package net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightAssemblyDriverCore {
    public static final int MAX_LEVEL = 8;

    private static final String COMPOUND_KEY_FLOW_METER = "FlowMeter";
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";
    private static final String COMPOUND_KEY_LEVEL_CALCULATOR = "LevelCalculator";
    private static final String COMPOUND_KEY_RESIDUE_MANAGER = "ResidueManager";

    private final AirtightAssemblyDriverFlowMeter flowMeter;
    private final AirtightAssemblyDriverLevelCalculator levelCalculator;
    private final AirtightAssemblyDriverResidueManager residueManager;
    private final AirtightAssemblyDriverStructureManager structureManager;
    private final AirtightAssemblyDriverTooltipBuilder tooltipBuilder;

    private boolean dirty;

    public AirtightAssemblyDriverCore() {
        flowMeter = new AirtightAssemblyDriverFlowMeter(this);
        residueManager = new AirtightAssemblyDriverResidueManager(this);
        structureManager = new AirtightAssemblyDriverStructureManager(this);
        tooltipBuilder = new AirtightAssemblyDriverTooltipBuilder(this);
        levelCalculator = new AirtightAssemblyDriverLevelCalculator(this);
    }

    public boolean addToGoggleTooltip(List<Component> tooltip) {
        if (!structureManager.isActive()) {
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

        if (structureManager.isActive()) {
            flowMeter.tick(level);
            residueManager.tick(level);
            levelCalculator.update(true);
        }
        if (!dirty) {
            return;
        }

        controller.notifyUpdate();
        dirty = false;
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

    public IGasHandler createGasHandler() {
        return new AirtightEngineGasHandler();
    }

    public void markDirty() {
        dirty = true;
    }

    public void reset() {
        flowMeter.reset(true);
        structureManager.reset();
        levelCalculator.reset();
        residueManager.reset();
    }

    public CompoundTag write(Provider provider, boolean clientPacket) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_FLOW_METER, flowMeter.write(provider));
        compoundTag.put(COMPOUND_KEY_STRUCTURE_MANAGER, structureManager.write());
        compoundTag.put(COMPOUND_KEY_LEVEL_CALCULATOR, levelCalculator.write());
        if (!clientPacket) {
            compoundTag.put(COMPOUND_KEY_RESIDUE_MANAGER, residueManager.write());
        }
        return compoundTag;
    }

    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (compoundTag.contains(COMPOUND_KEY_FLOW_METER)) {
            flowMeter.read(compoundTag.getCompound(COMPOUND_KEY_FLOW_METER), provider);
        }
        if (compoundTag.contains(COMPOUND_KEY_STRUCTURE_MANAGER)) {
            structureManager.read(compoundTag.getCompound(COMPOUND_KEY_STRUCTURE_MANAGER));
        }
        if (compoundTag.contains(COMPOUND_KEY_LEVEL_CALCULATOR)) {
            levelCalculator.read(compoundTag.getCompound(COMPOUND_KEY_LEVEL_CALCULATOR));
        }
        if (compoundTag.contains(COMPOUND_KEY_RESIDUE_MANAGER) && !clientPacket) {
            residueManager.read(compoundTag.getCompound(COMPOUND_KEY_RESIDUE_MANAGER));
        }
        levelCalculator.update(false);
    }

    public class AirtightEngineGasHandler implements IGasHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return GasStack.EMPTY;
        }

        @Override
        public long getTankCapacity(int tank) {
            return CCBConfig.server().airtights.maxCanisterCapacity.get() * 100L;
        }

        @Override
        public boolean isGasValid(int tank, GasStack gasStack) {
            return !gasStack.isEmpty() && gasStack.getGasType().getEngineEfficiency() > 0;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return isGasValid(0, resource) ? flowMeter.fill(resource, action) : 0;
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return GasStack.EMPTY;
        }
    }
}