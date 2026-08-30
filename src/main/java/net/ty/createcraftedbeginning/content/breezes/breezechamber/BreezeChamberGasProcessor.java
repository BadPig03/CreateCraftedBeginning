package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.IChamberGasTank;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.core.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberRecipeIndex.GasConversion;
import net.ty.createcraftedbeginning.core.ResourceTransaction;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeChamberGasProcessor {
    private static final int GAS_PROCESSING_INTERVAL = 20;
    private static final String PENDING_WORK = "PendingWork";
    private static final String PENDING_TICKS = "PendingTicks";
    private static final String PENDING_CHARGER_TYPE = "PendingChargerType";
    private final BreezeChamberBlockEntity chamber;
    private long pendingProcessingWork;
    private int pendingProcessingTicks;
    private ChargerType pendingChargerType = ChargerType.NONE;

    BreezeChamberGasProcessor(BreezeChamberBlockEntity chamber) {
        this.chamber = chamber;
    }

    private static boolean isControllerActive(IChamberGasTank chamberTank) {
        return chamberTank instanceof AirtightTankBlockEntity tankController && tankController.getCore().isActive();
    }

    private static int getProcessingAmount(int windTime) {
        if (windTime == 0) {
            return 0;
        }

        int maxProcessingRate = CCBConfig.server().airtights.maxProcessingRate.get();
        float windStrength = CCBMathUtils.clampUnit((float) Mth.abs(windTime) / BreezeChamberBlockEntity.getMaxEffectiveThreshold());
        return Mth.clamp((int) (maxProcessingRate * windStrength), 1, maxProcessingRate);
    }

    private static ChargerType chargerTypeFor(WindLevel windLevel) {
        return switch (windLevel) {
            case GALE -> ChargerType.NORMAL;
            case ILL -> ChargerType.BAD;
            case CALM -> ChargerType.NONE;
        };
    }

    boolean isControllerActive() {
        IChamberGasTank chamberTank = getTank();
        return chamberTank != null && isControllerActive(chamberTank);
    }

    Gas getTankGasType() {
        return getTankGasStack().getGasType();
    }

    boolean isOutputFull() {
        return outputTank().getSpace() == 0;
    }

    boolean isOutputMismatched() {
        GasStack inputStack = getTankGasStack();
        GasTank outputTank = outputTank();
        if (inputStack.isEmpty() || outputTank.isEmpty()) {
            return false;
        }

        ChargerType chargerType = chargerTypeFor(chamber.getWindLevel());
        List<GasConversion> conversions = getConversions(chargerType, inputStack);
        return !conversions.isEmpty() && conversions.stream().noneMatch(conversion -> GasStack.isSameGasSameComponents(outputTank.getGasStack(), conversion.output()));
    }

    boolean isInputInvalid() {
        GasStack inputStack = getTankGasStack();
        if (inputStack.isEmpty()) {
            return false;
        }

        ChargerType chargerType = chargerTypeFor(chamber.getWindLevel());
        return chargerType != ChargerType.NONE && getConversions(chargerType, inputStack).isEmpty();
    }

    void tickGasProcessing(ChargerType chargerType, int windTime) {
        if (chamber.getLevel() == null || chamber.getLevel().isClientSide || chargerType == ChargerType.NONE || windTime == 0) {
            return;
        }

        if (pendingProcessingTicks > 0 && pendingChargerType != chargerType) {
            flushPendingProcessing();
        }

        pendingChargerType = chargerType;
        pendingProcessingWork += getProcessingAmount(windTime);
        pendingProcessingTicks++;
        if (pendingProcessingTicks < GAS_PROCESSING_INTERVAL) {
            return;
        }

        flushPendingProcessing();
    }

    void flushPendingProcessing() {
        if (pendingProcessingTicks <= 0 || pendingChargerType == ChargerType.NONE) {
            resetPendingProcessing();
            return;
        }

        ChargerType chargerType = pendingChargerType;
        long processingBudget = pendingProcessingWork / GAS_PROCESSING_INTERVAL;
        resetPendingProcessing();
        if (processingBudget <= 0 || chamber.getLevel() == null || chamber.getLevel().isClientSide) {
            return;
        }

        IChamberGasTank chamberTank = getTank();
        if (chamberTank == null || isControllerActive(chamberTank)) {
            return;
        }

        processGas(chargerType, chamberTank, processingBudget);
    }

    void writePendingProcessing(CompoundTag compoundTag) {
        if (pendingProcessingTicks <= 0 || pendingChargerType == ChargerType.NONE) {
            return;
        }

        CCBNbtUtils.putLong(compoundTag, PENDING_WORK, pendingProcessingWork);
        CCBNbtUtils.putInt(compoundTag, PENDING_TICKS, pendingProcessingTicks);
        CCBNbtUtils.putString(compoundTag, PENDING_CHARGER_TYPE, pendingChargerType.name());
    }

    void readPendingProcessing(CompoundTag compoundTag) {
        resetPendingProcessing();
        ChargerType chargerType = ChargerType.fromTag(compoundTag, PENDING_CHARGER_TYPE);
        if (chargerType == ChargerType.NONE || chargerType != chamber.getChamberStateInternal().getChargerType()) {
            return;
        }

        int pendingTicks = CCBMathUtils.clampNonNegative(CCBNbtUtils.getIntOrDefault(compoundTag, PENDING_TICKS, 0), GAS_PROCESSING_INTERVAL - 1);
        long pendingWork = Math.max(0, CCBNbtUtils.getLongOrDefault(compoundTag, PENDING_WORK, 0));
        if (pendingTicks <= 0 || pendingWork <= 0) {
            return;
        }

        long maxPendingWork = (long) Math.max(1, CCBConfig.server().airtights.maxProcessingRate.get()) * pendingTicks;
        pendingProcessingTicks = pendingTicks;
        pendingProcessingWork = Math.min(pendingWork, maxPendingWork);
        pendingChargerType = chargerType;
    }

    private void resetPendingProcessing() {
        pendingProcessingWork = 0;
        pendingProcessingTicks = 0;
        pendingChargerType = ChargerType.NONE;
    }

    private void processGas(ChargerType chargerType, IChamberGasTank chamberTank, long processingBudget) {
        GasTank inputTank = chamberTank.getTankInventory();
        GasTank outputTank = outputTank();
        if (inputTank.isEmpty() || outputTank.getSpace() <= 0) {
            return;
        }

        GasStack inputStack = inputTank.getGasStack();
        Optional<GasConversion> executableConversion = findExecutableConversion(chargerType, inputStack, outputTank, processingBudget);
        if (executableConversion.isEmpty()) {
            return;
        }

        GasConversion conversion = executableConversion.get();
        GasStack outputPerBatch = conversion.output();
        long inputAmount = conversion.input().amount();
        long outputAmount = outputPerBatch.getAmount();
        long batchCount = Math.min(inputStack.getAmount() / inputAmount, processingBudget / inputAmount);
        batchCount = Math.min(batchCount, outputTank.getSpace() / outputAmount);

        GasStack inputRequest = inputStack.copyWithAmount(batchCount * inputAmount);
        GasStack outputRequest = outputPerBatch.copyWithAmount(batchCount * outputAmount);
        executeGasConversionTransaction(inputTank, inputRequest, outputRequest);
    }

    private void executeGasConversionTransaction(GasTank sourceTank, GasStack inputRequest, GasStack outputRequest) {
        SmartGasTankBehaviour outputBehaviour = chamber.getTankBehaviourInternal();
        ResourceTransaction conversionTransaction = new ResourceTransaction().add(ResourceTransaction.participant(() -> GasStack.matches(sourceTank.drain(inputRequest, GasAction.SIMULATE), inputRequest), () -> MachineResourceSnapshots.copyGas(sourceTank), () -> GasStack.matches(sourceTank.drain(inputRequest, GasAction.EXECUTE), inputRequest), sourceSnapshot -> MachineResourceSnapshots.restoreGas(sourceTank, sourceSnapshot))).add(ResourceTransaction.participant(() -> outputBehaviour.getInternalGasHandler().forceFill(outputRequest, GasAction.SIMULATE) == outputRequest.getAmount(), () -> MachineResourceSnapshots.snapshotGasTanks(outputBehaviour), () -> outputBehaviour.getInternalGasHandler().forceFill(outputRequest, GasAction.EXECUTE) == outputRequest.getAmount(), outputSnapshot -> MachineResourceSnapshots.restoreGasTanks(outputSnapshot, outputBehaviour)));
        conversionTransaction.commit();
    }

    private GasTank outputTank() {
        return chamber.getTankBehaviourInternal().getPrimaryHandler();
    }

    private GasStack getTankGasStack() {
        IChamberGasTank chamberTank = getTank();
        if (chamberTank == null) {
            return GasStack.EMPTY;
        }

        GasTank inputTank = chamberTank.getTankInventory();
        if (inputTank.isEmpty()) {
            return GasStack.EMPTY;
        }
        return inputTank.getGasStack();
    }

    private Optional<GasConversion> findExecutableConversion(ChargerType chargerType, GasStack inputStack, GasTank outputTank, long processingBudget) {
        for (GasConversion conversion : getConversions(chargerType, inputStack)) {
            if (!conversion.hasRequiredInput(inputStack)) {
                continue;
            }

            long inputAmount = conversion.input().amount();
            if (processingBudget < inputAmount) {
                continue;
            }

            GasStack outputPerBatch = conversion.output();
            if (!outputTank.isEmpty() && !GasStack.isSameGasSameComponents(outputTank.getGasStack(), outputPerBatch)) {
                continue;
            }

            if (outputTank.getSpace() < outputPerBatch.getAmount()) {
                continue;
            }

            return Optional.of(conversion);
        }
        return Optional.empty();
    }

    private List<GasConversion> getConversions(ChargerType chargerType, GasStack inputStack) {
        if (chamber.getLevel() == null || inputStack.isEmpty()) {
            return List.of();
        }
        return switch (chargerType) {
            case NORMAL -> BreezeChamberRecipeIndex.findEnergizationCandidates(chamber.getLevel().getRecipeManager(), inputStack);
            case BAD -> BreezeChamberRecipeIndex.findDissipationCandidates(chamber.getLevel().getRecipeManager(), inputStack);
            case NONE -> List.of();
        };
    }

    private @Nullable IChamberGasTank getTank() {
        if (chamber.getLevel() == null) {
            return null;
        }

        IChamberGasTank chamberTank = chamber.source.get();
        if (chamberTank != null && !chamberTank.isRemoved()) {
            return chamberTank.getControllerBE();
        }

        chamber.source = new WeakReference<>(null);
        chamberTank = chamber.getLevel().getBlockEntity(chamber.getBlockPos().below()) instanceof IChamberGasTank tankBelow ? tankBelow : null;
        chamber.source = new WeakReference<>(chamberTank);
        if (chamberTank == null) {
            return null;
        }
        return chamberTank.getControllerBE();
    }
}
