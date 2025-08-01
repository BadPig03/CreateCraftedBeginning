package net.ty.createcraftedbeginning.content.brasscrate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.crate.CrateBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.ty.createcraftedbeginning.content.brasscrate.BrassCrateBlock.MAX_SLOT;
import static net.ty.createcraftedbeginning.content.brasscrate.BrassCrateBlock.SLOT_LIMIT;

public class BrassCrateBlockEntity extends CrateBlockEntity {
    FilteringBehaviour filtering;

    final ItemStackHandler inv = new SafeItemStackHandler(this);

    public BrassCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private static class SafeItemStackHandler extends ItemStackHandler {
        private final BrassCrateBlockEntity parent;

        SafeItemStackHandler(BrassCrateBlockEntity parent) {
            super(MAX_SLOT);
            this.parent = parent;
        }

        @Override
        protected void onContentsChanged(int slot) {
            parent.setChanged();
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
        public int getSlotLimit(int slot) {
            return SLOT_LIMIT;
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (filtering == null) {
            filtering = createFilter();
            filtering.setLabel(Component.translatable("logistics.brass_crate.filter"));
        }
        behaviours.add(filtering);
    }

    private FilteringBehaviour createFilter() {
        return new FilteringBehaviour(this, new ValueBoxTransform() {
            @Override
            public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
                if (ms != null) {
                    TransformStack.of(ms).rotateXDegrees(90);
                }
            }

            @Override
            public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
                return new Vec3(0.5, 13.5 / 16d, 0.5);
            }

            @Override
            public float getScale() {
                return super.getScale();
            }
        });
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
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CCBBlockEntities.BRASS_CRATE.get(),
            (be, context) -> be.inv
        );
    }
}
