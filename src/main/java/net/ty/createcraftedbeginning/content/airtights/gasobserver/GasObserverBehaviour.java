package net.ty.createcraftedbeginning.content.airtights.gasobserver;

import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilters;
import net.ty.createcraftedbeginning.gas.behaviour.GasManipulationBehaviour;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasObserverBehaviour extends GasManipulationBehaviour {
    private ItemStack compiledStack = ItemStack.EMPTY;
    private Predicate<GasStack> compiledFilter = GasFilters.compile(ItemStack.EMPTY);

    public GasObserverBehaviour(SmartBlockEntity observer) {
        super(observer, (world, pos, state) -> new BlockFace(pos, DirectedDirectionalBlock.getTargetDirection(state)));
        bypassSidedness();
    }

    public boolean hasMatchingGas(FilteringBehaviour filtering) {
        Level level = getWorld();
        BlockPos target = getPos().relative(SmartObserverBlock.getTargetDirection(blockEntity.getBlockState()));
        if (level == null || !level.isLoaded(target)) {
            return false;
        }

        GasTransportBehaviour transport = get(level, target, GasTransportBehaviour.TYPE);
        if (transport == null) {
            return !simulate().extractAny().isEmpty();
        }

        ItemStack current = filtering.getFilter();
        if (!ItemStack.isSameItemSameComponents(compiledStack, current)) {
            compiledStack = GasFilters.normalizeStack(current);
            compiledFilter = GasFilters.compile(compiledStack);
        }
        for (Direction side : Iterate.directions) {
            FlowState flow = transport.getFlowState(side);
            if (flow == null || flow.direction() != FlowDirection.INBOUND || !compiledFilter.test(flow.gas())) {
                continue;
            }

            return true;
        }

        return false;
    }
}
