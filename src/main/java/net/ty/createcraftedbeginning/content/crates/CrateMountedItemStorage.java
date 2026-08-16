package net.ty.createcraftedbeginning.content.crates;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CrateMountedItemStorage<B extends CratesBlockEntity> extends MountedItemStorage {
    protected final Class<B> blockEntityClass;
    protected final CrateItemStackHandler handler;

    protected CrateMountedItemStorage(MountedItemStorageType<?> type, Class<B> blockEntityClass, ItemStack content, int count, IntSupplier maxCountSupplier) {
        this(type, blockEntityClass, content, count, maxCountSupplier, null);
    }

    protected CrateMountedItemStorage(MountedItemStorageType<?> type, Class<B> blockEntityClass, ItemStack content, int count, IntSupplier maxCountSupplier, @Nullable Predicate<ItemStack> trackedDiscardPredicate) {
        super(type);
        this.blockEntityClass = blockEntityClass;
        if (trackedDiscardPredicate == null) {
            handler = new CrateItemStackHandler(maxCountSupplier, this::canStoreItem, this::onContentsChanged);
        }
        else {
            handler = new DiscardingCrateItemStackHandler(maxCountSupplier, this::canStoreItem, this::onContentsChanged, trackedDiscardPredicate, this::onTrackedItemDiscarded);
        }
        handler.initializeStoredItems(content, count);
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!blockEntityClass.isInstance(be)) {
            return;
        }

        B crate = blockEntityClass.cast(be);
        crate.setStoredItems(getStoredItem(), getStoredCount());
        afterUnmount(crate);
    }

    protected void afterUnmount(B crate) {
    }

    protected boolean canStoreItem(ItemStack stack) {
        return true;
    }

    protected void onContentsChanged() {
    }

    protected void onTrackedItemDiscarded() {
    }

    @Override
    public int getSlots() {
        return handler.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return handler.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return handler.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return handler.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return handler.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return handler.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        handler.setStackInSlot(slot, stack);
    }

    public final ItemStack getStoredItem() {
        return handler.getStoredItem(0);
    }

    public final int getStoredCount() {
        return handler.getCountInSlot(0);
    }
}
