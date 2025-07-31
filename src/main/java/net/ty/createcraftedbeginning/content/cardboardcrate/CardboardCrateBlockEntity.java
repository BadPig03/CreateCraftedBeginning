package net.ty.createcraftedbeginning.content.cardboardcrate;

import com.simibubi.create.content.logistics.crate.CrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import static net.ty.createcraftedbeginning.content.cardboardcrate.CardboardCrateBlock.MAX_SLOT;
import static net.ty.createcraftedbeginning.content.cardboardcrate.CardboardCrateBlock.SLOT_LIMIT;

public class CardboardCrateBlockEntity extends CrateBlockEntity {
    private static final int STORAGE_SLOT = 0;

    public CardboardCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private final ItemStackHandler inv = new ItemStackHandler(MAX_SLOT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }

        @Override
        public int getSlotLimit(int slot) {
            return SLOT_LIMIT;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            int toInsert = Math.min(stack.getCount(), inv.getSlotLimit(STORAGE_SLOT));
            if (!simulate) {
                inv.setStackInSlot(STORAGE_SLOT, stack.copyWithCount(toInsert));
            }
            return toInsert >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - toInsert);
        }
    };

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
        if (!clientPacket) {
            compound.put("Inventory", inv.serializeNBT(registries));
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (!clientPacket && compound.contains("Inventory")) {
            inv.deserializeNBT(registries, compound.getCompound("Inventory"));
        }
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CCBBlockEntities.CARDBOARD_CRATE.get(),
            (be, context) -> be.inv);
    }
}
