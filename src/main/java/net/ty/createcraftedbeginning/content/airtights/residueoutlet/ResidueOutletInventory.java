package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import com.simibubi.create.foundation.blockEntity.ItemHandlerContainer;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class ResidueOutletInventory extends ItemHandlerContainer implements IItemHandlerModifiable, INBTSerializable<CompoundTag> {
	private static final int MAX_SIZE = 1;
	private static final String COMPOUND_KEY_PARTIAL_ITEM_COUNT = "PartialItemCount";

	private float partialItemCount;

    public ResidueOutletInventory(ResidueOutletBlockEntity outlet) {
        super(new InternalStackHandler(outlet));
    }

    @Override
	public int getSlots() {
		return MAX_SIZE;
	}

    @Override
	public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return stack;
	}

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return inv.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inv.getSlotLimit(slot);
    }

    @Override
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return inv.isItemValid(slot, stack);
	}

    @Override
	public @NotNull ItemStack getStackInSlot(int slot) {
		return inv.getStackInSlot(slot);
	}

    @Override
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		inv.setStackInSlot(slot, stack);
	}

    @Override
	public CompoundTag serializeNBT(@NotNull Provider provider) {
		CompoundTag compoundTag = ((InternalStackHandler) inv).serializeNBT(provider);
		compoundTag.putFloat(COMPOUND_KEY_PARTIAL_ITEM_COUNT, partialItemCount);
		return compoundTag;
	}

    @Override
	public void deserializeNBT(@NotNull Provider provider, @NotNull CompoundTag compoundTag) {
		((InternalStackHandler) inv).deserializeNBT(provider, compoundTag);
        if (!compoundTag.contains(COMPOUND_KEY_PARTIAL_ITEM_COUNT)) {
            return;
        }

        partialItemCount = compoundTag.getFloat(COMPOUND_KEY_PARTIAL_ITEM_COUNT);
    }

    public IItemHandler getCapability() {
		return inv;
	}

	public boolean addPartialItemCount(float count, ItemStack itemStack) {
		partialItemCount += count;
		int intPart = (int) partialItemCount;
		if (intPart > 0) {
			ItemStack inserted = inv.insertItem(0, itemStack.copyWithCount(intPart), false);
			if (inserted.isEmpty()) {
				partialItemCount -= intPart;
				return true;
			}
			else {
				return false;
			}
        }
        return true;
    }

    public static class InternalStackHandler extends ItemStackHandler {
        private final ResidueOutletBlockEntity outlet;

        public InternalStackHandler(ResidueOutletBlockEntity outlet) {
			super(MAX_SIZE);
			this.outlet = outlet;
		}

        @Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			outlet.notifyUpdate();
		}
    }
}
