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

    private static CanisterType toBlockCanisterType(HatchCanisterType canisterType) {
        return canisterType == HatchCanisterType.CREATIVE ? CanisterType.CREATIVE : CanisterType.NORMAL;
    }

    private static boolean isSameSnapshot(GasStack expectedGas, GasStack actualGas) {
        if (expectedGas.isEmpty() || actualGas.isEmpty()) {
            return expectedGas.isEmpty() && actualGas.isEmpty();
        }
        return expectedGas.getAmount() == actualGas.getAmount() && GasStack.isSameGasSameComponents(expectedGas, actualGas);
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

        BlockState hatchState = hatch.getBlockState();
        if (!(hatchState.getBlock() instanceof AirtightHatchBlock)) {
            return;
        }

        CanisterType expectedCanisterType = getStoredCanisterType();
        SmartGasTank gasTank = hatch.getGasTankBehaviour().getPrimaryHandler();
        boolean canisterStackChanged = false;
        boolean blockStateChanged = false;
        boolean gasTankChanged = false;

        hatch.getGasTankBehaviour().beginMutation();
        try {
            if (expectedCanisterType == CanisterType.EMPTY) {
                if (!canister.isEmpty()) {
                    canister = ItemStack.EMPTY;
                    canisterStackChanged = true;
                }
                if (!gasTank.getGasStack().isEmpty()) {
                    gasTank.setGasStack(GasStack.EMPTY);
                    gasTankChanged = true;
                }
                if (gasTank.getCapacity() != 0) {
                    gasTank.setCapacity(0);
                    gasTankChanged = true;
                }
            }
            else if (canister.getCount() != 1) {
                canister = canister.copyWithCount(1);
                canisterStackChanged = true;
            }

            if (hatchState.getValue(AirtightHatchBlock.CANISTER_TYPE) != expectedCanisterType) {
                blockStateChanged = updateCanisterBlockState(level, hatchState, expectedCanisterType);
            }
        } finally {
            gasTankChanged |= hatch.getGasTankBehaviour().endMutation();
        }

        if (canisterStackChanged || blockStateChanged || gasTankChanged) {
            hatch.setChanged();
        }
        if (!gasTankChanged) {
            return;
        }

        hatch.getGasTankBehaviour().sendDataImmediately();
    }

    ItemStack createCanisterItemStack() {
        ItemStack canisterStack = canister.copyWithCount(1);
        IAirtightHatchCanister hatchCanister = AirtightHatchCanisters.get(canisterStack);
        if (hatchCanister == null) {
            return ItemStack.EMPTY;
        }

        GasStack gasSnapshot = hatch.getHatchGasContent();
        if (!hatchCanister.setAirtightHatchContents(gasSnapshot)) {
            return ItemStack.EMPTY;
        }

        hatchCanister.save();
        IAirtightHatchCanister savedCanister = AirtightHatchCanisters.get(canisterStack);
        return savedCanister != null && isSameSnapshot(gasSnapshot, savedCanister.getAirtightHatchContents()) ? canisterStack : ItemStack.EMPTY;
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

        BlockState hatchState = hatch.getBlockState();
        if (!(hatchState.getBlock() instanceof AirtightHatchBlock)) {
            return false;
        }

        ItemStack installedCanister = sourceStack.copyWithCount(1);
        IAirtightHatchCanister hatchCanister = AirtightHatchCanisters.get(installedCanister);
        if (hatchCanister == null) {
            return false;
        }

        CanisterType canisterType = toBlockCanisterType(hatchCanister.getAirtightHatchType());
        GasStack canisterGas = hatchCanister.getAirtightHatchContents().copy();
        long canisterCapacity = getEffectiveCapacity(hatchCanister.getAirtightHatchCapacity(canisterGas), canisterGas.getAmount());

        ItemStack previousCanister = canister;
        long previousCapacity = hatch.getHatchCapacity();
        GasStack previousGas = getInternalGasContent().copy();
        SmartGasTank gasTank = hatch.getGasTankBehaviour().getPrimaryHandler();
        boolean blockStateUpdated = false;

        hatch.getGasTankBehaviour().beginMutation();
        try {
            canister = installedCanister;
            gasTank.setCapacity(canisterCapacity);
            gasTank.setGasStack(canisterGas);
            blockStateUpdated = updateCanisterBlockState(level, hatchState, canisterType);
            if (!blockStateUpdated) {
                canister = previousCanister;
                gasTank.setCapacity(previousCapacity);
                gasTank.setGasStack(previousGas);
                return false;
            }

            sourceStack.shrink(1);
            if (canisterType == CanisterType.CREATIVE && AirtightHatchTransferMode.fromValue(hatch.getTransferModeValue()) == AirtightHatchTransferMode.STAY_HALF) {
                hatch.resetTransferMode();
            }
            hatch.resetTransferQuota();
            hatch.setChanged();
            return true;
        } finally {
            boolean gasTankChanged = hatch.getGasTankBehaviour().endMutation();
            if (blockStateUpdated && gasTankChanged) {
                hatch.getGasTankBehaviour().sendDataImmediately();
            }
        }
    }

    void updateCapacity(boolean syncImmediately) {
        IAirtightHatchCanister hatchCanister = AirtightHatchCanisters.get(canister);
        if (hatchCanister == null) {
            return;
        }

        SmartGasTank gasTank = hatch.getGasTankBehaviour().getPrimaryHandler();
        GasStack gasContent = gasTank.getGasStack().copy();
        long effectiveCapacity = getEffectiveCapacity(hatchCanister.getAirtightHatchCapacity(gasContent), gasContent.getAmount());
        if (gasTank.getCapacity() == effectiveCapacity) {
            return;
        }

        gasTank.setCapacity(effectiveCapacity);
        Level level = hatch.getLevel();
        if (!syncImmediately || level == null || level.isClientSide) {
            return;
        }

        hatch.getGasTankBehaviour().sendDataImmediately();
    }

    private boolean updateCanisterBlockState(Level level, BlockState hatchState, CanisterType canisterType) {
        return hatchState.getValue(AirtightHatchBlock.CANISTER_TYPE) == canisterType || level.setBlockAndUpdate(hatch.getBlockPos(), hatchState.setValue(AirtightHatchBlock.CANISTER_TYPE, canisterType));
    }

    private ItemStack removeCanister() {
        Level level = hatch.getLevel();
        if (level == null || level.isClientSide || hatch.isEmpty()) {
            return ItemStack.EMPTY;
        }

        BlockState hatchState = hatch.getBlockState();
        if (!(hatchState.getBlock() instanceof AirtightHatchBlock)) {
            return ItemStack.EMPTY;
        }

        ItemStack removedCanister = createCanisterItemStack();
        if (removedCanister.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack previousCanister = canister;
        long previousCapacity = hatch.getHatchCapacity();
        GasStack previousGas = getInternalGasContent().copy();
        SmartGasTank gasTank = hatch.getGasTankBehaviour().getPrimaryHandler();
        boolean blockStateUpdated = false;

        hatch.getGasTankBehaviour().beginMutation();
        try {
            canister = ItemStack.EMPTY;
            gasTank.setGasStack(GasStack.EMPTY);
            gasTank.setCapacity(0);
            blockStateUpdated = updateCanisterBlockState(level, hatchState, CanisterType.EMPTY);
            if (!blockStateUpdated) {
                canister = previousCanister;
                gasTank.setCapacity(previousCapacity);
                gasTank.setGasStack(previousGas);
                return ItemStack.EMPTY;
            }

            hatch.resetTransferQuota();
            hatch.setChanged();
            return removedCanister;
        } finally {
            boolean gasTankChanged = hatch.getGasTankBehaviour().endMutation();
            if (blockStateUpdated && gasTankChanged) {
                hatch.getGasTankBehaviour().sendDataImmediately();
            }
        }
    }

    private GasStack getInternalGasContent() {
        return hatch.getGasTankBehaviour().getPrimaryHandler().getGasStack();
    }
}
