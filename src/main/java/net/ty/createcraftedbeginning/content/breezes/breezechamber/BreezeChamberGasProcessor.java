package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.IChamberGasTank;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.content.airtights.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberRecipeIndex.GasConversion;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeChamberGasProcessor {
    private static final int GAS_PROCESSING_INTERVAL = 20;
    private final BreezeChamberBlockEntity chamber;

    BreezeChamberGasProcessor(BreezeChamberBlockEntity chamber) {
        this.chamber = chamber;
    }

    private static boolean isControllerActive(IChamberGasTank tank) {
        if (!(tank instanceof AirtightTankBlockEntity controller)) {
            return false;
        }
        AirtightAssemblyDriverCore driverCore = controller.getCore();
        return driverCore.isActive();
    }

    private static int getProcessingAmount(int time) {
        if (time == 0) {
            return 0;
        }
        int maxAmount = CCBConfig.server().airtights.maxProcessingRate.get();
        float ratio = Mth.clamp((float) Mth.abs(time) / BreezeChamberBlockEntity.getMaxEffectiveThreshold(), 0, 1);
        return Mth.clamp((int) (maxAmount * ratio), 1, maxAmount);
    }

    private static ChargerType chargerTypeFor(WindLevel windLevel) {
        return switch (windLevel) {
            case GALE -> ChargerType.NORMAL;
            case ILL -> ChargerType.BAD;
            case CALM -> ChargerType.NONE;
        };
    }

    boolean isControllerActive() {
        IChamberGasTank tank = getTank();
        return tank != null && isControllerActive(tank);
    }

    Gas getTankGasType() {
        return getTankGasStack().getGasType();
    }

    boolean isOutputFull() {
        return outputTank().getSpace() == 0;
    }

    boolean isOutputMismatched() {
        GasStack inputStack = getTankGasStack();
        GasTank output = outputTank();
        if (inputStack.isEmpty() || output.isEmpty()) {
            return false;
        }
        ChargerType type = chargerTypeFor(chamber.getWindLevel());
        return getConversion(type, inputStack).map(conversion -> !GasStack.isSameGasSameComponents(output.getGasStack(), conversion.output())).orElse(false);
    }

    boolean isInputInvalid() {
        GasStack inputStack = getTankGasStack();
        if (inputStack.isEmpty()) {
            return false;
        }
        ChargerType type = chargerTypeFor(chamber.getWindLevel());
        return type != ChargerType.NONE && getConversion(type, inputStack).isEmpty();
    }

    void tickGasProcessing(ChargerType chargerType) {
        if (chamber.getLevel() == null || chamber.getLevel().isClientSide || chargerType == ChargerType.NONE) {
            return;
        }
        long phase = chamber.getLevel().getGameTime() + chamber.getBlockPos().asLong();
        if (Math.floorMod(phase, GAS_PROCESSING_INTERVAL) != 0) {
            return;
        }
        IChamberGasTank tank = getTank();
        if (tank == null || isControllerActive(tank)) {
            return;
        }
        processGas(chargerType, tank);
    }

    private void processGas(ChargerType chargerType, IChamberGasTank tank) {
        GasTank inventory = tank.getTankInventory();
        GasTank output = outputTank();
        if (inventory.isEmpty() || output.getSpace() <= 0) {
            return;
        }
        GasStack inputStack = inventory.getGasStack();
        Optional<GasConversion> conversionResult = getConversion(chargerType, inputStack);
        if (conversionResult.isEmpty()) {
            return;
        }
        GasConversion conversion = conversionResult.get();
        GasStack outputPerBatch = conversion.output();
        if (!output.isEmpty() && !GasStack.isSameGasSameComponents(output.getGasStack(), outputPerBatch)) {
            return;
        }
        long inputAmount = conversion.input().amount();
        long outputAmount = outputPerBatch.getAmount();
        long processingBudget = getProcessingAmount(chamber.getWindRemainingTime());
        long batches = Math.min(inputStack.getAmount() / inputAmount, processingBudget / inputAmount);
        batches = Math.min(batches, output.getSpace() / outputAmount);
        if (batches <= 0) {
            return;
        }
        GasStack inputRequest = inputStack.copyWithAmount(batches * inputAmount);
        GasStack outputRequest = outputPerBatch.copyWithAmount(batches * outputAmount);
        executeGasConversionTransaction(inventory, inputRequest, outputRequest);
    }

    private void executeGasConversionTransaction(GasTank sourceTank, GasStack inputRequest, GasStack outputRequest) {
        SmartGasTankBehaviour outputBehaviour = chamber.getTankBehaviourInternal();
        ResourceTransaction transaction = new ResourceTransaction().add(ResourceTransaction.participant(() -> GasStack.matches(sourceTank.drain(inputRequest, GasAction.SIMULATE), inputRequest), () -> MachineResourceSnapshots.copyGas(sourceTank), () -> GasStack.matches(sourceTank.drain(inputRequest, GasAction.EXECUTE), inputRequest), snapshot -> MachineResourceSnapshots.restoreGas(sourceTank, snapshot))).add(ResourceTransaction.participant(() -> outputBehaviour.getInternalGasHandler().forceFill(outputRequest, GasAction.SIMULATE) == outputRequest.getAmount(), () -> MachineResourceSnapshots.snapshotGasContents(outputBehaviour), () -> outputBehaviour.getInternalGasHandler().forceFill(outputRequest, GasAction.EXECUTE) == outputRequest.getAmount(), snapshot -> MachineResourceSnapshots.restoreGasContents(snapshot, outputBehaviour)));
        transaction.commit();
    }

    private GasTank outputTank() {
        return chamber.getTankBehaviourInternal().getPrimaryHandler();
    }

    private GasStack getTankGasStack() {
        IChamberGasTank tank = getTank();
        if (tank == null) {
            return GasStack.EMPTY;
        }
        GasTank inventory = tank.getTankInventory();
        return inventory.isEmpty() ? GasStack.EMPTY : inventory.getGasStack();
    }

    private Optional<GasConversion> getConversion(ChargerType chargerType, GasStack inputStack) {
        if (chamber.getLevel() == null || inputStack.isEmpty()) {
            return Optional.empty();
        }
        return switch (chargerType) {
            case NORMAL -> BreezeChamberRecipeIndex.findEnergization(chamber.getLevel().getRecipeManager(), inputStack);
            case BAD -> BreezeChamberRecipeIndex.findDissipation(chamber.getLevel().getRecipeManager(), inputStack);
            case NONE -> Optional.empty();
        };
    }

    private @Nullable IChamberGasTank getTank() {
        if (chamber.getLevel() == null) {
            return null;
        }
        IChamberGasTank tank = chamber.source.get();
        if (tank != null && !tank.isRemoved()) {
            return tank.getControllerBE();
        }
        chamber.source = new WeakReference<>(null);
        tank = chamber.getLevel().getBlockEntity(chamber.getBlockPos().below()) instanceof IChamberGasTank tankBe ? tankBe : null;
        chamber.source = new WeakReference<>(tank);
        return tank == null ? null : tank.getControllerBE();
    }
}
