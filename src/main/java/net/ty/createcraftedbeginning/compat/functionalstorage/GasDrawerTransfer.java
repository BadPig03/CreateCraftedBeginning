package net.ty.createcraftedbeginning.compat.functionalstorage;

import com.buuz135.functionalstorage.item.UpgradeItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasDrawerTransfer {
    private static final long TRANSFER_PER_OPERATION = 4 * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;

    private GasDrawerTransfer() {
    }

    static void push(Level level, GasDrawerBlockEntity drawer, ItemStack upgrade) {
        IGasHandler target = getAdjacentHandler(level, drawer, upgrade);
        if (target == null) {
            return;
        }

        for (GasDrawerTank tank : drawer.getGasHandler().getInternalTanks()) {
            GasStack preview = tank.drain(TRANSFER_PER_OPERATION, GasAction.SIMULATE);
            if (preview.isEmpty()) {
                continue;
            }

            long accepted = target.fill(preview, GasAction.SIMULATE);
            if (accepted <= 0) {
                continue;
            }

            long transfer = Math.min(accepted, preview.getAmount());
            GasStack drained = tank.drain(preview.copyWithAmount(transfer), GasAction.EXECUTE);
            if (drained.isEmpty()) {
                continue;
            }

            long inserted = target.fill(drained, GasAction.EXECUTE);
            restoreRemainder(tank, drained, inserted);
            return;
        }
    }

    static void pull(Level level, GasDrawerBlockEntity drawer, ItemStack upgrade) {
        IGasHandler source = getAdjacentHandler(level, drawer, upgrade);
        if (source == null) {
            return;
        }

        GasStack preview = source.drain(TRANSFER_PER_OPERATION, GasAction.SIMULATE);
        if (preview.isEmpty()) {
            return;
        }

        GasDrawerHandler target = drawer.getGasHandler();
        long accepted = target.fill(preview, GasAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }

        GasStack drained = source.drain(preview.copyWithAmount(accepted), GasAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }

        long inserted = target.fill(drained, GasAction.EXECUTE);
        if (inserted >= drained.getAmount()) {
            return;
        }
        source.fill(drained.copyWithAmount(drained.getAmount() - inserted), GasAction.EXECUTE);
    }

    private static void restoreRemainder(GasDrawerTank tank, GasStack drained, long inserted) {
        if (inserted >= drained.getAmount()) {
            return;
        }
        tank.fill(drained.copyWithAmount(drained.getAmount() - inserted), GasAction.EXECUTE);
    }

    private static @Nullable IGasHandler getAdjacentHandler(Level level, GasDrawerBlockEntity drawer, ItemStack upgrade) {
        Direction direction = UpgradeItem.getDirection(upgrade);
        return level.getCapability(GasHandler.BLOCK, drawer.getBlockPos().relative(direction), direction.getOpposite());
    }
}
