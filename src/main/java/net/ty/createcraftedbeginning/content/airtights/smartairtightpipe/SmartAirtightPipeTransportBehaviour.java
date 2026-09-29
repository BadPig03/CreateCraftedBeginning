package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.behaviour.GasFilteringBehaviour;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class SmartAirtightPipeTransportBehaviour extends AxisGasTransportBehaviour {
    private final @Nullable GasFilteringBehaviour filter;

    SmartAirtightPipeTransportBehaviour(SmartAirtightPipeBlockEntity pipe, @Nullable GasFilteringBehaviour filter) {
        super(pipe);
        this.filter = filter;
    }

    @Override
    public boolean acceptsGas(GasStack gasStack, BlockState state, Direction direction) {
        return (gasStack.isEmpty() || filter != null && filter.test(gasStack)) && super.acceptsGas(gasStack, state, direction);
    }
}
