package net.ty.createcraftedbeginning.content.crates.cardboardcrate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.content.crates.CustomCrateBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import static net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateBlock.MAX_SLOT;
import static net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateBlock.SLOT_LIMIT;

public class CardboardCrateBlockEntity extends CustomCrateBlockEntity {
    private static final int STORAGE_SLOT = 0;
    private final ItemStackHandler inv;

    public CardboardCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inv = new CardboardCrateItemHandler();
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CCBBlockEntities.CARDBOARD_CRATE.get(), (be, context) -> be.inv);
    }

    public ItemStack getStoredItem() {
        return inv.getStackInSlot(STORAGE_SLOT).copy();
    }

    public void setStoredItem(ItemStack stack) {
        inv.setStackInSlot(STORAGE_SLOT, stack);
        setChanged();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (clientPacket) {
            return;
        }
        compound.put("Inventory", inv.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (clientPacket || !compound.contains("Inventory")) {
            return;
        }
        inv.deserializeNBT(registries, compound.getCompound("Inventory"));
    }

    private class CardboardCrateItemHandler extends ItemStackHandler {
        CardboardCrateItemHandler() {
            super(MAX_SLOT);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            ItemStack current = getStackInSlot(STORAGE_SLOT);

            if (current.isEmpty()) {
                int maxInsert = Math.min(stack.getCount(), SLOT_LIMIT);
                if (!simulate) {
                    setStackInSlot(STORAGE_SLOT, stack.copyWithCount(maxInsert));
                }
                return maxInsert >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - maxInsert);
            }

            if (ItemStack.isSameItemSameComponents(current, stack)) {
                int maxStackSize = Math.min(current.getMaxStackSize(), SLOT_LIMIT);
                int availableSpace = maxStackSize - current.getCount();

                if (availableSpace <= 0) {
                    return ItemStack.EMPTY;
                }

                int toInsert = Math.min(stack.getCount(), availableSpace);
                if (!simulate) {
                    current.grow(toInsert);
                    setStackInSlot(STORAGE_SLOT, current);
                }
                return toInsert >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - toInsert);
            }

            if (!simulate) {
                int maxInsert = Math.min(stack.getCount(), SLOT_LIMIT);
                setStackInSlot(STORAGE_SLOT, stack.copyWithCount(maxInsert));
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == STORAGE_SLOT ? SLOT_LIMIT : 0;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    }
}
