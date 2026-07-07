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
import net.ty.createcraftedbeginning.content.airtights.gasfilter.IGasFilter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class GasManipulationBehaviour extends CapManipulationBehaviourBase<IGasHandler, GasManipulationBehaviour> {
    public static final BehaviourType<GasManipulationBehaviour> OBSERVE = new BehaviourType<>();

    private final BehaviourType<GasManipulationBehaviour> behaviourType;

    public GasManipulationBehaviour(SmartBlockEntity be, InterfaceProvider target) {
        this(OBSERVE, be, target);
    }

    private GasManipulationBehaviour(BehaviourType<GasManipulationBehaviour> type, SmartBlockEntity be, InterfaceProvider target) {
        super(be, target);
        behaviourType = type;
    }

    public GasStack extractAny() {
        IGasHandler gasHandler = getInventory();
        if (gasHandler == null) {
            return GasStack.EMPTY;
        }

        Predicate<GasStack> filterTest = getFilterTest(Predicates.alwaysTrue());
        for (int i = 0; i < gasHandler.getTanks(); i++) {
            GasStack gasInTank = gasHandler.getGasInTank(i);
            if (gasInTank.isEmpty() || !filterTest.test(gasInTank)) {
                continue;
            }

            GasStack drained = gasHandler.drain(gasInTank, simulateNext ? GasAction.SIMULATE : GasAction.EXECUTE);
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

        FilteringBehaviour filter = blockEntity.getBehaviour(FilteringBehaviour.TYPE);
        if (filter != null) {
            ItemStack filterStack = filter.getFilter();
            if (filterStack.isEmpty()) {
                return test;
            }

            if (!(filterStack.getItem() instanceof IGasFilter gasFilterItem)) {
                return gasStack -> false;
            }

            return test.and(gasStack -> gasFilterItem.test(filterStack, gasStack));
        }

        return test;
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
