package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorState.WorkState;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.content.airtights.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirCompressorProcessing {
    static final int LAZY_TICK_RATE = 5;

    private AirCompressorProcessing() {
    }

    static long getTankCapacity() {
        return CCBConfig.server().airtights.maxAirCompressorCapacity.get() * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    @Nullable
    static CompressionPlan createPlan(Level level, GasStack inputGas) {
        if (inputGas.isEmpty()) {
            return null;
        }

        PressurizationRecipe recipe = PressurizationRecipe.findRecipe(level, inputGas).orElse(null);
        if (recipe == null) {
            return null;
        }

        long inputPerBatch = recipe.getGasIngredient().amount();
        GasStack outputPerBatch = recipe.getGasResult().copy();
        if (inputPerBatch <= 0 || outputPerBatch.isEmpty() || outputPerBatch.getAmount() <= 0) {
            return null;
        }
        return new CompressionPlan(recipe, inputPerBatch, outputPerBatch);
    }

    static boolean canOperate(@Nullable CompressionPlan plan, boolean overStressed, float speed, OverheatState overheatState, SmartGasTankBehaviour inputTankBehaviour, SmartGasTankBehaviour outputTankBehaviour) {
        if (plan == null || overStressed) {
            return false;
        }

        if (Mth.abs(speed) < SpeedLevel.MEDIUM.getSpeedValue() || overheatState == OverheatState.MELTDOWN) {
            return false;
        }

        GasStack inputGas = inputTankBehaviour.getPrimaryHandler().getGasStack();
        if (inputGas.getAmount() < plan.inputPerBatch()) {
            return false;
        }

        GasStack outputGas = outputTankBehaviour.getPrimaryHandler().getGasStack();
        boolean hasCompatibleOutput = outputGas.isEmpty() || GasStack.isSameGasSameComponents(outputGas, plan.outputPerBatch());
        return hasCompatibleOutput && outputTankBehaviour.getPrimaryHandler().getSpace() >= plan.outputPerBatch().getAmount();
    }

    static WorkState accumulateWork(WorkState workState, CompressionPlan plan, float speed, OverheatState overheatState) {
        long accumulatedWork = workState.recipe() == plan.recipe() ? workState.accumulatedWork() : 0;
        float scaledWork = Mth.abs(speed) * getPressurizationRateMultiplier() * overheatState.getEfficiencyPercent();
        long workIncrement = Math.max(0, Mth.floor(scaledWork));
        long updatedAccumulatedWork = workIncrement >= Long.MAX_VALUE - accumulatedWork ? Long.MAX_VALUE : accumulatedWork + workIncrement;
        return new WorkState(plan.recipe(), updatedAccumulatedWork);
    }

    static WorkState pressurize(WorkState workState, CompressionPlan plan, SmartGasTankBehaviour inputTankBehaviour, SmartGasTankBehaviour outputTankBehaviour) {
        long accumulatedWork = workState.recipe() == plan.recipe() ? workState.accumulatedWork() : 0;
        long batchesByWork = accumulatedWork / 100 / plan.inputPerBatch();
        long batchesByInput = inputTankBehaviour.getPrimaryHandler().getGasAmount() / plan.inputPerBatch();
        long batchesByOutput = outputTankBehaviour.getPrimaryHandler().getSpace() / plan.outputPerBatch().getAmount();
        long batchCount = Math.min(batchesByWork, Math.min(batchesByInput, batchesByOutput));
        if (batchCount <= 0) {
            return new WorkState(plan.recipe(), accumulatedWork);
        }

        long totalInputAmount = batchCount * plan.inputPerBatch();
        long totalOutputAmount = batchCount * plan.outputPerBatch().getAmount();
        GasStack outputStack = plan.outputPerBatch().copyWithAmount(totalOutputAmount);
        ResourceTransaction transaction = new ResourceTransaction().add(ResourceTransaction.participant(() -> {
            GasStack simulatedDrain = inputTankBehaviour.getInternalGasHandler().forceDrain(totalInputAmount, GasAction.SIMULATE);
            long simulatedFillAmount = outputTankBehaviour.getInternalGasHandler().forceFill(outputStack, GasAction.SIMULATE);
            return simulatedDrain.getAmount() == totalInputAmount && plan.recipe().getGasIngredient().ingredient().test(simulatedDrain) && simulatedFillAmount == totalOutputAmount;
        }, () -> MachineResourceSnapshots.snapshotGasContents(inputTankBehaviour, outputTankBehaviour), () -> {
            GasStack drainedInputGas = inputTankBehaviour.getInternalGasHandler().forceDrain(totalInputAmount, GasAction.EXECUTE);
            return drainedInputGas.getAmount() == totalInputAmount && plan.recipe().getGasIngredient().ingredient().test(drainedInputGas) && outputTankBehaviour.getInternalGasHandler().forceFill(outputStack, GasAction.EXECUTE) == totalOutputAmount;
        }, snapshot -> MachineResourceSnapshots.restoreGasContents(snapshot, inputTankBehaviour, outputTankBehaviour)));
        if (!transaction.commit()) {
            return new WorkState(plan.recipe(), accumulatedWork);
        }

        long remainingWork = accumulatedWork - totalInputAmount * 100;
        return new WorkState(plan.recipe(), remainingWork);
    }

    private static float getPressurizationRateMultiplier() {
        return CCBConfig.server().airtights.pressurizationRateMultiplier.getF();
    }

    record CompressionPlan(PressurizationRecipe recipe, long inputPerBatch, GasStack outputPerBatch) {}
}
