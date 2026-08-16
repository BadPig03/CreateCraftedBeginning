package net.ty.createcraftedbeginning.content.crates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CrateBlockEntityStorage {
    private static final String COMPOUND_KEY_INVENTORY = "Inventory";

    private final CrateItemStackHandler handler;
    private boolean clientSyncPending;

    public CrateBlockEntityStorage(IntSupplier maxCountSupplier, Predicate<ItemStack> itemValidator, Runnable contentsChangedListener, @Nullable Predicate<ItemStack> trackedDiscardPredicate, Runnable trackedDiscardListener) {
        if (trackedDiscardPredicate == null) {
            handler = new CrateItemStackHandler(maxCountSupplier, itemValidator, contentsChangedListener);
            return;
        }

        handler = new DiscardingCrateItemStackHandler(maxCountSupplier, itemValidator, contentsChangedListener, trackedDiscardPredicate, trackedDiscardListener);
    }

    public CrateItemStackHandler handler() {
        return handler;
    }

    public ItemStack storedItem() {
        return handler.getStoredItem(0);
    }

    public int storedCount() {
        return handler.getCountInSlot(0);
    }

    public void setStoredItems(ItemStack content, int count) {
        handler.setStoredItems(0, content, count);
    }

    public void requestClientSync() {
        clientSyncPending = true;
    }

    public boolean consumeClientSyncRequest() {
        if (!clientSyncPending) {
            return false;
        }

        clientSyncPending = false;
        return true;
    }

    public void write(CompoundTag tag, Provider provider) {
        tag.put(COMPOUND_KEY_INVENTORY, handler.serializeNBT(provider));
    }

    public void read(CompoundTag tag, Provider provider) {
        if (!tag.contains(COMPOUND_KEY_INVENTORY)) {
            return;
        }

        handler.deserializeNBT(provider, tag.getCompound(COMPOUND_KEY_INVENTORY));
    }
}
