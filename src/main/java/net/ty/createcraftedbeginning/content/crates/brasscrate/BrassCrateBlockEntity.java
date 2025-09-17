package net.ty.createcraftedbeginning.content.crates.brasscrate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.content.crates.CustomCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlock;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateBlock.MAX_SLOT;
import static net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateBlock.SLOT_LIMIT;

public class BrassCrateBlockEntity extends CustomCrateBlockEntity {
    private final ItemStackHandler inv;
    private FilteringBehaviour filtering;

    public BrassCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inv = new BrassCrateItemHandler(this);
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CCBBlockEntities.BRASS_CRATE.get(), (be, context) -> be.inv);
    }

    public FilteringBehaviour getFiltering() {
        return filtering;
    }

    public ItemStackHandler getInv() {
        return inv;
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        filtering = new FilteringBehaviour(this, new BrassSmartFilterSlot());
        behaviours.add(filtering);
    }

    public void setStoredItems(ItemStack[] stacks) {
        if (stacks == null || stacks.length == 0) {
            return;
        }

        int max = Math.min(MAX_SLOT, stacks.length);
        for (int i = 0; i < max; i++) {
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

    private class BrassCrateItemHandler extends ItemStackHandler {
        private final BrassCrateBlockEntity parent;

        BrassCrateItemHandler(BrassCrateBlockEntity parent) {
            super(MAX_SLOT);
            this.parent = parent;
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
            if (parent.filtering != null && !parent.filtering.test(stack)) {
                return false;
            }

            if (!stack.isEmpty()) {
                ItemStack reference = null;
                for (int i = 0; i < getSlots(); i++) {
                    ItemStack inSlot = getStackInSlot(i);
                    if (!inSlot.isEmpty()) {
                        reference = inSlot;
                        break;
                    }
                }

                return reference == null || ItemStack.isSameItemSameComponents(reference, stack);
            }
            return true;
        }

        @Override
        protected void onContentsChanged(int slot) {
            parent.setChanged();
        }
    }

    class BrassSmartFilterSlot extends ValueBoxTransform {
        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            return new Vec3(0.5, 13.5 / 16d, 0.5);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, @NotNull BlockState state, PoseStack ms) {
            Direction facing = state.getValue(SturdyCrateBlock.FACING);
            TransformStack.of(ms).rotateXDegrees(90).rotateZDegrees(facing.getOpposite().toYRot());
        }
    }
}
