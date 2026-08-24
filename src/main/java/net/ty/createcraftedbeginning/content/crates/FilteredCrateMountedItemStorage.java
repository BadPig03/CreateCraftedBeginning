package net.ty.createcraftedbeginning.content.crates;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.content.logistics.filter.FilterItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.IntSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FilteredCrateMountedItemStorage<B extends FilteredCrateBlockEntity> extends CrateMountedItemStorage<B> {
    private final ItemStack filterItem;

    protected FilteredCrateMountedItemStorage(MountedItemStorageType<?> type, Class<B> blockEntityClass, ItemStack content, int count, ItemStack filterItem, IntSupplier maxCountSupplier) {
        super(type, blockEntityClass, content, count, maxCountSupplier);
        this.filterItem = filterItem.isEmpty() ? ItemStack.EMPTY : filterItem.copyWithCount(1);
    }

    @Override
    protected boolean canStoreItem(ItemStack stack) {
        return (filterItem.isEmpty() || FilterItem.testDirect(filterItem, stack, false)) && super.canStoreItem(stack);
    }

    protected final ItemStack getFilterItem() {
        return filterItem.isEmpty() ? ItemStack.EMPTY : filterItem.copy();
    }
}
