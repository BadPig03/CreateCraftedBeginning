package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.SmartGasTank;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock.CanisterType;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHatchCanisterManager {
    private final AirtightHatchBlockEntity hatch;

    private ItemStack canister = ItemStack.EMPTY;

    AirtightHatchCanisterManager(AirtightHatchBlockEntity hatch) {
        this.hatch = hatch;
    }

    private static long getEffectiveCapacity(long configuredCapacity, long currentCapacity, long gasAmount) {
        long normalizedCapacity = Math.max(0, configuredCapacity);
        long normalizedGasAmount = Math.max(0, gasAmount);
        if (normalizedGasAmount <= normalizedCapacity) {
            return normalizedCapacity;
        }
        return Math.max(Math.max(0, currentCapacity), normalizedGasAmount);
    }

    ItemStack getStoredCanister() {
        return canister;
    }

    void setStoredCanister(ItemStack canister) {
        this.canister = canister;
    }

    ItemStack createCanisterItemStack() {
        ItemStack stack = canister.copyWithCount(1);
        if (stack.isEmpty() || !(stack.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents)) {
            return ItemStack.EMPTY;
        }

        stack.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, List.of(hatch.getHatchGasContent()));
        return stack;
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
        if (!(newCanister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents contents)) {
            return false;
        }

        CanisterType type = CanisterContainerSuppliers.isValidCreativeGasCanister(newCanister) ? CanisterType.CREATIVE : CanisterType.NORMAL;
        GasStack gas = contents.getGasInTank(0).copy();
        long capacity = getEffectiveCapacity(contents.getTankCapacity(0), 0, gas.getAmount());

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
            updated = level.setBlockAndUpdate(hatch.getBlockPos(), state.setValue(AirtightHatchBlock.CANISTER_TYPE, type));
            if (!updated) {
                canister = oldCanister;
                tank.setCapacity(oldCapacity);
                tank.setGasStack(oldGas);
                return false;
            }

            sourceStack.shrink(1);
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

    ItemStack removeCanister() {
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
            updated = level.setBlockAndUpdate(hatch.getBlockPos(), state.setValue(AirtightHatchBlock.CANISTER_TYPE, CanisterType.EMPTY));
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

    void updateCapacity(boolean syncImmediately) {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return;
        }

        SmartGasTank tank = hatch.getGasTankBehaviour().getPrimaryHandler();
        long capacity = getEffectiveCapacity(canisterContents.getTankCapacity(0), tank.getCapacity(), tank.getGasAmount());
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

    private GasStack getInternalGasContent() {
        return hatch.getGasTankBehaviour().getPrimaryHandler().getGasStack();
    }
}
