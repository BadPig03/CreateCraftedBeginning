package net.ty.createcraftedbeginning.content.airtights.gaspackager;

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
final class GasPackagerPendingGas {
    private static final String COMPOUND_KEY_PENDING_GAS = "PendingGas";

    private GasStack pendingGas = GasStack.EMPTY;

    private static ItemStack copyOrEmpty(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return stack.copy();
    }

    boolean isEmpty() {
        return pendingGas.isEmpty();
    }

    boolean canStage(ItemStack box, GasHandler handler, long sourcePressurePa) {
        GasStack gas = BalloonItem.getGas(box);
        return !gas.isEmpty() && BalloonGasTransferPlan.plan(handler, gas, sourcePressurePa, TransferPolicy.FULL_ONLY).acceptsEntireBalloon();
    }

    void stage(ItemStack box) {
        pendingGas = BalloonItem.getGas(box);
    }

    InsertionResult insertInto(@Nullable GasHandler handler, ItemStack previouslyUnwrapped, long sourcePressurePa) {
        GasStack gas = pendingGas.copy();
        if (gas.isEmpty()) {
            return InsertionResult.NO_OP;
        }

        if (handler == null || sourcePressurePa <= GasPressure.VACUUM_PA) {
            return new InsertionResult(copyOrEmpty(previouslyUnwrapped), false);
        }

        ExecutionResult execution = BalloonGasTransferPlan.plan(handler, gas, sourcePressurePa, TransferPolicy.FULL_ONLY).execute();
        if (!execution.complete()) {
            return new InsertionResult(copyOrEmpty(previouslyUnwrapped), false);
        }

        return new InsertionResult(ItemStack.EMPTY, execution.transferredAmount() > 0);
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

    record InsertionResult(ItemStack returnedPackage, boolean inventoryChanged) {
        private static final InsertionResult NO_OP = new InsertionResult(ItemStack.EMPTY, false);
    }
}
