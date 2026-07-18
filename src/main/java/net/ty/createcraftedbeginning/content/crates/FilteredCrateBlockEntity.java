package net.ty.createcraftedbeginning.content.crates;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.IntSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class FilteredCrateBlockEntity extends CratesBlockEntity {
    private FilteringBehaviour filteringBehaviour;

    protected FilteredCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, IntSupplier maxCountSupplier) {
        super(type, pos, state, maxCountSupplier);
    }

    public final ItemStack getFilterItem() {
        if (filteringBehaviour == null) {
            return ItemStack.EMPTY;
        }

        ItemStack filter = filteringBehaviour.getFilter();
        return filter.isEmpty() ? ItemStack.EMPTY : filter.copyWithCount(1);
    }

    public final void setFilterItem(ItemStack filterItem) {
        if (filteringBehaviour == null) {
            return;
        }

        filteringBehaviour.setFilter(filterItem.isEmpty() ? ItemStack.EMPTY : filterItem.copyWithCount(1));
    }

    @Override
    protected boolean canStoreItem(ItemStack stack) {
        ItemStack filterItem = getFilterItem();
        return filterItem.isEmpty() || FilterItem.testDirect(filterItem, stack, false);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        filteringBehaviour = new FilteringBehaviour(this, new CrateFilterSlot());
        behaviours.add(filteringBehaviour);
    }
}
