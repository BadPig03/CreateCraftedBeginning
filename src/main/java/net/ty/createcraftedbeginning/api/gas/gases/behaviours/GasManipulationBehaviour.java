package net.ty.createcraftedbeginning.api.gas.gases.behaviours;

import com.google.common.base.Predicates;
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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class GasManipulationBehaviour extends CapManipulationBehaviourBase<IGasHandler, GasManipulationBehaviour> {
    public static final BehaviourType<GasManipulationBehaviour> OBSERVE = new BehaviourType<>();

    private final BehaviourType<GasManipulationBehaviour> behaviourType;

    private ItemStack compiledFilterStack = ItemStack.EMPTY;
    private Predicate<GasStack> compiledFilter = GasFilterUtils.compile(ItemStack.EMPTY);

    /**
     * Creates a new {@code GasManipulationBehaviour} instance.
     *
     * @param be     the block entity that participates in the operation
     * @param target the target to use
     */
    public GasManipulationBehaviour(SmartBlockEntity be, InterfaceProvider target) {
        this(OBSERVE, be, target);
    }

    private GasManipulationBehaviour(BehaviourType<GasManipulationBehaviour> type, SmartBlockEntity be, InterfaceProvider target) {
        super(be, target);
        behaviourType = type;
    }

    /**
     * Computes and returns the extract any result.
     *
     * @return the resulting gas stack
     */
    public GasStack extractAny() {
        IGasHandler gasHandler = getInventory();
        if (gasHandler == null) {
            return GasStack.EMPTY;
        }

        Predicate<GasStack> filter = getFilterTest(Predicates.alwaysTrue());
        for (int i = 0; i < gasHandler.getTanks(); i++) {
            GasStack gasInTank = gasHandler.getGasInTank(i);
            if (gasInTank.isEmpty() || !filter.test(gasInTank)) {
                continue;
            }

            GasAction action = simulateNext ? GasAction.SIMULATE : GasAction.EXECUTE;
            GasStack drained = gasHandler.drain(gasInTank, action);
            if (drained.isEmpty()) {
                continue;
            }

            return drained;
        }

        return GasStack.EMPTY;
    }

    protected Predicate<GasStack> getFilterTest(Predicate<GasStack> test) {
        GasFilteringBehaviour gasFilter = blockEntity.getBehaviour(GasFilteringBehaviour.TYPE);
        if (gasFilter != null) {
            return test.and(gasFilter::test);
        }

        FilteringBehaviour itemFilter = blockEntity.getBehaviour(FilteringBehaviour.TYPE);
        if (itemFilter == null) {
            return test;
        }

        ItemStack filterStack = itemFilter.getFilter();
        if (filterStack.isEmpty()) {
            return test;
        }
        return test.and(getCompiledFilter(filterStack));
    }

    private Predicate<GasStack> getCompiledFilter(ItemStack filterStack) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public BehaviourType<?> getType() {
        return behaviourType;
    }
}
