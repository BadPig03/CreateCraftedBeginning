package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.api.canister.AirtightHatchCanister;
import net.ty.createcraftedbeginning.api.canister.AirtightHatchCanister.HatchCanisterType;
import net.ty.createcraftedbeginning.api.canister.AirtightHatchCanisters;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock.CanisterType;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import net.ty.createcraftedbeginning.gas.storage.SmartGasTank;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHatchCanisterManager {
    private static final GasTankLimits EMPTY_LIMITS = new GasTankLimits(0, 0);

    private final AirtightHatchBlockEntity hatch;

    private ItemStack canister = ItemStack.EMPTY;

    AirtightHatchCanisterManager(AirtightHatchBlockEntity hatch) {
        this.hatch = hatch;
    }

    ItemStack getStoredCanister() {
        return canister.copy();
    }

    void setStoredCanister(ItemStack canister) {
        this.canister = canister.isEmpty() ? ItemStack.EMPTY : canister.copyWithCount(1);
    }

    CanisterType getStoredCanisterType() {
        AirtightHatchCanister hatchCanister = AirtightHatchCanisters.of(canister);
        if (hatchCanister == null) {
            return CanisterType.EMPTY;
        }

        return toBlockCanisterType(hatchCanister.getAirtightHatchType());
    }

    GasTankLimits getStoredCanisterLimits() {
        AirtightHatchCanister hatchCanister = AirtightHatchCanisters.of(canister);
        if (hatchCanister == null) {
            return EMPTY_LIMITS;
        }

        return getCanisterLimits(hatchCanister);
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
        SmartGasTankBehaviour tankBehaviour = hatch.getGasTankBehaviour();
        SmartGasTank gasTank = tankBehaviour.getPrimaryHandler();
        boolean canisterStackChanged = false;
        boolean blockStateChanged = false;
        boolean gasTankChanged = false;

        tankBehaviour.beginMutation();
        try {
            if (expectedCanisterType == CanisterType.EMPTY) {
                if (!canister.isEmpty()) {
                    canister = ItemStack.EMPTY;
                    canisterStackChanged = true;
                }
                gasTankChanged = gasTank.tryApplyState(new GasTankState(EMPTY_LIMITS, GasStack.EMPTY)).changed();
            }
            else {
                if (canister.getCount() != 1) {
                    canister = canister.copyWithCount(1);
                    canisterStackChanged = true;
                }

                AirtightHatchCanister hatchCanister = AirtightHatchCanisters.of(canister);
                if (hatchCanister != null) {
                    gasTankChanged = gasTank.tryReconfigure(getCanisterLimits(hatchCanister)).changed();
                }
            }

            if (hatchState.getValue(AirtightHatchBlock.CANISTER_TYPE) != expectedCanisterType) {
                blockStateChanged = updateCanisterBlockState(level, hatchState, expectedCanisterType);
            }
        }
        finally {
            gasTankChanged |= tankBehaviour.endMutation();
        }

        if (canisterStackChanged || blockStateChanged || gasTankChanged) {
            hatch.setChanged();
        }
        if (!gasTankChanged) {
            return;
        }

        tankBehaviour.sendDataImmediately();
    }

    ItemStack createCanisterItemStack() {
        ItemStack canisterStack = canister.copyWithCount(1);
        AirtightHatchCanister hatchCanister = AirtightHatchCanisters.of(canisterStack);
        if (hatchCanister == null) {
            return ItemStack.EMPTY;
        }

        GasStack gasSnapshot = hatch.getHatchGasContent();
        if (!hatchCanister.setAirtightHatchContents(gasSnapshot)) {
            return ItemStack.EMPTY;
        }

        hatchCanister.save();
        AirtightHatchCanister savedCanister = AirtightHatchCanisters.of(canisterStack);
        if (savedCanister == null || !isSameSnapshot(gasSnapshot, savedCanister.getAirtightHatchContents())) {
            return ItemStack.EMPTY;
        }

        return canisterStack;
    }

    boolean giveCanisterToPlayer(Player player) {
        ItemStack removedCanister = removeCanister();
        if (removedCanister.isEmpty()) {
            return false;
        }

        ItemHandlerHelper.giveItemToPlayer(player, removedCanister);
        return true;
    }

    boolean canInstallCanister(ItemStack sourceStack) {
        if (sourceStack.isEmpty() || !hatch.isEmpty()) {
            return false;
        }

        AirtightHatchCanister hatchCanister = AirtightHatchCanisters.of(sourceStack.copyWithCount(1));
        if (hatchCanister == null) {
            return false;
        }

        GasStack canisterGas = hatchCanister.getAirtightHatchContents();
        GasTankLimits canisterLimits = getCanisterLimits(hatchCanister);
        return hatch.getGasTankBehaviour().getPrimaryHandler().canContain(canisterLimits, canisterGas);
    }

    boolean installCanister(ItemStack sourceStack) {
        Level level = hatch.getLevel();
        if (level == null || level.isClientSide || !canInstallCanister(sourceStack)) {
            return false;
        }

        BlockState hatchState = hatch.getBlockState();
        if (!(hatchState.getBlock() instanceof AirtightHatchBlock)) {
            return false;
        }

        ItemStack installedCanister = sourceStack.copyWithCount(1);
        AirtightHatchCanister hatchCanister = AirtightHatchCanisters.of(installedCanister);
        if (hatchCanister == null) {
            return false;
        }

        CanisterType canisterType = toBlockCanisterType(hatchCanister.getAirtightHatchType());
        GasStack canisterGas = hatchCanister.getAirtightHatchContents();
        GasTankLimits canisterLimits = getCanisterLimits(hatchCanister);
        SmartGasTankBehaviour tankBehaviour = hatch.getGasTankBehaviour();
        SmartGasTank gasTank = tankBehaviour.getPrimaryHandler();
        if (!gasTank.canContain(canisterLimits, canisterGas)) {
            return false;
        }

        ItemStack previousCanister = canister.copy();
        GasTankState previousState = gasTank.snapshot();
        boolean blockStateUpdated = false;

        tankBehaviour.beginMutation();
        try {
            canister = installedCanister;
            gasTank.tryApplyState(new GasTankState(canisterLimits, canisterGas)).requireAccepted();
            blockStateUpdated = updateCanisterBlockState(level, hatchState, canisterType);
            if (!blockStateUpdated) {
                canister = previousCanister;
                gasTank.tryApplyState(previousState).requireAccepted();
                return false;
            }

            sourceStack.shrink(1);
            if (canisterType == CanisterType.CREATIVE && AirtightHatchTransferMode.fromValue(hatch.getTransferModeValue()) == AirtightHatchTransferMode.TARGET_PRESSURE) {
                hatch.resetTransferMode();
            }
            hatch.resetTransferQuota();
            hatch.setChanged();
            return true;
        }
        finally {
            boolean gasTankChanged = tankBehaviour.endMutation();
            if (blockStateUpdated && gasTankChanged) {
                tankBehaviour.sendDataImmediately();
            }
        }
    }

    void updateTankLimits() {
        AirtightHatchCanister hatchCanister = AirtightHatchCanisters.of(canister);
        if (hatchCanister == null) {
            return;
        }

        SmartGasTank gasTank = hatch.getGasTankBehaviour().getPrimaryHandler();
        if (!gasTank.tryReconfigure(getCanisterLimits(hatchCanister)).changed()) {
            return;
        }

        Level level = hatch.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        hatch.getGasTankBehaviour().sendDataImmediately();
    }

    ItemStack removeCanister() {
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

        ItemStack previousCanister = canister.copy();
        SmartGasTankBehaviour tankBehaviour = hatch.getGasTankBehaviour();
        SmartGasTank gasTank = tankBehaviour.getPrimaryHandler();
        GasTankState previousState = gasTank.snapshot();
        boolean blockStateUpdated = false;

        tankBehaviour.beginMutation();
        try {
            canister = ItemStack.EMPTY;
            gasTank.tryApplyState(new GasTankState(EMPTY_LIMITS, GasStack.EMPTY)).requireAccepted();
            blockStateUpdated = updateCanisterBlockState(level, hatchState, CanisterType.EMPTY);
            if (!blockStateUpdated) {
                canister = previousCanister;
                gasTank.tryApplyState(previousState).requireAccepted();
                return ItemStack.EMPTY;
            }

            hatch.resetTransferQuota();
            hatch.setChanged();
            return removedCanister;
        }
        finally {
            boolean gasTankChanged = tankBehaviour.endMutation();
            if (blockStateUpdated && gasTankChanged) {
                tankBehaviour.sendDataImmediately();
            }
        }
    }

    private static GasTankLimits getCanisterLimits(AirtightHatchCanister canister) {
        return new GasTankLimits(canister.getTankVolume(0), GasPressureLimits.clampToHardLimit(canister.getTankMaxPressurePa(0)));
    }

    private static CanisterType toBlockCanisterType(HatchCanisterType canisterType) {
        if (canisterType != HatchCanisterType.CREATIVE) {
            return CanisterType.NORMAL;
        }

        return CanisterType.CREATIVE;
    }

    private static boolean isSameSnapshot(GasStack expectedGas, GasStack actualGas) {
        if (expectedGas.isEmpty() || actualGas.isEmpty()) {
            return expectedGas.isEmpty() && actualGas.isEmpty();
        }

        return expectedGas.getAmount() == actualGas.getAmount() && GasStack.isSameGasSameComponents(expectedGas, actualGas);
    }

    private boolean updateCanisterBlockState(Level level, BlockState hatchState, CanisterType canisterType) {
        return hatchState.getValue(AirtightHatchBlock.CANISTER_TYPE) == canisterType || level.setBlockAndUpdate(hatch.getBlockPos(), hatchState.setValue(AirtightHatchBlock.CANISTER_TYPE, canisterType));
    }
}
