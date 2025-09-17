package net.ty.createcraftedbeginning.content.crates.andesitecrate;

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

import static net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateBlock.MAX_SLOT;
import static net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateBlock.SLOT_LIMIT;

public class AndesiteCrateBlockEntity extends CustomCrateBlockEntity {
    private final ItemStackHandler inv;

    public AndesiteCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inv = new AndesiteItemHandler();
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CCBBlockEntities.ANDESITE_CRATE.get(), (be, context) -> be.inv);
    }

    public ItemStackHandler getInv() {
        return inv;
    }

    public void setStoredItems(ItemStack[] stacks) {
        for (int i = 0; i < MAX_SLOT && i < stacks.length; i++) {
            inv.setStackInSlot(i, stacks[i].copy());
        }
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

    private class AndesiteItemHandler extends ItemStackHandler {
        AndesiteItemHandler() {
            super(MAX_SLOT);
        }

        @Override
        public int getSlotLimit(int slot) {
            return SLOT_LIMIT;
        }

        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return SLOT_LIMIT;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }

            ItemStack reference = null;
            for (int i = 0; i < MAX_SLOT; i++) {
                ItemStack current = getStackInSlot(i);
                if (current.isEmpty()) {
                    continue;
                }
                reference = current;
                break;
            }

            return reference == null || ItemStack.isSameItemSameComponents(reference, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    }
}
