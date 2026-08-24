package net.ty.createcraftedbeginning.content.crates;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
    private CrateFilterController filterController;

    protected FilteredCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, IntSupplier maxCountSupplier) {
        super(type, pos, state, maxCountSupplier);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        filterController = new CrateFilterController();
        filterController.addBehaviour(this, behaviours);
    }

    @Override
    protected boolean canStoreItem(ItemStack stack) {
        return filterController == null || filterController.canStoreItem(stack);
    }

    public final ItemStack getFilterItem() {
        return filterController == null ? ItemStack.EMPTY : filterController.getFilterItem();
    }

    public final void setFilterItem(ItemStack filterItem) {
        if (filterController == null) {
            return;
        }

        filterController.setFilterItem(filterItem);
    }
}
