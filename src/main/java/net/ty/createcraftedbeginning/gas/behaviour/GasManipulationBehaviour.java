package net.ty.createcraftedbeginning.gas.behaviour;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilters;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasManipulationBehaviour extends CapManipulationBehaviourBase<GasHandler, GasManipulationBehaviour> {
    private static final BehaviourType<GasManipulationBehaviour> OBSERVE = new BehaviourType<>();

    private final BehaviourType<GasManipulationBehaviour> behaviourType;

    private ItemStack compiledFilterStack = ItemStack.EMPTY;
    private Predicate<GasStack> compiledFilter = GasFilters.compile(ItemStack.EMPTY);

    public GasManipulationBehaviour(SmartBlockEntity blockEntity, InterfaceProvider target) {
        this(OBSERVE, blockEntity, target);
    }

    private GasManipulationBehaviour(BehaviourType<GasManipulationBehaviour> type, SmartBlockEntity blockEntity, InterfaceProvider target) {
        super(blockEntity, target);
        behaviourType = type;
    }

    private static boolean matchesFilter(GasStack gasStack, @Nullable GasFilteringBehaviour gasFilter, @Nullable Predicate<GasStack> itemFilter) {
        if (gasFilter == null) {
            return itemFilter == null || itemFilter.test(gasStack);
        }

        return gasFilter.test(gasStack);
    }

    @Override
    protected BlockCapability<GasHandler, Direction> capability() {
        return GasCapabilities.BLOCK;
    }

    @Override
    public BehaviourType<?> getType() {
        return behaviourType;
    }

    public GasStack extractAny() {
        GasHandler gasHandler = getInventory();
        if (gasHandler == null) {
            return GasStack.EMPTY;
        }

        GasFilteringBehaviour gasFilter = blockEntity.getBehaviour(GasFilteringBehaviour.TYPE);
        Predicate<GasStack> itemFilter = gasFilter == null ? getItemFilterTest() : null;
        for (int tankIndex = 0; tankIndex < gasHandler.getTanks(); tankIndex++) {
            GasStack gasInTank = gasHandler.getGasInTank(tankIndex);
            if (gasInTank.isEmpty() || !matchesFilter(gasInTank, gasFilter, itemFilter)) {
                continue;
            }

            GasStack extractedGas = gasHandler.drain(gasInTank, simulateNext ? GasAction.SIMULATE : GasAction.EXECUTE);
            if (extractedGas.isEmpty()) {
                continue;
            }

            return extractedGas;
        }
        return GasStack.EMPTY;
    }

    private @Nullable Predicate<GasStack> getItemFilterTest() {
        FilteringBehaviour itemFilter = blockEntity.getBehaviour(FilteringBehaviour.TYPE);
        if (itemFilter == null) {
            return null;
        }

        ItemStack filterStack = itemFilter.getFilter();
        if (filterStack.isEmpty()) {
            return null;
        }

        return getCompiledFilter(filterStack);
    }

    private Predicate<GasStack> getCompiledFilter(ItemStack filterStack) {
        if (ItemStack.isSameItemSameComponents(compiledFilterStack, filterStack)) {
            return compiledFilter;
        }

        compiledFilterStack = GasFilters.normalizeStack(filterStack);
        compiledFilter = GasFilters.compile(compiledFilterStack);
        return compiledFilter;
    }
}
