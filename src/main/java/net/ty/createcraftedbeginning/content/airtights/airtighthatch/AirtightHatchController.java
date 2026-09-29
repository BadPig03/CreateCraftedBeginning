package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferEndpoint;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferResult;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightHatchController {
    private static final int TICKS_PER_SECOND = 20;

    private final AirtightHatchBlockEntity hatch;
    private final AirtightHatchCanisterManager canisterManager;

    private long transferRemainder;

    AirtightHatchController(AirtightHatchBlockEntity hatch, AirtightHatchCanisterManager canisterManager) {
        this.hatch = hatch;
        this.canisterManager = canisterManager;
    }

    public static long inputOnlyPressureSafe(GasStorageHandler hatchHandler, GasHandler targetHandler, long transferLimit, boolean isCreative) {
        if (transferLimit <= 0 || hatchHandler.getTanks() <= 0) {
            return 0;
        }

        if (isCreative) {
            return transferFromHandler(targetHandler, GasPressureTransferEndpoint.pressureBoundary(new DiscardBoundary()), transferLimit);
        }

        return transferFromHandler(targetHandler, GasPressureTransferEndpoint.storage(hatchHandler, 0), transferLimit);
    }

    public static long outputOnlyPressureSafe(GasStorageHandler hatchHandler, GasHandler targetHandler, long transferLimit, boolean isCreative) {
        if (transferLimit <= 0 || hatchHandler.getTanks() <= 0) {
            return 0;
        }

        GasStack hatchGas = hatchHandler.getGasInTank(0);
        if (hatchGas.isEmpty()) {
            return 0;
        }

        GasPressureTransferEndpoint source;
        if (isCreative) {
            long sourcePressurePa = Math.max(GasPressure.VACUUM_PA, hatchHandler.getTankPressurePa(0));
            source = GasPressureTransferEndpoint.pressureBoundary(new CreativeSourceBoundary(hatchGas, sourcePressurePa));
        }
        else {
            source = GasPressureTransferEndpoint.storage(hatchHandler, 0);
        }
        return transferToHandler(source, targetHandler, transferLimit);
    }

    public static long targetPressureSafe(GasStorageHandler hatchHandler, GasHandler targetHandler, long transferLimit, long targetPressurePa) {
        if (transferLimit <= 0 || hatchHandler.getTanks() <= 0) {
            return 0;
        }

        long volume = hatchHandler.getTankVolume(0);
        long maxPressurePa = GasPressureLimits.clampToHardLimit(hatchHandler.getTankMaxPressurePa(0));
        long clampedTargetPressurePa = Mth.clamp(targetPressurePa, GasPressure.VACUUM_PA, maxPressurePa);
        long targetAmount = Math.min(hatchHandler.getTankMaxAmount(0), GasPressure.amount(volume, clampedTargetPressurePa));
        long currentAmount = hatchHandler.getGasInTank(0).getAmount();
        if (currentAmount == targetAmount) {
            return 0;
        }

        if (currentAmount > targetAmount) {
            return outputOnlyPressureSafe(hatchHandler, targetHandler, Math.min(transferLimit, currentAmount - targetAmount), false);
        }

        return inputOnlyPressureSafe(hatchHandler, targetHandler, Math.min(transferLimit, targetAmount - currentAmount), false);
    }

    void tick() {
        Level level = hatch.getLevel();
        if (level == null || level.isClientSide || hatch.isEmpty()) {
            return;
        }

        AirtightHatchTransferMode transferMode = AirtightHatchTransferMode.fromValue(hatch.getTransferModeValue());
        if (hatch.isCreative() && transferMode == AirtightHatchTransferMode.TARGET_PRESSURE) {
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

        canisterManager.updateTankLimits();
    }

    void resetTransferQuota() {
        transferRemainder = 0;
    }

    private static long transferFromHandler(GasHandler sourceHandler, GasPressureTransferEndpoint target, long transferLimit) {
        long remaining = Math.max(0, transferLimit);
        long transferred = 0;
        for (int tank = 0; tank < sourceHandler.getTanks() && remaining > 0; tank++) {
            Optional<GasPressureTransferEndpoint> source = GasPressureTransferEndpoint.tryHandler(sourceHandler, tank);
            if (source.isEmpty()) {
                continue;
            }

            GasPressureTransferResult result = GasPressureTransferService.transferPassive(source.get(), target, remaining, GasAction.EXECUTE);
            transferred += result.filledAmount();
            remaining -= result.filledAmount();
        }
        return transferred;
    }

    private static long transferToHandler(GasPressureTransferEndpoint source, GasHandler targetHandler, long transferLimit) {
        long remaining = Math.max(0, transferLimit);
        long transferred = 0;
        for (int tank = 0; tank < targetHandler.getTanks() && remaining > 0; tank++) {
            Optional<GasPressureTransferEndpoint> target = GasPressureTransferEndpoint.tryHandler(targetHandler, tank);
            if (target.isEmpty()) {
                continue;
            }

            GasPressureTransferResult result = GasPressureTransferService.transferPassive(source, target.get(), remaining, GasAction.EXECUTE);
            transferred += result.filledAmount();
            remaining -= result.filledAmount();
        }
        return transferred;
    }

    private long getTransferQuota() {
        transferRemainder += CCBConfig.server().machines.airtightHatch.maxTransferPerSecond.get();
        long transferQuota = transferRemainder / TICKS_PER_SECOND;
        transferRemainder %= TICKS_PER_SECOND;
        return transferQuota;
    }

    private void tryTransferGas(Level level, long transferQuota, AirtightHatchTransferMode transferMode) {
        if (transferMode == AirtightHatchTransferMode.NO_TRANSFER) {
            return;
        }

        GasHandler targetHandler = hatch.getTargetGasHandler(level);
        if (targetHandler == null) {
            return;
        }

        GasStorageHandler hatchHandler = hatch.getGasTankBehaviour().getPrimaryHandler();
        boolean isCreative = hatch.isCreative();
        switch (transferMode) {
            case INPUT_ONLY -> inputOnlyPressureSafe(hatchHandler, targetHandler, transferQuota, isCreative);
            case OUTPUT_ONLY -> outputOnlyPressureSafe(hatchHandler, targetHandler, transferQuota, isCreative);
            case TARGET_PRESSURE -> targetPressureSafe(hatchHandler, targetHandler, transferQuota, hatch.getTargetPressurePa());
        }
    }

    private record CreativeSourceBoundary(GasStack gas, long pressurePa) implements GasPressureBoundary {
        private CreativeSourceBoundary {
            gas = gas.isEmpty() ? GasStack.EMPTY : gas.copyWithAmount(1);
            pressurePa = Math.max(GasPressure.VACUUM_PA, pressurePa);
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0 && !gas.isEmpty() && GasStack.isSameGasSameComponents(gas, stack);
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (resource.isEmpty() || !GasStack.isSameGasSameComponents(gas, resource)) {
                return GasStack.EMPTY;
            }

            return gas.copyWithAmount(resource.getAmount());
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            if (gas.isEmpty() || maxDrain <= 0) {
                return GasStack.EMPTY;
            }

            return gas.copyWithAmount(maxDrain);
        }

        @Override
        public GasStack getGasInTank(int tank) {
            if (tank != 0 || gas.isEmpty()) {
                return GasStack.EMPTY;
            }

            return gas.copyWithAmount(Long.MAX_VALUE);
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (resource.isEmpty() || !GasStack.isSameGasSameComponents(gas, resource)) {
                return 0;
            }

            return resource.getAmount();
        }

        @Override
        public long getDrainPressurePa(int tank, GasStack requestedGas) {
            if (tank == 0 && GasStack.isSameGasSameComponents(gas, requestedGas)) {
                return pressurePa;
            }

            return GasPressure.VACUUM_PA;
        }

        @Override
        public boolean supportsExactDrainRecovery(int tank) {
            return tank == 0;
        }
    }

    private static final class DiscardBoundary implements GasPressureBoundary {
        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0 && !stack.isEmpty();
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return GasStack.EMPTY;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (resource.isEmpty()) {
                return 0;
            }

            return resource.getAmount();
        }
    }
}
