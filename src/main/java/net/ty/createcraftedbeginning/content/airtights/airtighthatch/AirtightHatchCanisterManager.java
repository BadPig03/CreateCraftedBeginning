package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.SmartGasTank;
import net.ty.createcraftedbeginning.api.gascanisters.AirtightHatchCanisters;
import net.ty.createcraftedbeginning.api.gascanisters.IAirtightHatchCanister;
import net.ty.createcraftedbeginning.api.gascanisters.IAirtightHatchCanister.HatchCanisterType;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock.CanisterType;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHatchCanisterManager {
    private final AirtightHatchBlockEntity hatch;

    private ItemStack canister = ItemStack.EMPTY;

    AirtightHatchCanisterManager(AirtightHatchBlockEntity hatch) {
        this.hatch = hatch;
    }

    private static long getEffectiveCapacity(long configuredCapacity, long gasAmount) {
        long normalizedCapacity = Math.max(0, configuredCapacity);
        long normalizedGasAmount = Math.max(0, gasAmount);
        return Math.max(normalizedCapacity, normalizedGasAmount);
    }

    private static CanisterType toBlockCanisterType(HatchCanisterType type) {
        return type == HatchCanisterType.CREATIVE ? CanisterType.CREATIVE : CanisterType.NORMAL;
    }

    private static boolean isSameSnapshot(GasStack first, GasStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return first.isEmpty() && second.isEmpty();
        }
        return first.getAmount() == second.getAmount() && GasStack.isSameGasSameComponents(first, second);
    }

    ItemStack getStoredCanister() {
        return canister.copy();
    }

    void setStoredCanister(ItemStack canister) {
        this.canister = canister.isEmpty() ? ItemStack.EMPTY : canister.copyWithCount(1);
    }

    CanisterType getStoredCanisterType() {
        IAirtightHatchCanister hatchCanister = AirtightHatchCanisters.get(canister);
        return hatchCanister == null ? CanisterType.EMPTY : toBlockCanisterType(hatchCanister.getAirtightHatchType());
    }

    boolean isEmpty() {
        return getStoredCanisterType() == CanisterType.EMPTY;
    }

    boolean isCreative() {
        return getStoredCanisterType() == CanisterType.CREATIVE;
    }

    void reconcileCanisterState() {
        Level level = hatch.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = hatch.getBlockState();
        if (!(state.getBlock() instanceof AirtightHatchBlock)) {
            return;
        }

        CanisterType expectedType = getStoredCanisterType();
        SmartGasTank tank = hatch.getGasTankBehaviour().getPrimaryHandler();
        boolean canisterChanged = false;
        boolean stateChanged = false;
        boolean tankStateChanged = false;

        hatch.getGasTankBehaviour().beginMutation();
        try {
            if (expectedType == CanisterType.EMPTY) {
                if (!canister.isEmpty()) {
                    canister = ItemStack.EMPTY;
                    canisterChanged = true;
                }
                if (!tank.getGasStack().isEmpty()) {
                    tank.setGasStack(GasStack.EMPTY);
                    tankStateChanged = true;
                }
                if (tank.getCapacity() != 0) {
                    tank.setCapacity(0);
                    tankStateChanged = true;
                }
            }
            else if (canister.getCount() != 1) {
                canister = canister.copyWithCount(1);
                canisterChanged = true;
            }

            if (state.getValue(AirtightHatchBlock.CANISTER_TYPE) != expectedType) {
                stateChanged = updateCanisterBlockState(level, state, expectedType);
            }
        } finally {
            tankStateChanged |= hatch.getGasTankBehaviour().endMutation();
        }

        if (canisterChanged || stateChanged || tankStateChanged) {
            hatch.setChanged();
        }
        if (!tankStateChanged) {
            return;
        }

        hatch.getGasTankBehaviour().sendDataImmediately();
    }

    ItemStack createCanisterItemStack() {
        ItemStack stack = canister.copyWithCount(1);
        IAirtightHatchCanister hatchCanister = AirtightHatchCanisters.get(stack);
        if (hatchCanister == null) {
            return ItemStack.EMPTY;
        }

        GasStack snapshot = hatch.getHatchGasContent();
        if (!hatchCanister.setAirtightHatchContents(snapshot)) {
            return ItemStack.EMPTY;
        }

        hatchCanister.save();
        IAirtightHatchCanister savedCanister = AirtightHatchCanisters.get(stack);
        return savedCanister != null && isSameSnapshot(snapshot, savedCanister.getAirtightHatchContents()) ? stack : ItemStack.EMPTY;
    }

    boolean giveCanisterToPlayer(Player player) {
        ItemStack removedCanister = removeCanister();
        if (removedCanister.isEmpty()) {
            return false;
        }

        ItemHandlerHelper.giveItemToPlayer(player, removedCanister);
        return true;
    }

    boolean installCanister(ItemStack sourceStack) {
        Level level = hatch.getLevel();
        if (level == null || level.isClientSide || sourceStack.isEmpty() || !hatch.isEmpty()) {
            return false;
        }

        BlockState state = hatch.getBlockState();
        if (!(state.getBlock() instanceof AirtightHatchBlock)) {
            return false;
        }

        ItemStack newCanister = sourceStack.copyWithCount(1);
        IAirtightHatchCanister hatchCanister = AirtightHatchCanisters.get(newCanister);
        if (hatchCanister == null) {
            return false;
        }

        CanisterType type = toBlockCanisterType(hatchCanister.getAirtightHatchType());
        GasStack gas = hatchCanister.getAirtightHatchContents().copy();
        long capacity = getEffectiveCapacity(hatchCanister.getAirtightHatchCapacity(gas), gas.getAmount());

        ItemStack oldCanister = canister;
        long oldCapacity = hatch.getHatchCapacity();
        GasStack oldGas = getInternalGasContent().copy();
        SmartGasTank tank = hatch.getGasTankBehaviour().getPrimaryHandler();
        boolean updated = false;

        hatch.getGasTankBehaviour().beginMutation();
        try {
            canister = newCanister;
            tank.setCapacity(capacity);
            tank.setGasStack(gas);
            updated = updateCanisterBlockState(level, state, type);
            if (!updated) {
                canister = oldCanister;
                tank.setCapacity(oldCapacity);
                tank.setGasStack(oldGas);
                return false;
            }

            sourceStack.shrink(1);
            if (type == CanisterType.CREATIVE && AirtightHatchTransferMode.fromValue(hatch.getTransferModeValue()) == AirtightHatchTransferMode.STAY_HALF) {
                hatch.resetTransferMode();
            }
            hatch.resetTransferQuota();
            hatch.setChanged();
            return true;
        } finally {
            boolean tankChanged = hatch.getGasTankBehaviour().endMutation();
            if (updated && tankChanged) {
                hatch.getGasTankBehaviour().sendDataImmediately();
            }
        }
    }

    void updateCapacity(boolean syncImmediately) {
        IAirtightHatchCanister hatchCanister = AirtightHatchCanisters.get(canister);
        if (hatchCanister == null) {
            return;
        }

        SmartGasTank tank = hatch.getGasTankBehaviour().getPrimaryHandler();
        GasStack gas = tank.getGasStack().copy();
        long capacity = getEffectiveCapacity(hatchCanister.getAirtightHatchCapacity(gas), gas.getAmount());
        if (tank.getCapacity() == capacity) {
            return;
        }

        tank.setCapacity(capacity);
        Level level = hatch.getLevel();
        if (!syncImmediately || level == null || level.isClientSide) {
            return;
        }

        hatch.getGasTankBehaviour().sendDataImmediately();
    }

    private boolean updateCanisterBlockState(Level level, BlockState state, CanisterType type) {
        return state.getValue(AirtightHatchBlock.CANISTER_TYPE) == type || level.setBlockAndUpdate(hatch.getBlockPos(), state.setValue(AirtightHatchBlock.CANISTER_TYPE, type));
    }

    private ItemStack removeCanister() {
        Level level = hatch.getLevel();
        if (level == null || level.isClientSide || hatch.isEmpty()) {
            return ItemStack.EMPTY;
        }

        BlockState state = hatch.getBlockState();
        if (!(state.getBlock() instanceof AirtightHatchBlock)) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = createCanisterItemStack();
        if (removed.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack oldCanister = canister;
        long oldCapacity = hatch.getHatchCapacity();
        GasStack oldGas = getInternalGasContent().copy();
        SmartGasTank tank = hatch.getGasTankBehaviour().getPrimaryHandler();
        boolean updated = false;

        hatch.getGasTankBehaviour().beginMutation();
        try {
            canister = ItemStack.EMPTY;
            tank.setGasStack(GasStack.EMPTY);
            tank.setCapacity(0);
            updated = updateCanisterBlockState(level, state, CanisterType.EMPTY);
            if (!updated) {
                canister = oldCanister;
                tank.setCapacity(oldCapacity);
                tank.setGasStack(oldGas);
                return ItemStack.EMPTY;
            }

            hatch.resetTransferQuota();
            hatch.setChanged();
            return removed;
        } finally {
            boolean tankChanged = hatch.getGasTankBehaviour().endMutation();
            if (updated && tankChanged) {
                hatch.getGasTankBehaviour().sendDataImmediately();
            }
        }
    }

    private GasStack getInternalGasContent() {
        return hatch.getGasTankBehaviour().getPrimaryHandler().getGasStack();
    }
}
