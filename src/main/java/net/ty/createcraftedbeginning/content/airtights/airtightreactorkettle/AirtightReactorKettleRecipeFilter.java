package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterUtils;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightReactorKettleRecipeFilter {
    private AirtightReactorKettleRecipeFilter() {
    }

    static @Nullable FilteringBehaviour getBehaviour(AirtightReactorKettleBlockEntity kettle) {
        BlockPos filterPos = kettle.getBlockPos().below().north();
        Level level = kettle.getLevel();
        if (level == null || !(level.getBlockEntity(filterPos) instanceof AirtightReactorKettleStructuralBlockEntity structural)) {
            return null;
        }
        return structural.getFilteringBehaviour();
    }

    static boolean matches(AirtightReactorKettleBlockEntity kettle, ReactorKettleRecipe recipe) {
        FilteringBehaviour filter = getBehaviour(kettle);
        Level level = kettle.getLevel();
        if (filter == null || level == null) {
            return false;
        }

        ItemStack filterItem = filter.getFilter();
        if (GasFilterUtils.isFilter(filterItem) && !recipe.getGasResults().isEmpty()) {
            return GasFilterUtils.matches(filterItem, recipe.getGasResults().getFirst());
        }
        if (!recipe.getRollableResults().isEmpty() || recipe.getFluidResults().isEmpty()) {
            return filter.test(recipe.getResultItem(level.registryAccess()));
        }
        return filter.test(recipe.getFluidResults().getFirst());
    }
}
