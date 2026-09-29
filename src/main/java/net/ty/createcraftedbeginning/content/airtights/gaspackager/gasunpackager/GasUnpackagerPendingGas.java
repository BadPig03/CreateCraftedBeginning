package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasunpackager;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasTransferPlan;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasTransferPlan.ExecutionResult;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasTransferPlan.TransferPolicy;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasUnpackagerPendingGas {
    private static final String COMPOUND_KEY_PENDING_GAS = "PendingGas";

    private GasStack pendingGas = GasStack.EMPTY;

    boolean isEmpty() {
        return pendingGas.isEmpty();
    }

    boolean canStage(ItemStack balloon, GasHandler handler, long sourcePressurePa) {
        GasStack gas = BalloonItem.getGas(balloon);
        return !gas.isEmpty() && BalloonGasTransferPlan.plan(handler, gas, sourcePressurePa, TransferPolicy.BEST_EFFORT).canTransfer();
    }

    void stage(ItemStack balloon) {
        pendingGas = BalloonItem.getGas(balloon);
    }

    InsertionResult insertInto(@Nullable GasHandler handler, ItemStack previouslyUnwrapped, long sourcePressurePa) {
        GasStack gas = pendingGas.copy();
        if (gas.isEmpty()) {
            return InsertionResult.NO_OP;
        }

        ItemStack returnedBalloon = copyBalloon(previouslyUnwrapped);
        if (handler == null || sourcePressurePa <= GasPressure.VACUUM_PA || returnedBalloon.isEmpty()) {
            return new InsertionResult(returnedBalloon, false, 0);
        }

        ExecutionResult execution = BalloonGasTransferPlan.plan(handler, gas, sourcePressurePa, TransferPolicy.BEST_EFFORT).execute();
        long transferredAmount = Math.min(gas.getAmount(), execution.transferredAmount());
        if (transferredAmount <= 0) {
            return new InsertionResult(returnedBalloon, false, 0);
        }

        long remainingAmount = gas.getAmount() - transferredAmount;
        if (remainingAmount <= 0) {
            returnedBalloon = ItemStack.EMPTY;
        }
        else {
            BalloonItem.setGas(returnedBalloon, gas.copyWithAmount(remainingAmount));
        }
        return new InsertionResult(returnedBalloon, true, transferredAmount);
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (!compoundTag.contains(COMPOUND_KEY_PENDING_GAS) || clientPacket) {
            return;
        }

        Tag pendingTag = compoundTag.get(COMPOUND_KEY_PENDING_GAS);
        pendingGas = pendingTag == null ? GasStack.EMPTY : GasStack.parse(provider, pendingTag).orElse(GasStack.EMPTY);
    }

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_PENDING_GAS, pendingGas.saveOptional(provider));
    }

    void clear() {
        pendingGas = GasStack.EMPTY;
    }

    private static ItemStack copyBalloon(ItemStack stack) {
        if (!BalloonItem.isBalloon(stack)) {
            return ItemStack.EMPTY;
        }

        return stack.copyWithCount(1);
    }

    record InsertionResult(ItemStack returnedBalloon, boolean inventoryChanged, long transferredAmount) {
        private static final InsertionResult NO_OP = new InsertionResult(ItemStack.EMPTY, false, 0);
    }
}
