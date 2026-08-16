package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightAssemblyDriverCore {
    public static final int MAX_LEVEL = AirtightEngineHandler.MAX_LEVEL;

    private final AirtightAssemblyDriverFlowMeter flowMeter;
    private final AirtightAssemblyDriverLevelCalculator levelCalculator;
    private final AirtightAssemblyDriverResidueManager residueManager;
    private final AirtightAssemblyDriverStructureManager structureManager;
    private final AirtightAssemblyDriverTooltipBuilder tooltipBuilder;
    private final AirtightAssemblyDriverController controller;
    private final AirtightAssemblyDriverSerialization serialization;
    private final IGasHandler gasHandler;

    public AirtightAssemblyDriverCore() {
        flowMeter = new AirtightAssemblyDriverFlowMeter(this);
        residueManager = new AirtightAssemblyDriverResidueManager(this);
        structureManager = new AirtightAssemblyDriverStructureManager(this);
        tooltipBuilder = new AirtightAssemblyDriverTooltipBuilder(this);
        levelCalculator = new AirtightAssemblyDriverLevelCalculator(this);
        gasHandler = new AirtightAssemblyDriverGasHandler(flowMeter);
        controller = new AirtightAssemblyDriverController(this);
        serialization = new AirtightAssemblyDriverSerialization(this);
    }

    public boolean addToGoggleTooltip(List<Component> tooltip) {
        if (!structureManager.isAssembled()) {
            return false;
        }

        tooltipBuilder.addToGoggleTooltip(tooltip);
        return true;
    }

    public void tick(AirtightTankBlockEntity tankController) {
        controller.tick(tankController);
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

    public void requestStructureEvaluation() {
        structureManager.requestEvaluation();
    }

    public boolean isActive() {
        return structureManager.isActive();
    }

    public int getCurrentLevel() {
        return levelCalculator.getCurrentLevel();
    }

    public int getAttachedEngines() {
        return structureManager.getAttachedEngines();
    }

    public void markForSave() {
        controller.markForSave();
    }

    public void markForClientSync() {
        controller.markForClientSync();
    }

    public void markForSaveAndClientSync() {
        controller.markForSaveAndClientSync();
    }

    public void reset() {
        controller.reset();
    }

    public CompoundTag write(Provider provider, boolean clientPacket) {
        return serialization.write(provider, clientPacket);
    }

    public void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        serialization.read(tag, provider, clientPacket);
    }

    public AirtightAssemblyDriverController getController() {
        return controller;
    }
}
