package net.ty.createcraftedbeginning.content.crates;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CratesBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ThresholdSwitchObservable {
    protected final CrateBlockEntityStorage storage;
    protected final CrateDisplay display;

    protected CratesBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, IntSupplier maxCountSupplier) {
        this(type, pos, state, maxCountSupplier, null);
    }

    protected CratesBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, IntSupplier maxCountSupplier, @Nullable Predicate<ItemStack> trackedDiscardPredicate) {
        super(type, pos, state);
        storage = new CrateBlockEntityStorage(maxCountSupplier, this::canStoreItem, this::onInventoryChanged, trackedDiscardPredicate, this::onTrackedItemDiscarded);
        display = new CrateDisplay(storage);
    }

    public static <T extends CratesBlockEntity> void registerCapabilities(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(ItemHandler.BLOCK, type, (be, context) -> be.getHandler());
    }

    public final CrateItemStackHandler getHandler() {
        return storage.handler();
    }

    public final ItemStack getStoredItem() {
        return storage.storedItem();
    }

    public final int getStoredCount() {
        return storage.storedCount();
    }

    public final void setStoredItems(ItemStack content, int count) {
        storage.setStoredItems(content, count);
    }

    protected boolean canStoreItem(ItemStack stack) {
        return true;
    }

    protected void onInventoryChanged() {
        notifyUpdate();
    }

    protected void onTrackedItemDiscarded() {
    }

    @Override
    public void sendData() {
        if (storage == null || level == null || level.isClientSide) {
            return;
        }

        storage.requestClientSync();
    }

    @Override
    public int getMaxValue() {
        return display.maxValue();
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return display.currentValue();
    }

    @Override
    public MutableComponent format(int value) {
        return display.format(value);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        display.addToGoggleTooltip(tooltip);
        return true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !storage.consumeClientSyncRequest()) {
            return;
        }

        super.sendData();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        storage.write(compoundTag, provider);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        storage.read(compoundTag, provider);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }
}
