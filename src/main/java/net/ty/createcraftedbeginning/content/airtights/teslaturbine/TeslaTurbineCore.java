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
class TeslaTurbineCore {
    private final TeslaTurbineBlockEntity turbine;
    private final TeslaTurbineStructureManager structureManager;
    private final TeslaTurbineTooltipBuilder tooltipBuilder;
    private final TeslaTurbineLevelCalculator levelCalculator;
    private final TeslaTurbineFlowMeter flowMeter;
    private final TeslaTurbineController controller;
    private final TeslaTurbineSerialization serialization;
    private final IGasHandler clockwiseHandler;
    private final IGasHandler counterClockwiseHandler;

    TeslaTurbineCore(TeslaTurbineBlockEntity turbine) {
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

    void tick() {
        controller.tick();
    }

    void lazyTick() {
        controller.lazyTick();
    }

    void initialize() {
        controller.initialize();
    }

    void onSpeedChanged() {
        controller.onSpeedChanged();
    }

    float getGeneratedSpeed() {
        return controller.getGeneratedSpeed();
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        if (!structureManager.isActive()) {
            return false;
        }

        tooltipBuilder.addToGoggleTooltip(tooltip);
        return true;
    }

    TeslaTurbineStructureManager getStructureManager() {
        return structureManager;
    }

    TeslaTurbineLevelCalculator getLevelCalculator() {
        return levelCalculator;
    }

    TeslaTurbineFlowMeter getFlowMeter() {
        return flowMeter;
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

    CompoundTag write(Provider provider, boolean clientPacket) {
        return serialization.write(provider, clientPacket);
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        serialization.read(compoundTag, provider, clientPacket);
    }

    IGasHandler createGasHandler(boolean clockwise) {
        if (!clockwise) {
            return counterClockwiseHandler;
        }
        return clockwiseHandler;
    }

    TeslaTurbineBlockEntity getTurbine() {
        return turbine;
    }

    TeslaTurbineController getController() {
        return controller;
    }
}
