package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.ChamberGasTank;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberConversionPlanner.GasConversionPlan;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberRecipeIndex.GasConversion;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.foundation.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeChamberGasProcessor {
    private static final int GAS_PROCESSING_INTERVAL = 20;
    private static final String PENDING_WORK = "PendingWork";
    private static final String PENDING_TICKS = "PendingTicks";
    private static final String PENDING_CHARGER_TYPE = "PendingChargerType";
    private static final String PROCESSING_CREDIT = "ProcessingCredit";

    private final BreezeChamberBlockEntity chamber;
    private final BreezeChamberConversionPlanner planner;

    private long pendingProcessingWork;
    private int pendingProcessingTicks;
    private long processingCredit;
    private ChargerType pendingChargerType = ChargerType.NONE;

    BreezeChamberGasProcessor(BreezeChamberBlockEntity chamber) {
        this.chamber = chamber;
        planner = new BreezeChamberConversionPlanner(chamber);
    }

    boolean isControllerActive() {
        ChamberGasTank chamberTank = getTank();
        return chamberTank != null && isControllerActive(chamberTank);
    }

    Gas getTankGasType() {
        return getTankGasStack().getGasType();
    }

    boolean isOutputBlocked() {
        ChargerType chargerType = chargerTypeFor(chamber.getWindLevel());
        if (chargerType == ChargerType.NONE) {
            return false;
        }

        ChamberGasTank chamberTank = getTank();
        if (chamberTank == null || isControllerActive(chamberTank)) {
            return false;
        }

        GasPressureCompartment inputTank = chamberTank.getTankInventory().getPressureCompartment(0);
        if (inputTank.getStoredAmount() <= 0) {
            return false;
        }

        GasStack inputStack = inputTank.getGasStack();
        GasTank outputTank = outputTank();
        boolean hasReadyInput = false;
        for (GasConversion conversion : planner.getConversions(chargerType, inputStack)) {
            if (GasConsumptionPlanner.plan(conversion.input(), inputTank).isEmpty()) {
                continue;
            }

            hasReadyInput = true;
            if (!BreezeChamberConversionPlanner.canAcceptOutputPhysically(outputTank, conversion.output())) {
                continue;
            }

            return false;
        }

        return hasReadyInput;
    }

    boolean isInputInvalid() {
        GasStack inputStack = getTankGasStack();
        if (inputStack.isEmpty()) {
            return false;
        }

        ChargerType chargerType = chargerTypeFor(chamber.getWindLevel());
        return chargerType != ChargerType.NONE && planner.getConversions(chargerType, inputStack).isEmpty();
    }

    void tickGasProcessing(ChargerType chargerType, int windTime) {
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide || chargerType == ChargerType.NONE || windTime == 0) {
            return;
        }

        ChamberGasTank chamberTank = getTank();
        if (chamberTank == null || isControllerActive(chamberTank)) {
            discardProcessingProgress();
            return;
        }

        if (pendingProcessingTicks > 0 && pendingChargerType != chargerType) {
            flushPendingProcessing();
            processingCredit = 0;
        }
        pendingChargerType = chargerType;
        pendingProcessingWork += planner.getProcessingAmount(windTime);
        pendingProcessingTicks++;
        if (pendingProcessingTicks < GAS_PROCESSING_INTERVAL) {
            return;
        }

        flushPendingProcessing();
    }

    void flushPendingProcessing() {
        Level level = chamber.getLevel();
        if (pendingProcessingTicks <= 0 || pendingChargerType == ChargerType.NONE) {
            resetPendingProcessing();
            return;
        }

        ChargerType chargerType = pendingChargerType;
        long generatedBudget = pendingProcessingWork / GAS_PROCESSING_INTERVAL;
        resetPendingProcessing();
        if (level == null || level.isClientSide) {
            return;
        }

        ChamberGasTank chamberTank = getTank();
        if (chamberTank == null || isControllerActive(chamberTank)) {
            processingCredit = 0;
            return;
        }

        if (generatedBudget <= 0 && processingCredit <= 0) {
            return;
        }

        processGas(chargerType, chamberTank, generatedBudget);
    }

    void writePendingProcessing(CompoundTag compoundTag) {
        if (processingCredit > 0) {
            compoundTag.putLong(PROCESSING_CREDIT, processingCredit);
        }
        if (pendingProcessingTicks <= 0 || pendingChargerType == ChargerType.NONE) {
            return;
        }

        compoundTag.putLong(PENDING_WORK, pendingProcessingWork);
        compoundTag.putInt(PENDING_TICKS, pendingProcessingTicks);
        compoundTag.putString(PENDING_CHARGER_TYPE, pendingChargerType.name());
    }

    void readPendingProcessing(CompoundTag compoundTag) {
        discardProcessingProgress();
        ChargerType stateChargerType = chamber.getChamberStateInternal().getChargerType();
        if (stateChargerType == ChargerType.NONE) {
            return;
        }

        processingCredit = Math.max(0, NbtValues.getLongOrDefault(compoundTag, PROCESSING_CREDIT, 0));
        ChargerType chargerType = ChargerType.fromTag(compoundTag, PENDING_CHARGER_TYPE);
        if (chargerType == ChargerType.NONE || chargerType != stateChargerType) {
            return;
        }

        int pendingTicks = Mth.clamp(NbtValues.getIntOrDefault(compoundTag, PENDING_TICKS, 0), 0, GAS_PROCESSING_INTERVAL - 1);
        long pendingWork = Math.max(0, NbtValues.getLongOrDefault(compoundTag, PENDING_WORK, 0));
        if (pendingTicks <= 0 || pendingWork <= 0) {
            return;
        }

        long maxPendingWork = (long) Math.max(1, CCBConfig.server().machines.breezeChamber.maxProcessingPerSecond.get()) * pendingTicks;
        pendingProcessingTicks = pendingTicks;
        pendingProcessingWork = Math.min(pendingWork, maxPendingWork);
        pendingChargerType = chargerType;
    }

    void discardProcessingProgress() {
        resetPendingProcessing();
        processingCredit = 0;
    }

    private static boolean isControllerActive(ChamberGasTank chamberTank) {
        return chamberTank instanceof AirtightTankBlockEntity tankController && tankController.getCore().isActive();
    }

    private static ChargerType chargerTypeFor(WindLevel windLevel) {
        return switch (windLevel) {
            case GALE -> ChargerType.NORMAL;
            case ILL -> ChargerType.BAD;
            case CALM -> ChargerType.NONE;
        };
    }

    private void resetPendingProcessing() {
        pendingProcessingWork = 0;
        pendingProcessingTicks = 0;
        pendingChargerType = ChargerType.NONE;
    }

    private void processGas(ChargerType chargerType, ChamberGasTank chamberTank, long generatedBudget) {
        GasPressureCompartment inputTank = chamberTank.getTankInventory().getPressureCompartment(0);
        if (inputTank.getStoredAmount() <= 0) {
            processingCredit = 0;
            return;
        }

        GasStack inputStack = inputTank.getGasStack();
        GasTank outputTank = outputTank();
        Optional<GasConversion> target = planner.findProcessingTarget(chargerType, inputStack, inputTank, outputTank);
        if (target.isEmpty()) {
            processingCredit = 0;
            return;
        }

        GasConversion conversion = target.get();
        long processingBudget = BoundedMath.saturatedAdd(processingCredit, Math.max(0, generatedBudget));
        long inputAmount = conversion.input().amount();
        if (processingBudget < inputAmount) {
            processingCredit = processingBudget;
            return;
        }

        Optional<GasConversionPlan> executablePlan = planner.planConversion(conversion, inputTank, outputTank, processingBudget);
        if (executablePlan.isEmpty()) {
            processingCredit = Math.min(processingBudget, inputAmount - 1);
            return;
        }

        GasConversionPlan plan = executablePlan.get();
        GasStack outputPerBatch = plan.conversion().output();
        GasStack outputRequest = outputPerBatch.copyWithAmount(plan.batchCount() * outputPerBatch.getAmount());
        if (!executeGasConversionTransaction(inputTank, plan.inputPlan(), outputRequest)) {
            processingCredit = Math.min(processingBudget, inputAmount - 1);
            return;
        }

        long consumedBudget = plan.batchCount() * inputAmount;
        long remainingBudget = processingBudget - consumedBudget;
        processingCredit = planner.retainableProcessingCredit(chargerType, inputTank, outputTank, remainingBudget);
    }

    private boolean executeGasConversionTransaction(GasPressureCompartment sourceTank, GasConsumptionPlan inputPlan, GasStack outputRequest) {
        long[] plannedAmounts = inputPlan.tankAmounts();
        if (plannedAmounts.length != 1 || plannedAmounts[0] <= 0) {
            return false;
        }

        GasStack sourceGas = sourceTank.getGasStack();
        if (sourceGas.isEmpty()) {
            return false;
        }

        GasStack sourceSnapshot = sourceGas.copyWithAmount(plannedAmounts[0]);
        SmartGasTankBehaviour outputBehaviour = chamber.getTankBehaviourInternal();
        ResourceTransaction conversionTransaction = new ResourceTransaction().add(ResourceTransaction.participant(inputPlan::canExecute, sourceSnapshot::copy, inputPlan::execute, drainedSnapshot -> {
            long restoredAmount = sourceTank.restoreDrainedGas(drainedSnapshot, GasAction.EXECUTE);
            if (restoredAmount != drainedSnapshot.getAmount()) {
                throw new IllegalStateException("Failed to restore breeze chamber source gas: expected " + drainedSnapshot.getAmount() + " GU, restored " + restoredAmount + " GU.");
            }
        })).add(ResourceTransaction.participant(() -> outputBehaviour.getInternalGasHandler().forceFill(outputRequest, GasAction.SIMULATE) == outputRequest.getAmount(), () -> MachineResourceSnapshots.snapshotGasTanks(outputBehaviour), () -> outputBehaviour.getInternalGasHandler().forceFill(outputRequest, GasAction.EXECUTE) == outputRequest.getAmount(), outputSnapshot -> MachineResourceSnapshots.restoreGasTanks(outputSnapshot, outputBehaviour)));
        return conversionTransaction.commit();
    }

    private GasTank outputTank() {
        return chamber.getTankBehaviourInternal().getPrimaryHandler();
    }

    private GasStack getTankGasStack() {
        ChamberGasTank chamberTank = getTank();
        if (chamberTank == null) {
            return GasStack.EMPTY;
        }

        GasPressureCompartment inputTank = chamberTank.getTankInventory().getPressureCompartment(0);
        if (inputTank.getStoredAmount() <= 0) {
            return GasStack.EMPTY;
        }

        return inputTank.getGasStack();
    }

    private @Nullable ChamberGasTank getTank() {
        Level level = chamber.getLevel();
        if (level == null) {
            return null;
        }

        ChamberGasTank chamberTank = chamber.source.get();
        if (chamberTank != null && !chamberTank.isRemoved()) {
            return chamberTank.getControllerBE();
        }

        chamber.source = new WeakReference<>(null);
        chamberTank = level.getBlockEntity(chamber.getBlockPos().below()) instanceof ChamberGasTank tankBelow ? tankBelow : null;
        chamber.source = new WeakReference<>(chamberTank);
        if (chamberTank == null) {
            return null;
        }

        return chamberTank.getControllerBE();
    }
}
