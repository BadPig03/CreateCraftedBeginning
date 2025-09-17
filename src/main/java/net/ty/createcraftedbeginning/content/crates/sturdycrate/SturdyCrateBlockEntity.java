package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.content.crates.CustomCrateBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlock.MAX_SLOT;
import static net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlock.SLOT_LIMIT;

public class SturdyCrateBlockEntity extends CustomCrateBlockEntity {
    private final ItemStackHandler inv;
    private FilteringBehaviour filtering;

    public SturdyCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inv = new SturdyCrateItemHandler(this);
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CCBBlockEntities.STURDY_CRATE.get(), (be, context) -> be.inv);
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        filtering = new FilteringBehaviour(this, new SturdySmartFilterSlot());
        behaviours.add(filtering);
    }

    public ItemStackHandler getInv() {
        return inv;
    }

    public FilteringBehaviour getFiltering() {
        return filtering;
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
        NonNullList<ItemStack> items = NonNullList.withSize(MAX_SLOT, ItemStack.EMPTY);
        for (int i = 0; i < MAX_SLOT; i++) {
            items.set(i, inv.getStackInSlot(i).copy());
        }
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));

        ItemStack filterItem = filtering.getFilter();
        if (filterItem.isEmpty()) {
            return;
        }

        stack.set(CCBDataComponents.FILTER_ITEM, ItemContainerContents.fromItems(Collections.singletonList(filterItem.copy())));
    }

    public void loadFromItem(@NotNull ItemStack stack) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return;
        }

        NonNullList<ItemStack> loadedItems = NonNullList.withSize(MAX_SLOT, ItemStack.EMPTY);
        contents.copyInto(loadedItems);

        for (int i = 0; i < MAX_SLOT; i++) {
            inv.setStackInSlot(i, loadedItems.get(i));
        }

        ItemContainerContents filterContents = stack.get(CCBDataComponents.FILTER_ITEM);
        if (filterContents == null || filterContents.getSlots() != 1) {
            return;
        }
        filtering.setFilter(filterContents.getStackInSlot(0));
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

    private class SturdyCrateItemHandler extends ItemStackHandler {
        private final SturdyCrateBlockEntity parent;

        SturdyCrateItemHandler(SturdyCrateBlockEntity parent) {
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
            if (!stack.getItem().canFitInsideContainerItems()) {
                return false;
            }

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

    class SturdySmartFilterSlot extends ValueBoxTransform {
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
