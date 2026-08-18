package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorProcessing.CompressionPlan;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirCompressorController {
    private final OperatingStateUpdater operatingStateUpdater;

    private boolean queuedPressurization;

    AirCompressorController(OperatingStateUpdater operatingStateUpdater) {
        this.operatingStateUpdater = operatingStateUpdater;
    }

    void queuePressurization() {
        queuedPressurization = true;
    }

    ServerTickResult tickServer(Level level, AirCompressorState compressorState, boolean overStressed, float speed, SmartGasTankBehaviour inputTankBehaviour, SmartGasTankBehaviour outputTankBehaviour) {
        boolean shouldPressurize = queuedPressurization;
        queuedPressurization = false;

        int previousStoredHeat = compressorState.getStoredHeat();
        OverheatState previousOverheatState = compressorState.getOverheatState();
        if (previousOverheatState == OverheatState.MELTDOWN) {
            operatingStateUpdater.update(level, false);
            return new ServerTickResult(true, false, false, previousStoredHeat, previousOverheatState);
        }

        CompressionPlan plan = AirCompressorProcessing.createPlan(level, inputTankBehaviour.getPrimaryHandler().getGasStack());
        boolean operating = AirCompressorProcessing.canOperate(plan, overStressed, speed, previousOverheatState, inputTankBehaviour, outputTankBehaviour);
        boolean operatingForHeat = operating;
        if (shouldPressurize && operating) {
            compressorState.setWorkState(AirCompressorProcessing.pressurize(compressorState.getWorkState(), plan, inputTankBehaviour, outputTankBehaviour));
            operating = AirCompressorProcessing.canOperate(plan, false, speed, previousOverheatState, inputTankBehaviour, outputTankBehaviour);
        }

        operatingStateUpdater.update(level, operating);
        if (operating) {
            compressorState.setWorkState(AirCompressorProcessing.accumulateWork(compressorState.getWorkState(), plan, speed, previousOverheatState));
        }

        boolean closeCall = AirCompressorThermal.isMeltdownPreventedByCoolant(previousStoredHeat, speed, operatingForHeat, compressorState.getCoolantEfficiency(), level);
        compressorState.setStoredHeat(AirCompressorThermal.updateStoredHeat(previousStoredHeat, speed, operatingForHeat, compressorState.getCoolantEfficiency(), level));
        OverheatState updatedOverheatState = compressorState.getOverheatState();
        boolean enteredMeltdown = updatedOverheatState == OverheatState.MELTDOWN;
        return new ServerTickResult(false, closeCall, enteredMeltdown, previousStoredHeat, previousOverheatState);
    }

    @FunctionalInterface
    interface OperatingStateUpdater {
        void update(Level level, boolean operating);
    }

    record ServerTickResult(boolean initiallyMeltdown, boolean closeCall, boolean enteredMeltdown, int previousStoredHeat, OverheatState previousOverheatState) {
        boolean overheatStateChanged(AirCompressorState compressorState) {
            return previousOverheatState != compressorState.getOverheatState();
        }
    }
}
