package net.ty.createcraftedbeginning.content.andesitecrate;

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

import static net.ty.createcraftedbeginning.content.andesitecrate.AndesiteCrateBlock.MAX_SLOT;
import static net.ty.createcraftedbeginning.content.andesitecrate.AndesiteCrateBlock.SLOT_LIMIT;

public class AndesiteCrateBlockEntity extends CrateBlockEntity {
    public final ItemStackHandler inv = new ItemStackHandler(MAX_SLOT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }

            ItemStack sampleStack = null;

            for (int i = 0; i < MAX_SLOT; i++) {
                ItemStack current = getStackInSlot(i);
                if (current.isEmpty()) {
                    continue;
                }
                sampleStack = current;
                break;
            }

            if (sampleStack == null) {
                return true;
            }

            return ItemStack.isSameItemSameComponents(sampleStack, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return SLOT_LIMIT;
        }
    };

    public AndesiteCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CCBBlockEntities.ANDESITE_CRATE.get(), (be, context) -> be.inv);
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
}
