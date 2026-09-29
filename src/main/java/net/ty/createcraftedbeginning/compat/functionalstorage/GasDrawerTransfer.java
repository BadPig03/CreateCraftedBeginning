package net.ty.createcraftedbeginning.compat.functionalstorage;

import com.buuz135.functionalstorage.item.UpgradeItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferEndpoint;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferResult;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferService;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerTransfer {
    private static final long TRANSFER_PER_OPERATION = 4 * GasUnits.GU_PER_KGU;

    private GasDrawerTransfer() {
    }

    public static void push(Level level, GasDrawerBlockEntity drawer, ItemStack upgrade) {
        GasHandler targetHandler = getAdjacentHandler(level, drawer, upgrade);
        if (targetHandler == null) {
            return;
        }

        pushPressureSafe(drawer.getGasHandler(), targetHandler, TRANSFER_PER_OPERATION);
    }

    public static void pull(Level level, GasDrawerBlockEntity drawer, ItemStack upgrade) {
        GasHandler sourceHandler = getAdjacentHandler(level, drawer, upgrade);
        if (sourceHandler == null) {
            return;
        }

        pullPressureSafe(sourceHandler, drawer.getGasHandler(), TRANSFER_PER_OPERATION);
    }

    public static long transferDrawerToCanister(GasPressureCompartment source, GasCanisterContainer canister, int targetTank, long maxAmount) {
        GasPressureTransferResult result = GasPressureTransferService.transferPassive(
            GasPressureTransferEndpoint.compartment(source),
            GasPressureTransferEndpoint.canister(canister, targetTank),
            maxAmount,
            GasAction.EXECUTE
        );
        return result.sourceNetLoss();
    }

    public static long transferCanisterToDrawer(GasCanisterContainer canister, int sourceTank, GasPressureCompartment target, @Nullable VoidTarget voidTarget, long maxAmount) {
        GasPressureTransferEndpoint sourceEndpoint = GasPressureTransferEndpoint.canister(canister, sourceTank);
        GasStack sourceGas = sourceEndpoint.getGasStack();
        if (sourceGas.isEmpty()) {
            return 0;
        }

        return transferIntoTarget(
            sourceEndpoint,
            GasPressureTransferEndpoint.compartment(target),
            sourceGas.copyWithAmount(1),
            maxAmount,
            voidTarget
        );
    }

    public static long pushPressureSafe(GasStorageHandler sourceHandler, GasHandler targetHandler, long maxAmount) {
        long transferLimit = Math.max(0, maxAmount);
        if (transferLimit <= 0) {
            return 0;
        }

        for (int sourceTank = 0; sourceTank < sourceHandler.getTanks(); sourceTank++) {
            GasPressureTransferEndpoint sourceEndpoint = GasPressureTransferEndpoint.storage(sourceHandler, sourceTank);
            GasStack sourceGas = sourceEndpoint.getGasStack();
            if (sourceGas.isEmpty()) {
                continue;
            }

            long transferred = transferToHandler(sourceEndpoint, targetHandler, sourceGas.copyWithAmount(1), transferLimit);
            if (transferred > 0) {
                return transferred;
            }
        }
        return 0;
    }

    public static long pullPressureSafe(GasHandler sourceHandler, GasStorageHandler targetHandler, long maxAmount) {
        long transferLimit = Math.max(0, maxAmount);
        if (transferLimit <= 0) {
            return 0;
        }

        GasStack sourcePreview = sourceHandler.drain(transferLimit, GasAction.SIMULATE);
        if (sourcePreview.isEmpty()) {
            return 0;
        }

        GasStack selectedGas = sourcePreview.copyWithAmount(1);
        long transferred = 0;
        for (int sourceTank = 0; sourceTank < sourceHandler.getTanks() && transferred < transferLimit; sourceTank++) {
            Optional<GasPressureTransferEndpoint> sourceOptional = GasPressureTransferEndpoint.tryHandler(sourceHandler, sourceTank);
            if (sourceOptional.isEmpty()) {
                continue;
            }

            GasPressureTransferEndpoint sourceEndpoint = sourceOptional.get();
            GasStack sourceGas = sourceEndpoint.getGasStack();
            if (sourceGas.isEmpty() || !GasStack.isSameGasSameComponents(sourceGas, selectedGas)) {
                continue;
            }

            long moved = transferIntoStorageHandler(sourceEndpoint, targetHandler, selectedGas, transferLimit - transferred);
            transferred += moved;
        }
        return transferred;
    }

    private static long transferToHandler(GasPressureTransferEndpoint source, GasHandler targetHandler, GasStack gas, long maxAmount) {
        long transferred = transferToHandlerPass(source, targetHandler, gas, maxAmount, true);
        if (transferred >= maxAmount) {
            return transferred;
        }

        return transferred + transferToHandlerPass(source, targetHandler, gas, maxAmount - transferred, false);
    }

    private static long transferToHandlerPass(GasPressureTransferEndpoint source, GasHandler targetHandler, GasStack gas, long maxAmount, boolean existingGasOnly) {
        long transferred = 0;
        for (int targetTank = 0; targetTank < targetHandler.getTanks() && transferred < maxAmount; targetTank++) {
            Optional<GasPressureTransferEndpoint> targetOptional = GasPressureTransferEndpoint.tryHandler(targetHandler, targetTank);
            if (targetOptional.isEmpty()) {
                continue;
            }

            GasPressureTransferEndpoint target = targetOptional.get();
            GasStack targetGas = target.getGasStack();
            if (existingGasOnly) {
                if (targetGas.isEmpty() || !GasStack.isSameGasSameComponents(targetGas, gas)) {
                    continue;
                }
            }
            else if (!targetGas.isEmpty()) {
                continue;
            }

            long moved = transferIntoTarget(source, target, gas, maxAmount - transferred, findVoidTarget(targetHandler, targetTank));
            transferred += moved;
        }
        return transferred;
    }

    private static long transferIntoStorageHandler(GasPressureTransferEndpoint source, GasStorageHandler targetHandler, GasStack gas, long maxAmount) {
        for (boolean existingGasOnly : new boolean[]{true, false}) {
            for (int targetTank = 0; targetTank < targetHandler.getTanks(); targetTank++) {
                GasPressureCompartment targetCompartment = targetHandler.getPressureCompartment(targetTank);
                GasStack targetGas = targetCompartment.getGasStack();
                if (existingGasOnly) {
                    if (targetGas.isEmpty() || !GasStack.isSameGasSameComponents(targetGas, gas)) {
                        continue;
                    }
                }
                else if (!targetGas.isEmpty()) {
                    continue;
                }

                GasPressureTransferEndpoint target = GasPressureTransferEndpoint.compartment(targetCompartment);
                long moved = transferIntoTarget(source, target, gas, maxAmount, findVoidTarget(targetCompartment));
                if (moved > 0) {
                    return moved;
                }
            }
        }
        return 0;
    }

    private static long transferIntoTarget(GasPressureTransferEndpoint source, GasPressureTransferEndpoint target, GasStack gas, long maxAmount, @Nullable VoidTarget voidTarget) {
        long transferLimit = Math.max(0, maxAmount);
        if (transferLimit <= 0 || gas.isEmpty()) {
            return 0;
        }

        GasPressureTransferResult result = GasPressureTransferService.transferPassive(source, target, transferLimit, GasAction.EXECUTE);
        long sourceLoss = Math.min(transferLimit, result.sourceNetLoss());
        if (!result.fullyRecovered()) {
            return sourceLoss;
        }

        long remaining = transferLimit - sourceLoss;
        if (remaining <= 0 || voidTarget == null || source.identifiesSameCompartment(target) || !voidTarget.canVoid(gas)) {
            return sourceLoss;
        }

        long discarded = discardToVoid(source, gas, remaining);
        return sourceLoss + discarded;
    }

    private static long discardToVoid(GasPressureTransferEndpoint source, GasStack gas, long maxAmount) {
        if (maxAmount <= 0 || source.getDrainPressurePa(gas) <= GasPressure.VACUUM_PA) {
            return 0;
        }

        long simulatedDrain = source.simulateDrainAmount(gas, maxAmount);
        if (simulatedDrain <= 0) {
            return 0;
        }

        GasStack drainedGas = source.executeDrain(gas, simulatedDrain);
        return drainedGas.getAmount();
    }

    private static @Nullable VoidTarget findVoidTarget(GasHandler handler, int tank) {
        if (handler instanceof GasPressureCompartment compartment && tank == 0) {
            return findVoidTarget(compartment);
        }

        if (!(handler instanceof GasStorageHandler storage) || tank < 0 || tank >= storage.getTanks()) {
            return null;
        }

        return findVoidTarget(storage.getPressureCompartment(tank));
    }

    private static @Nullable VoidTarget findVoidTarget(GasPressureCompartment compartment) {
        if (compartment instanceof VoidTarget directVoidTarget) {
            return directVoidTarget;
        }

        Object identity = compartment.getCompartmentIdentity();
        if (!(identity instanceof VoidTarget identityVoidTarget)) {
            return null;
        }

        return identityVoidTarget;
    }

    private static @Nullable GasHandler getAdjacentHandler(Level level, GasDrawerBlockEntity drawer, ItemStack upgrade) {
        Direction transferDirection = UpgradeItem.getDirection(upgrade);
        return level.getCapability(GasCapabilities.BLOCK, drawer.getBlockPos().relative(transferDirection), transferDirection.getOpposite());
    }

    @FunctionalInterface
    public interface VoidTarget {
        boolean canVoid(GasStack gas);
    }
}
