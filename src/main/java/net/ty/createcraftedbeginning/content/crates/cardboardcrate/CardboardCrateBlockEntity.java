package net.ty.createcraftedbeginning.content.crates.cardboardcrate;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import net.ty.createcraftedbeginning.content.crates.CratesBlockEntity;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CardboardCrateBlockEntity extends CratesBlockEntity implements IHaveGoggleInformation, ThresholdSwitchObservable {
    private static final String COMPOUND_KEY_INVENTORY = "Inventory";
    private final CrateItemStackHandler handler;
    private CCBAdvancementBehaviour advancementBehaviour;

    public CardboardCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        handler = new CardboardItemHandler();
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.CARDBOARD_CRATE.get(), (be, context) -> be.handler);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_INVENTORY, handler.serializeNBT(provider));
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!compoundTag.contains(COMPOUND_KEY_INVENTORY)) {
            return;
        }

        handler.deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_INVENTORY));
    }

    @Override
    public CrateItemStackHandler getHandler() {
        return handler;
    }

    @Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
	}

    @Override
    public void setStoredItems(ItemStack content, int count) {
        handler.setStackInSlot(0, content);
        handler.setCountInSlot(0, count);
        notifyUpdate();
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.BORN_FROM_THE_SAME_ROOT);
        behaviours.add(advancementBehaviour);
    }

    @Override
    public int getMaxValue() {
        return handler.getSlotLimit(0);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return handler.getCountInSlot(0);
    }

    @Override
    public MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.items")).component();
    }

    private class CardboardItemHandler extends CrateItemStackHandler {
        CardboardItemHandler() {
            super(CCBConfig.server().crates.maxCardboardCapacity.get(), null);
        }

        private static boolean awardAdvancement(@NotNull ItemStack stack) {
            return stack.getItem() instanceof PackageItem;
        }

        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            validateSlotIndex(slot);
            return slot == 1 || content.isEmpty() || count == 0 ? ItemStack.EMPTY : content.copyWithCount(count);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            validateSlotIndex(slot);
            if (slot == 1) {
                return;
            }

            content = stack;
            count = stack.isEmpty() ? 0 : 1;
            onContentsChanged(slot);
        }

        @Override
        public int getCountInSlot(int slot) {
            validateSlotIndex(slot);
            return slot == 1 || content.isEmpty() ? 0 : count;
        }

        @Override
        public void setCountInSlot(int slot, int newCount) {
            validateSlotIndex(slot);
            if (slot == 1 || content.isEmpty()) {
                return;
            }

            count = newCount;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            if (content.isEmpty()) {
                int newCount = Math.min(stack.getCount(), maxCount);
                if (!simulate) {
                    content = stack.copyWithCount(1);
                    count = newCount;
                    onContentsChanged(slot);
                }
                return ItemStack.EMPTY;
            }

            if (ItemStack.isSameItemSameComponents(content, stack)) {
                int space = maxCount - count;
                if (space <= 0) {
                    return ItemStack.EMPTY;
                }

                int toInsert = Math.min(stack.getCount(), space);
                if (!simulate) {
                    count += toInsert;
                    onContentsChanged(slot);
                }

                return ItemStack.EMPTY;
            }

            if (!simulate) {
                if (awardAdvancement(content)) {
                    advancementBehaviour.awardPlayer(CCBAdvancements.BORN_FROM_THE_SAME_ROOT);
                }
                content = stack.copyWithCount(1);
                count = Math.min(stack.getCount(), maxCount);
                onContentsChanged(slot);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            validateSlotIndex(slot);
            return slot == 1 ? 0 : maxCount;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            validateSlotIndex(slot);
            return true;
        }

        @Override
        protected void validateSlotIndex(int slot) {
            if (slot >= 0 && slot <= 1) {
                return;
            }

            throw new RuntimeException("Slot " + slot + " not in valid range - [0,2)");
        }

        @Override
        protected void onContentsChanged(int slot) {
            notifyUpdate();
        }
    }
}
