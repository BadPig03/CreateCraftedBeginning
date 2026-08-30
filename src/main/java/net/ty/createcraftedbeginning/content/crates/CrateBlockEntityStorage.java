package net.ty.createcraftedbeginning.content.crates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class CrateBlockEntityStorage {
    private static final String COMPOUND_KEY_INVENTORY = "Inventory";

    private final CrateItemStackHandler handler;
    private boolean clientSyncPending;

    CrateBlockEntityStorage(IntSupplier maxCountSupplier, Predicate<ItemStack> itemValidator, Runnable contentsChangedListener, @Nullable Predicate<ItemStack> trackedDiscardPredicate, Runnable trackedDiscardListener) {
        if (trackedDiscardPredicate == null) {
            handler = new CrateItemStackHandler(maxCountSupplier, itemValidator, contentsChangedListener);
            return;
        }

        handler = new DiscardingCrateItemStackHandler(maxCountSupplier, itemValidator, contentsChangedListener, trackedDiscardPredicate, trackedDiscardListener);
    }

    CrateItemStackHandler handler() {
        return handler;
    }

    ItemStack storedItem() {
        return handler.getStoredItem(0);
    }

    int storedCount() {
        return handler.getCountInSlot(0);
    }

    void setStoredItems(ItemStack content, int count) {
        handler.setStoredItems(0, content, count);
    }

    void requestClientSync() {
        clientSyncPending = true;
    }

    boolean consumeClientSyncRequest() {
        if (!clientSyncPending) {
            return false;
        }

        clientSyncPending = false;
        return true;
    }

    void write(CompoundTag compoundTag, Provider provider) {
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_INVENTORY, handler.serializeNBT(provider));
    }

    void read(CompoundTag compoundTag, Provider provider) {
        if (!CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_INVENTORY)) {
            return;
        }

        handler.deserializeNBT(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_INVENTORY));
    }
}
