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

    ServerTickResult tickServer(Level level, AirCompressorState state, boolean overStressed, float speed, SmartGasTankBehaviour inputTankBehaviour, SmartGasTankBehaviour outputTankBehaviour) {
        boolean shouldPressurize = queuedPressurization;
        queuedPressurization = false;

        int previousStoredHeat = state.getStoredHeat();
        OverheatState previousState = state.getOverheatState();
        if (previousState == OverheatState.MELTDOWN) {
            operatingStateUpdater.update(level, false);
            return new ServerTickResult(true, false, false, previousStoredHeat, previousState);
        }

        CompressionPlan plan = AirCompressorProcessing.createPlan(level, inputTankBehaviour.getPrimaryHandler().getGasStack());
        boolean operating = AirCompressorProcessing.canOperate(plan, overStressed, speed, previousState, inputTankBehaviour, outputTankBehaviour);
        if (shouldPressurize && operating) {
            state.setWorkState(AirCompressorProcessing.pressurize(state.getWorkState(), plan, inputTankBehaviour, outputTankBehaviour));
            operating = AirCompressorProcessing.canOperate(plan, false, speed, previousState, inputTankBehaviour, outputTankBehaviour);
        }

        operatingStateUpdater.update(level, operating);
        if (operating) {
            state.setWorkState(AirCompressorProcessing.accumulateWork(state.getWorkState(), plan, speed, previousState));
        }

        state.setStoredHeat(AirCompressorThermal.updateStoredHeat(previousStoredHeat, speed, operating, state.getCoolantEfficiency(), level));
        OverheatState newState = state.getOverheatState();
        boolean closeCall = previousState == OverheatState.SEVERE && newState.ordinal() < OverheatState.SEVERE.ordinal();
        boolean enteredMeltdown = newState == OverheatState.MELTDOWN;
        return new ServerTickResult(false, closeCall, enteredMeltdown, previousStoredHeat, previousState);
    }

    @FunctionalInterface
    interface OperatingStateUpdater {
        void update(Level level, boolean operating);
    }

    record ServerTickResult(boolean initiallyMeltdown, boolean closeCall, boolean enteredMeltdown, int previousStoredHeat, OverheatState previousState) {
        boolean overheatStateChanged(AirCompressorState state) {
            return previousState != state.getOverheatState();
        }
    }
}
