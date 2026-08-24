package net.ty.createcraftedbeginning.content.airtights.gas.behaviours;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasManipulationBehaviour extends CapManipulationBehaviourBase<IGasHandler, GasManipulationBehaviour> {
    protected static final BehaviourType<GasManipulationBehaviour> OBSERVE = new BehaviourType<>();

    protected final BehaviourType<GasManipulationBehaviour> behaviourType;

    protected ItemStack compiledFilterStack = ItemStack.EMPTY;
    protected Predicate<GasStack> compiledFilter = GasFilterUtils.compile(ItemStack.EMPTY);

    public GasManipulationBehaviour(SmartBlockEntity be, InterfaceProvider target) {
        this(OBSERVE, be, target);
    }

    protected GasManipulationBehaviour(BehaviourType<GasManipulationBehaviour> type, SmartBlockEntity be, InterfaceProvider target) {
        super(be, target);
        behaviourType = type;
    }

    private static boolean matchesFilter(GasStack gasStack, @Nullable GasFilteringBehaviour gasFilter, @Nullable Predicate<GasStack> itemFilter) {
        return gasFilter != null ? gasFilter.test(gasStack) : itemFilter == null || itemFilter.test(gasStack);
    }

    public GasStack extractAny() {
        IGasHandler gasHandler = getInventory();
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

    protected @Nullable Predicate<GasStack> getItemFilterTest() {
        FilteringBehaviour itemFilter = blockEntity.getBehaviour(FilteringBehaviour.TYPE);
        if (itemFilter == null) {
            return null;
        }

        ItemStack filterStack = itemFilter.getFilter();
        return filterStack.isEmpty() ? null : getCompiledFilter(filterStack);
    }

    protected Predicate<GasStack> getCompiledFilter(ItemStack filterStack) {
        if (ItemStack.isSameItemSameComponents(compiledFilterStack, filterStack)) {
            return compiledFilter;
        }

        compiledFilterStack = GasFilterUtils.normalizeStack(filterStack);
        compiledFilter = GasFilterUtils.compile(compiledFilterStack);
        return compiledFilter;
    }

    @Override
    protected BlockCapability<IGasHandler, Direction> capability() {
        return GasHandler.BLOCK;
    }

    @Override
    public BehaviourType<?> getType() {
        return behaviourType;
    }
}
