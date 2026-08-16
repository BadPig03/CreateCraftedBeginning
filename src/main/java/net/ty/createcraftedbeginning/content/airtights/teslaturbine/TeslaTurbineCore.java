package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineCore {
    private final TeslaTurbineBlockEntity turbine;
    private final TeslaTurbineStructureManager structureManager;
    private final TeslaTurbineTooltipBuilder tooltipBuilder;
    private final TeslaTurbineLevelCalculator levelCalculator;
    private final TeslaTurbineFlowMeter flowMeter;
    private final TeslaTurbineController controller;
    private final TeslaTurbineSerialization serialization;
    private final IGasHandler clockwiseHandler;
    private final IGasHandler counterClockwiseHandler;

    public TeslaTurbineCore(TeslaTurbineBlockEntity turbine) {
        this.turbine = turbine;
        structureManager = new TeslaTurbineStructureManager(this, turbine);
        levelCalculator = new TeslaTurbineLevelCalculator(this, turbine);
        flowMeter = new TeslaTurbineFlowMeter(this, turbine);
        tooltipBuilder = new TeslaTurbineTooltipBuilder(this);
        controller = new TeslaTurbineController(this, turbine);
        serialization = new TeslaTurbineSerialization(this);
        clockwiseHandler = new TeslaTurbineGasHandler(flowMeter, true);
        counterClockwiseHandler = new TeslaTurbineGasHandler(flowMeter, false);
    }

    public void tick() {
        controller.tick();
    }

    public void lazyTick() {
        controller.lazyTick();
    }

    public void initialize() {
        controller.initialize();
    }

    public void onSpeedChanged() {
        controller.onSpeedChanged();
    }

    public float getGeneratedSpeed() {
        return controller.getGeneratedSpeed();
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
        controller.markForSave();
    }

    public void markForClientSync() {
        controller.markForClientSync();
    }

    public void markForSaveAndClientSync() {
        controller.markForSaveAndClientSync();
    }

    public CompoundTag write(Provider provider, boolean clientPacket) {
        return serialization.write(provider, clientPacket);
    }

    public void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        serialization.read(tag, provider, clientPacket);
    }

    public IGasHandler createGasHandler(boolean clockwise) {
        return clockwise ? clockwiseHandler : counterClockwiseHandler;
    }

    public TeslaTurbineBlockEntity getTurbine() {
        return turbine;
    }

    public TeslaTurbineController getController() {
        return controller;
    }
}
