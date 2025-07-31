package net.ty.createcraftedbeginning.content.sturdycrate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.crate.CrateBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.ty.createcraftedbeginning.content.sturdycrate.SturdyCrateBlock.MAX_SLOT;
import static net.ty.createcraftedbeginning.content.sturdycrate.SturdyCrateBlock.SLOT_LIMIT;

public class SturdyCrateBlockEntity extends CrateBlockEntity {
    FilteringBehaviour filtering;

    final ItemStackHandler inv = new SafeItemStackHandler(this);

    public SturdyCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private static class SafeItemStackHandler extends ItemStackHandler {
        private final SturdyCrateBlockEntity parent;

        SafeItemStackHandler(SturdyCrateBlockEntity parent) {
            super(MAX_SLOT);
            this.parent = parent;
        }

        @Override
        protected void onContentsChanged(int slot) {
            parent.setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (!stack.getItem().canFitInsideContainerItems()) {
                return false;
            }

            if (parent.filtering == null) {
                return true;
            }
            return parent.filtering.test(stack);
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
            filtering.setLabel(Component.translatable("logistics.sturdy_crate.filter"));
        }
        behaviours.add(filtering);
    }

    public FilteringBehaviour createFilter() {
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

    public boolean isEmpty() {
        for (int i = 0; i < MAX_SLOT; i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void saveToItem(ItemStack stack) {
        NonNullList<ItemStack> allItems = NonNullList.withSize(MAX_SLOT + 1, ItemStack.EMPTY);

        for (int i = 0; i < MAX_SLOT; i++) {
            ItemStack slotStack = inv.getStackInSlot(i);
            allItems.set(i, slotStack.copy());
        }

        if (filtering != null && !filtering.getFilter().isEmpty()) {
            allItems.set(MAX_SLOT, filtering.getFilter().copyWithCount(1));
        }

        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(allItems));
    }

    public void loadFromItem(ItemStack stack) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return;
        }

        NonNullList<ItemStack> loadedItems = NonNullList.withSize(MAX_SLOT + 1, ItemStack.EMPTY);
        contents.copyInto(loadedItems);

        for (int i = 0; i < MAX_SLOT; i++) {
            inv.setStackInSlot(i, loadedItems.get(i));
        }

        if (filtering == null) {
            filtering = createFilter();
        }
        ItemStack filter = loadedItems.get(MAX_SLOT).copy();
        filtering.setFilter(filter);
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
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CCBBlockEntities.STURDY_CRATE.get(),
            (be, context) -> be.inv
        );
    }
}
