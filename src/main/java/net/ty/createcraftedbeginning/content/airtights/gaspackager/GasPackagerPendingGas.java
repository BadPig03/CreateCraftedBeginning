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
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
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
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack.copy();
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
        BalloonGasContents pendingContents = pendingGases.copy();
        if (pendingContents.isEmpty()) {
            return InsertionResult.NO_OP;
        }

        if (handler == null) {
            return new InsertionResult(copyOrEmpty(previouslyUnwrapped), false);
        }

        List<GasStack> gasStacks = pendingContents.copyGasStacks();
        if (gasStacks.size() > 1) {
            if (!handler.tryFillAtomically(gasStacks, GasAction.EXECUTE).isSuccess()) {
                return new InsertionResult(copyOrEmpty(previouslyUnwrapped), false);
            }
            return new InsertionResult(ItemStack.EMPTY, true);
        }

        List<GasStack> gasRemainders = new ArrayList<>();
        for (GasStack gas : gasStacks) {
            long filledAmount = handler.fill(gas.copy(), GasAction.EXECUTE);
            if (filledAmount < gas.getAmount()) {
                gasRemainders.add(gas.copyWithAmount(gas.getAmount() - filledAmount));
            }
        }

        BalloonGasContents remainingContents = new BalloonGasContents(gasRemainders);
        ItemStack returnedPackage = ItemStack.EMPTY;
        if (!remainingContents.isEmpty() && !previouslyUnwrapped.isEmpty()) {
            returnedPackage = previouslyUnwrapped.copy();
            BalloonUtils.setGasContents(returnedPackage, remainingContents);
        }

        return new InsertionResult(returnedPackage, true);
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (!CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_PENDING_GASES) || clientPacket) {
            return;
        }

        Tag pendingTag = CCBNbtUtils.getTag(compoundTag, COMPOUND_KEY_PENDING_GASES);
        pendingGases = pendingTag == null ? BalloonGasContents.EMPTY : BalloonGasContents.parseOptional(provider, pendingTag);
    }

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            return;
        }

        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_PENDING_GASES, pendingGases.saveOptional(provider));
    }

    void clear() {
        pendingGases = BalloonGasContents.EMPTY;
    }

    record InsertionResult(ItemStack returnedPackage, boolean inventoryChanged) {
        private static final InsertionResult NO_OP = new InsertionResult(ItemStack.EMPTY, false);
    }
}
