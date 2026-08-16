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
public final class AirtightHatchController {
    private static final int TICKS_PER_SECOND = 20;

    private final AirtightHatchBlockEntity hatch;
    private final AirtightHatchCanisterManager canisterManager;

    private long transferRemainder;

    public AirtightHatchController(AirtightHatchBlockEntity hatch, AirtightHatchCanisterManager canisterManager) {
        this.hatch = hatch;
        this.canisterManager = canisterManager;
    }

    private static void inputOnly(IGasHandler hatch, IGasHandler target, long limit, boolean creative) {
        GasStack hatchGas = hatch.getGasInTank(0);
        GasStack available = hatchGas.isEmpty() ? target.drain(limit, GasAction.SIMULATE) : target.drain(hatchGas.copyWithAmount(limit), GasAction.SIMULATE);
        if (available.isEmpty()) {
            return;
        }

        long amount = Math.min(limit, available.getAmount());
        transferGas(target, hatch, available.copyWithAmount(amount), false, creative);
    }

    private static void outputOnly(IGasHandler hatch, IGasHandler target, long limit, boolean creative) {
        GasStack hatchGas = hatch.getGasInTank(0);
        if (hatchGas.isEmpty()) {
            return;
        }

        long amount = Math.min(limit, hatchGas.getAmount());
        transferGas(hatch, target, hatchGas.copyWithAmount(amount), creative, false);
    }

    private static void stayHalf(IGasHandler hatch, IGasHandler target, long limit, boolean creative) {
        GasStack hatchGas = hatch.getGasInTank(0);
        long delta = hatchGas.getAmount() - hatch.getTankCapacity(0) / 2;
        if (delta == 0) {
            return;
        }

        long amount = Math.min(limit, Math.abs(delta));
        if (delta > 0) {
            outputOnly(hatch, target, amount, creative);
            return;
        }

        inputOnly(hatch, target, amount, creative);
    }

    private static void transferGas(IGasHandler source, IGasHandler target, GasStack offered, boolean infiniteSource, boolean voidTarget) {
        if (offered.isEmpty()) {
            return;
        }

        long accepted = voidTarget ? offered.getAmount() : target.fill(offered, GasAction.SIMULATE);
        accepted = Math.clamp(accepted, 0, offered.getAmount());
        if (accepted == 0) {
            return;
        }

        GasStack request = offered.copyWithAmount(accepted);
        GasStack drained = infiniteSource ? request : executeMatchingDrain(source, request);
        if (drained.isEmpty()) {
            return;
        }

        if (voidTarget) {
            return;
        }

        long filled = target.fill(drained, GasAction.EXECUTE);
        filled = Math.clamp(filled, 0, drained.getAmount());
        if (infiniteSource || filled >= drained.getAmount()) {
            return;
        }

        GasStack remainder = drained.copyWithAmount(drained.getAmount() - filled);
        source.fill(remainder, GasAction.EXECUTE);
    }

    private static GasStack executeMatchingDrain(IGasHandler source, GasStack request) {
        if (request.isEmpty()) {
            return GasStack.EMPTY;
        }

        GasStack drained = source.drain(request, GasAction.EXECUTE);
        if (!drained.isEmpty()) {
            return validateDrainedGas(source, request, drained);
        }

        GasStack genericPreview = source.drain(request.getAmount(), GasAction.SIMULATE);
        if (genericPreview.isEmpty() || !GasStack.isSameGasSameComponents(genericPreview, request)) {
            return GasStack.EMPTY;
        }

        drained = source.drain(request.getAmount(), GasAction.EXECUTE);
        return validateDrainedGas(source, request, drained);
    }

    private static GasStack validateDrainedGas(IGasHandler source, GasStack request, GasStack drained) {
        if (drained.isEmpty()) {
            return GasStack.EMPTY;
        }

        if (!GasStack.isSameGasSameComponents(drained, request)) {
            source.fill(drained, GasAction.EXECUTE);
            return GasStack.EMPTY;
        }

        long requestedAmount = request.getAmount();
        if (drained.getAmount() <= requestedAmount) {
            return drained;
        }

        GasStack excess = drained.copyWithAmount(drained.getAmount() - requestedAmount);
        source.fill(excess, GasAction.EXECUTE);
        return drained.copyWithAmount(requestedAmount);
    }

    public void tick() {
        Level level = hatch.getLevel();
        if (level == null || level.isClientSide || hatch.isEmpty()) {
            return;
        }

        long transferQuota = getTransferQuota();
        if (transferQuota <= 0) {
            return;
        }

        tryTransferGas(level, transferQuota);
    }

    public void lazyTick() {
        Level level = hatch.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = hatch.getBlockState();
        if (!(state.getBlock() instanceof AirtightHatchBlock hatchBlock)) {
            return;
        }

        if (!hatchBlock.canSurvive(state, level, hatch.getBlockPos())) {
            level.destroyBlock(hatch.getBlockPos(), true);
            return;
        }

        if (hatch.isEmpty()) {
            return;
        }

        canisterManager.updateCapacity(true);
    }

    public void resetTransferQuota() {
        transferRemainder = 0;
    }

    private long getTransferQuota() {
        transferRemainder += CCBConfig.server().airtights.maxTransferRate.get();
        long quota = transferRemainder / TICKS_PER_SECOND;
        transferRemainder %= TICKS_PER_SECOND;
        return quota;
    }

    private void tryTransferGas(Level level, long quota) {
        AirtightHatchTransferMode mode = AirtightHatchTransferMode.fromValue(hatch.getTransferModeValue());
        if (mode == AirtightHatchTransferMode.NO_TRANSFER) {
            return;
        }

        IGasHandler target = hatch.getTargetGasHandler(level);
        if (target == null) {
            return;
        }

        IGasHandler hatchHandler = hatch.getGasTankBehaviour().getPrimaryHandler();
        boolean creative = hatch.isCreative();
        switch (mode) {
            case INPUT_ONLY -> inputOnly(hatchHandler, target, quota, creative);
            case OUTPUT_ONLY -> outputOnly(hatchHandler, target, quota, creative);
            case STAY_HALF -> stayHalf(hatchHandler, target, quota, creative);
        }
    }
}
