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
    static CompressionPlan createPlan(Level level, GasStack input) {
        if (input.isEmpty()) {
            return null;
        }

        PressurizationRecipe recipe = PressurizationRecipe.findRecipe(level, input).orElse(null);
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

        GasStack input = inputTankBehaviour.getPrimaryHandler().getGasStack();
        if (input.getAmount() < plan.inputPerBatch()) {
            return false;
        }

        GasStack output = outputTankBehaviour.getPrimaryHandler().getGasStack();
        boolean hasCompatibleOutput = output.isEmpty() || GasStack.isSameGasSameComponents(output, plan.outputPerBatch());
        return hasCompatibleOutput && outputTankBehaviour.getPrimaryHandler().getSpace() >= plan.outputPerBatch().getAmount();
    }

    static WorkState accumulateWork(WorkState workState, CompressionPlan plan, float speed, OverheatState overheatState) {
        long accumulatedWork = workState.recipe() == plan.recipe() ? workState.accumulatedWork() : 0;
        float scaledWork = Mth.abs(speed) * getPressurizationRateMultiplier() * overheatState.getEfficiencyPercent();
        long addedWork = Math.max(0, Mth.floor(scaledWork));
        long updatedWork = addedWork >= Long.MAX_VALUE - accumulatedWork ? Long.MAX_VALUE : accumulatedWork + addedWork;
        return new WorkState(plan.recipe(), updatedWork);
    }

    static WorkState pressurize(WorkState workState, CompressionPlan plan, SmartGasTankBehaviour inputTankBehaviour, SmartGasTankBehaviour outputTankBehaviour) {
        long accumulatedWork = workState.recipe() == plan.recipe() ? workState.accumulatedWork() : 0;
        long batchesByWork = accumulatedWork / 100 / plan.inputPerBatch();
        long batchesByInput = inputTankBehaviour.getPrimaryHandler().getGasAmount() / plan.inputPerBatch();
        long batchesByOutput = outputTankBehaviour.getPrimaryHandler().getSpace() / plan.outputPerBatch().getAmount();
        long batches = Math.min(batchesByWork, Math.min(batchesByInput, batchesByOutput));
        if (batches <= 0) {
            return new WorkState(plan.recipe(), accumulatedWork);
        }

        long totalInput = batches * plan.inputPerBatch();
        long totalOutput = batches * plan.outputPerBatch().getAmount();
        GasStack outputStack = plan.outputPerBatch().copyWithAmount(totalOutput);
        ResourceTransaction transaction = new ResourceTransaction().add(ResourceTransaction.participant(() -> {
            GasStack simulatedDrain = inputTankBehaviour.getInternalGasHandler().forceDrain(totalInput, GasAction.SIMULATE);
            long simulatedFill = outputTankBehaviour.getInternalGasHandler().forceFill(outputStack, GasAction.SIMULATE);
            return simulatedDrain.getAmount() == totalInput && plan.recipe().getGasIngredient().ingredient().test(simulatedDrain) && simulatedFill == totalOutput;
        }, () -> MachineResourceSnapshots.snapshotGasContents(inputTankBehaviour, outputTankBehaviour), () -> {
            GasStack drained = inputTankBehaviour.getInternalGasHandler().forceDrain(totalInput, GasAction.EXECUTE);
            return drained.getAmount() == totalInput && plan.recipe().getGasIngredient().ingredient().test(drained) && outputTankBehaviour.getInternalGasHandler().forceFill(outputStack, GasAction.EXECUTE) == totalOutput;
        }, snapshot -> MachineResourceSnapshots.restoreGasContents(snapshot, inputTankBehaviour, outputTankBehaviour)));
        if (!transaction.commit()) {
            return new WorkState(plan.recipe(), accumulatedWork);
        }

        long remainingWork = accumulatedWork - totalInput * 100;
        return new WorkState(plan.recipe(), remainingWork);
    }

    private static float getPressurizationRateMultiplier() {
        return CCBConfig.server().airtights.pressurizationRateMultiplier.getF();
    }

    record CompressionPlan(PressurizationRecipe recipe, long inputPerBatch, GasStack outputPerBatch) {}
}
