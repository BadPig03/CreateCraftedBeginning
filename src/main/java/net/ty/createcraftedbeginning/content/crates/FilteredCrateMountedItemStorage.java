package net.ty.createcraftedbeginning.content.crates;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.IntSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FilteredCrateMountedItemStorage<B extends FilteredCrateBlockEntity> extends CrateMountedItemStorage<B> {
    private final FilterItemStack filter;
    private @Nullable Level level;

    protected FilteredCrateMountedItemStorage(MountedItemStorageType<?> type, Class<B> blockEntityClass, ItemStack content, int count, ItemStack filterItem, IntSupplier maxCountSupplier) {
        super(type, blockEntityClass, content, count, maxCountSupplier);
        filter = FilterItemStack.of(filterItem.copyWithCount(1));
    }

    public static void bindLevel(MountedStorageManager storageManager, Level level) {
        for (MountedItemStorage storage : storageManager.getAllItemStorages().values()) {
            if (!(storage instanceof FilteredCrateMountedItemStorage<?> crate)) {
                continue;
            }

            crate.level = level;
        }
    }

    @Override
    protected boolean canStoreItem(ItemStack stack) {
        return (level != null || !filter.isFilterItem()) && filter.test(level, stack) && super.canStoreItem(stack);
    }

    protected final ItemStack getFilterItem() {
        ItemStack filterItem = filter.item();
        if (filterItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return filterItem.copy();
    }

    protected final void setLevel(@Nullable Level level) {
        this.level = level;
    }
}
