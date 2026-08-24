package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHatchController {
    private static final int TICKS_PER_SECOND = 20;

    private final AirtightHatchBlockEntity hatch;
    private final AirtightHatchCanisterManager canisterManager;

    private long transferRemainder;

    AirtightHatchController(AirtightHatchBlockEntity hatch, AirtightHatchCanisterManager canisterManager) {
        this.hatch = hatch;
        this.canisterManager = canisterManager;
    }

    private static void inputOnly(IGasHandler hatchHandler, IGasHandler targetHandler, long transferLimit, boolean isCreative) {
        GasStack hatchGas = hatchHandler.getGasInTank(0);
        GasStack availableGas = isCreative || hatchGas.isEmpty() ? targetHandler.drain(transferLimit, GasAction.SIMULATE) : targetHandler.drain(hatchGas.copyWithAmount(transferLimit), GasAction.SIMULATE);
        if (availableGas.isEmpty()) {
            return;
        }

        long transferAmount = Math.min(transferLimit, availableGas.getAmount());
        transferGas(targetHandler, hatchHandler, availableGas.copyWithAmount(transferAmount), false, isCreative);
    }

    private static void outputOnly(IGasHandler hatchHandler, IGasHandler targetHandler, long transferLimit, boolean isCreative) {
        GasStack hatchGas = hatchHandler.getGasInTank(0);
        if (hatchGas.isEmpty()) {
            return;
        }

        long transferAmount = isCreative ? transferLimit : Math.min(transferLimit, hatchGas.getAmount());
        transferGas(hatchHandler, targetHandler, hatchGas.copyWithAmount(transferAmount), isCreative, false);
    }

    private static void stayHalf(IGasHandler hatchHandler, IGasHandler targetHandler, long transferLimit) {
        GasStack hatchGas = hatchHandler.getGasInTank(0);
        long gasDelta = hatchGas.getAmount() - hatchHandler.getTankCapacity(0) / 2;
        if (gasDelta == 0) {
            return;
        }

        long transferAmount = Math.min(transferLimit, Math.abs(gasDelta));
        if (gasDelta > 0) {
            outputOnly(hatchHandler, targetHandler, transferAmount, false);
            return;
        }

        inputOnly(hatchHandler, targetHandler, transferAmount, false);
    }

    private static void transferGas(IGasHandler source, IGasHandler target, GasStack offeredGas, boolean isInfiniteSource, boolean shouldVoidTarget) {
        if (offeredGas.isEmpty()) {
            return;
        }

        long acceptedAmount = shouldVoidTarget ? offeredGas.getAmount() : target.fill(offeredGas, GasAction.SIMULATE);
        acceptedAmount = Math.clamp(acceptedAmount, 0, offeredGas.getAmount());
        if (acceptedAmount == 0) {
            return;
        }

        GasStack drainRequest = offeredGas.copyWithAmount(acceptedAmount);
        GasStack drainedGas = isInfiniteSource ? drainRequest : executeMatchingDrain(source, drainRequest);
        if (drainedGas.isEmpty()) {
            return;
        }

        if (shouldVoidTarget) {
            return;
        }

        long filledAmount = target.fill(drainedGas, GasAction.EXECUTE);
        filledAmount = Math.clamp(filledAmount, 0, drainedGas.getAmount());
        if (isInfiniteSource || filledAmount >= drainedGas.getAmount()) {
            return;
        }

        GasStack remainderGas = drainedGas.copyWithAmount(drainedGas.getAmount() - filledAmount);
        source.fill(remainderGas, GasAction.EXECUTE);
    }

    private static GasStack executeMatchingDrain(IGasHandler source, GasStack requestedGas) {
        if (requestedGas.isEmpty()) {
            return GasStack.EMPTY;
        }

        GasStack drainedGas = source.drain(requestedGas, GasAction.EXECUTE);
        if (!drainedGas.isEmpty()) {
            return validateDrainedGas(source, requestedGas, drainedGas);
        }

        GasStack simulatedDrain = source.drain(requestedGas.getAmount(), GasAction.SIMULATE);
        if (simulatedDrain.isEmpty() || !GasStack.isSameGasSameComponents(simulatedDrain, requestedGas)) {
            return GasStack.EMPTY;
        }

        drainedGas = source.drain(requestedGas.getAmount(), GasAction.EXECUTE);
        return validateDrainedGas(source, requestedGas, drainedGas);
    }

    private static GasStack validateDrainedGas(IGasHandler source, GasStack requestedGas, GasStack drainedGas) {
        if (drainedGas.isEmpty()) {
            return GasStack.EMPTY;
        }

        if (!GasStack.isSameGasSameComponents(drainedGas, requestedGas)) {
            source.fill(drainedGas, GasAction.EXECUTE);
            return GasStack.EMPTY;
        }

        long requestedAmount = requestedGas.getAmount();
        if (drainedGas.getAmount() <= requestedAmount) {
            return drainedGas;
        }

        GasStack excessGas = drainedGas.copyWithAmount(drainedGas.getAmount() - requestedAmount);
        source.fill(excessGas, GasAction.EXECUTE);
        return drainedGas.copyWithAmount(requestedAmount);
    }

    void tick() {
        Level level = hatch.getLevel();
        if (level == null || level.isClientSide || hatch.isEmpty()) {
            return;
        }

        AirtightHatchTransferMode transferMode = AirtightHatchTransferMode.fromValue(hatch.getTransferModeValue());
        if (hatch.isCreative() && transferMode == AirtightHatchTransferMode.STAY_HALF) {
            hatch.resetTransferMode();
            hatch.resetTransferQuota();
            hatch.setChanged();
            hatch.sendData();
            return;
        }

        long transferQuota = getTransferQuota();
        if (transferQuota <= 0) {
            return;
        }

        tryTransferGas(level, transferQuota, transferMode);
    }

    void lazyTick() {
        Level level = hatch.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState hatchState = hatch.getBlockState();
        if (!(hatchState.getBlock() instanceof AirtightHatchBlock hatchBlock)) {
            return;
        }

        if (!hatchBlock.canSurvive(hatchState, level, hatch.getBlockPos())) {
            level.destroyBlock(hatch.getBlockPos(), true);
            return;
        }

        canisterManager.reconcileCanisterState();
        if (hatch.isEmpty()) {
            return;
        }

        canisterManager.updateCapacity(true);
    }

    void resetTransferQuota() {
        transferRemainder = 0;
    }

    private long getTransferQuota() {
        transferRemainder += CCBConfig.server().airtights.maxTransferRate.get();
        long transferQuota = transferRemainder / TICKS_PER_SECOND;
        transferRemainder %= TICKS_PER_SECOND;
        return transferQuota;
    }

    private void tryTransferGas(Level level, long transferQuota, AirtightHatchTransferMode transferMode) {
        if (transferMode == AirtightHatchTransferMode.NO_TRANSFER) {
            return;
        }

        IGasHandler targetHandler = hatch.getTargetGasHandler(level);
        if (targetHandler == null) {
            return;
        }

        IGasHandler hatchHandler = hatch.getGasTankBehaviour().getPrimaryHandler();
        boolean isCreative = hatch.isCreative();
        switch (transferMode) {
            case INPUT_ONLY -> inputOnly(hatchHandler, targetHandler, transferQuota, isCreative);
            case OUTPUT_ONLY -> outputOnly(hatchHandler, targetHandler, transferQuota, isCreative);
            case STAY_HALF -> stayHalf(hatchHandler, targetHandler, transferQuota);
        }
    }
}
