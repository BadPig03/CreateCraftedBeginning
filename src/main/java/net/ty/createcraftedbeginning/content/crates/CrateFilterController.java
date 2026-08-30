package net.ty.createcraftedbeginning.content.crates;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class CrateFilterController {
    private FilteringBehaviour filteringBehaviour;

    ItemStack getFilterItem() {
        if (filteringBehaviour == null) {
            return ItemStack.EMPTY;
        }

        ItemStack filterItem = filteringBehaviour.getFilter();
        if (filterItem.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return filterItem.copyWithCount(1);
    }

    void setFilterItem(ItemStack filterItem) {
        if (filteringBehaviour == null) {
            return;
        }

        ItemStack filter = filterItem.isEmpty() ? ItemStack.EMPTY : filterItem.copyWithCount(1);
        filteringBehaviour.setFilter(filter);
    }

    boolean canStoreItem(ItemStack stack) {
        if (filteringBehaviour == null) {
            return true;
        }

        ItemStack filterItem = filteringBehaviour.getFilter();
        return filterItem.isEmpty() || FilterItem.testDirect(filterItem, stack, false);
    }

    void addBehaviour(FilteredCrateBlockEntity blockEntity, List<BlockEntityBehaviour> behaviours) {
        filteringBehaviour = new FilteringBehaviour(blockEntity, new CrateFilterSlot());
        behaviours.add(filteringBehaviour);
    }
}
