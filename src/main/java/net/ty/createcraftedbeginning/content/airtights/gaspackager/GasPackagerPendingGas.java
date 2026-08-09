package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasContents;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasPackagerPendingGas {
    private static final String COMPOUND_KEY_PENDING_GASES = "PendingGases";

    private BalloonGasContents pendingGases = BalloonGasContents.EMPTY;

    private static ItemStack copyOrEmpty(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    boolean isEmpty() {
        return pendingGases.isEmpty();
    }

    boolean canStage(ItemStack box, IGasHandler handler) {
        if (!BalloonUtils.containsGasContents(box)) {
            return false;
        }

        BalloonGasContents contents = BalloonUtils.getGasContents(box);
        return !contents.isEmpty() && BalloonUtils.fitsInBalloon(contents) && GasPackagerUtils.canInsertAll(handler, contents);
    }

    void stage(ItemStack box) {
        pendingGases = BalloonUtils.getGasContents(box).copy();
    }

    InsertionResult insertInto(@Nullable IGasHandler handler, ItemStack previouslyUnwrapped) {
        BalloonGasContents contents = pendingGases.copy();
        if (contents.isEmpty()) {
            return InsertionResult.NO_OP;
        }

        if (handler == null) {
            return new InsertionResult(copyOrEmpty(previouslyUnwrapped), false);
        }

        List<GasStack> gases = contents.copyGasStacks();
        if (gases.size() > 1) {
            if (!handler.tryFillAtomically(gases, GasAction.EXECUTE).isSuccess()) {
                return new InsertionResult(copyOrEmpty(previouslyUnwrapped), false);
            }
            return new InsertionResult(ItemStack.EMPTY, true);
        }

        List<GasStack> remainders = new ArrayList<>();
        for (GasStack gas : gases) {
            long filled = handler.fill(gas.copy(), GasAction.EXECUTE);
            if (filled < gas.getAmount()) {
                remainders.add(gas.copyWithAmount(gas.getAmount() - filled));
            }
        }

        BalloonGasContents remainderContents = new BalloonGasContents(remainders);
        ItemStack returned = ItemStack.EMPTY;
        if (!remainderContents.isEmpty() && !previouslyUnwrapped.isEmpty()) {
            returned = previouslyUnwrapped.copy();
            BalloonUtils.setGasContents(returned, remainderContents);
        }

        return new InsertionResult(returned, true);
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (!compoundTag.contains(COMPOUND_KEY_PENDING_GASES) || clientPacket) {
            return;
        }

        Tag pendingTag = compoundTag.get(COMPOUND_KEY_PENDING_GASES);
        pendingGases = pendingTag == null ? BalloonGasContents.EMPTY : BalloonGasContents.parseOptional(provider, pendingTag);
    }

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_PENDING_GASES, pendingGases.saveOptional(provider));
    }

    void clear() {
        pendingGases = BalloonGasContents.EMPTY;
    }

    record InsertionResult(ItemStack returnedPackage, boolean inventoryChanged) {
        private static final InsertionResult NO_OP = new InsertionResult(ItemStack.EMPTY, false);
    }
}
