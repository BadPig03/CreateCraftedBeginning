package net.ty.createcraftedbeginning.content.crates.andesitecrate;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import net.ty.createcraftedbeginning.content.crates.CratesBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class AndesiteCrateBlockEntity extends CratesBlockEntity implements IHaveGoggleInformation, ThresholdSwitchObservable {
    private static final String COMPOUND_KEY_INVENTORY = "Inventory";

    private final CrateItemStackHandler handler;

    public AndesiteCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        handler = new AndesiteItemHandler();
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.ANDESITE_CRATE.get(), (be, context) -> be.handler);
    }

    @Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
	}

    @Override
    public CrateItemStackHandler getHandler() {
        return handler;
    }

    @Override
    public void setStoredItems(ItemStack content, int count) {
        handler.setStackInSlot(0, content);
        handler.setCountInSlot(0, count);
        notifyUpdate();
    }

    @Override
    protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put(COMPOUND_KEY_INVENTORY, handler.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (!compound.contains(COMPOUND_KEY_INVENTORY)) {
            return;
        }

        handler.deserializeNBT(registries, compound.getCompound(COMPOUND_KEY_INVENTORY));
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
        return CreateLang.text(value + " ").add(CreateLang.translate("schedule.condition.threshold.items")).component();
    }

    private class AndesiteItemHandler extends CrateItemStackHandler {
        AndesiteItemHandler() {
            super(CCBConfig.server().crates.maxAndesiteCapacity.get(), null);
        }

        @Override
        protected void onContentsChanged(int slot) {
            notifyUpdate();
        }
    }
}
