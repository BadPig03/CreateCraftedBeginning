package net.ty.createcraftedbeginning.compat.functionalstorage;

import com.buuz135.functionalstorage.item.UpgradeItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerTransfer {
    private static final long TRANSFER_PER_OPERATION = 4 * GasAmounts.MILLIBUCKETS_PER_BUCKET;

    private GasDrawerTransfer() {
    }

    public static void push(Level level, GasDrawerBlockEntity drawer, ItemStack upgrade) {
        IGasHandler targetHandler = getAdjacentHandler(level, drawer, upgrade);
        if (targetHandler == null) {
            return;
        }

        for (GasDrawerTank drawerTank : drawer.getGasHandler().getInternalTanks()) {
            GasStack simulatedDrain = drawerTank.drain(TRANSFER_PER_OPERATION, GasAction.SIMULATE);
            if (simulatedDrain.isEmpty()) {
                continue;
            }

            long acceptedAmount = targetHandler.fill(simulatedDrain, GasAction.SIMULATE);
            if (acceptedAmount <= 0) {
                continue;
            }

            GasStack drainedGas = drawerTank.drain(simulatedDrain.copyWithAmount(Math.min(acceptedAmount, simulatedDrain.getAmount())), GasAction.EXECUTE);
            if (drainedGas.isEmpty()) {
                continue;
            }

            long insertedAmount = targetHandler.fill(drainedGas, GasAction.EXECUTE);
            restoreRemainder(drawerTank, drainedGas, insertedAmount);
            return;
        }
    }

    public static void pull(Level level, GasDrawerBlockEntity drawer, ItemStack upgrade) {
        IGasHandler sourceHandler = getAdjacentHandler(level, drawer, upgrade);
        if (sourceHandler == null) {
            return;
        }

        GasStack simulatedDrain = sourceHandler.drain(TRANSFER_PER_OPERATION, GasAction.SIMULATE);
        if (simulatedDrain.isEmpty()) {
            return;
        }

        GasDrawerHandler targetHandler = drawer.getGasHandler();
        long acceptedAmount = targetHandler.fill(simulatedDrain, GasAction.SIMULATE);
        if (acceptedAmount <= 0) {
            return;
        }

        GasStack drainedGas = sourceHandler.drain(simulatedDrain.copyWithAmount(acceptedAmount), GasAction.EXECUTE);
        if (drainedGas.isEmpty()) {
            return;
        }

        long insertedAmount = targetHandler.fill(drainedGas, GasAction.EXECUTE);
        if (insertedAmount >= drainedGas.getAmount()) {
            return;
        }
        sourceHandler.fill(drainedGas.copyWithAmount(drainedGas.getAmount() - insertedAmount), GasAction.EXECUTE);
    }

    private static void restoreRemainder(GasDrawerTank drawerTank, GasStack drainedGas, long insertedAmount) {
        if (insertedAmount >= drainedGas.getAmount()) {
            return;
        }
        drawerTank.fill(drainedGas.copyWithAmount(drainedGas.getAmount() - insertedAmount), GasAction.EXECUTE);
    }

    private static @Nullable IGasHandler getAdjacentHandler(Level level, GasDrawerBlockEntity drawer, ItemStack upgrade) {
        Direction transferDirection = UpgradeItem.getDirection(upgrade);
        return level.getCapability(GasHandler.BLOCK, drawer.getBlockPos().relative(transferDirection), transferDirection.getOpposite());
    }
}
