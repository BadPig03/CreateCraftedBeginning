package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

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
    private final GasPressureBoundary gasHandler;

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

    public static double getStressCapacityPerGasUnit(double workFactor) {
        if (!GasConsumptionMath.isFinite(workFactor) || workFactor <= 0) {
            return 0;
        }

        double engineCapacity = BlockStressValues.getCapacity(CCBBlocks.AIRTIGHT_ENGINE_BLOCK.get());
        if (!GasConsumptionMath.isFinite(engineCapacity) || engineCapacity <= 0) {
            return 0;
        }

        return AirtightEngineBlockEntity.BASE_ROTATION_SPEED * engineCapacity * workFactor / AirtightAssemblyDriverFlowMeter.SUPPLY_PER_LEVEL;
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

    public void requestStructureEvaluation() {
        structureManager.requestEvaluation();
    }

    public boolean isActive() {
        return structureManager.isActive();
    }

    public boolean isUsingSteam() {
        return flowMeter.getGasType().is(CCBGases.STEAM);
    }

    public int getCurrentLevel() {
        return levelCalculator.getCurrentLevel();
    }

    public int getAttachedEngines() {
        return structureManager.getAttachedEngines();
    }

    public void reset() {
        controller.reset();
    }

    public CompoundTag write(Provider provider, boolean clientPacket) {
        return serialization.write(provider, clientPacket);
    }

    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        serialization.read(compoundTag, provider, clientPacket);
    }

    AirtightAssemblyDriverFlowMeter getFlowMeter() {
        return flowMeter;
    }

    AirtightAssemblyDriverStructureManager getStructureManager() {
        return structureManager;
    }

    AirtightAssemblyDriverLevelCalculator getLevelCalculator() {
        return levelCalculator;
    }

    AirtightAssemblyDriverResidueManager getResidueManager() {
        return residueManager;
    }

    AirtightAssemblyDriverController getController() {
        return controller;
    }

    GasPressureBoundary getGasHandler() {
        return gasHandler;
    }

    void markForSave() {
        controller.markForSave();
    }

    void markForClientSync() {
        controller.markForClientSync();
    }

    void markForSaveAndClientSync() {
        controller.markForSaveAndClientSync();
    }
}
