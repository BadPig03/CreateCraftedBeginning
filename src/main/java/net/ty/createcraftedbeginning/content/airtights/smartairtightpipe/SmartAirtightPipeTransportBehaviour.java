package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasFilteringBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SmartAirtightPipeTransportBehaviour extends AxisGasTransportBehaviour {
    private final SmartAirtightPipeBlockEntity pipe;

    public SmartAirtightPipeTransportBehaviour(SmartAirtightPipeBlockEntity pipe) {
        super(pipe);
        this.pipe = pipe;
    }

    @Override
    public boolean canPullGasFrom(GasStack gasStack, BlockState state, Direction direction) {
        GasFilteringBehaviour filter = pipe.getFilter();
        return (gasStack.isEmpty() || filter != null && filter.test(gasStack)) && super.canPullGasFrom(gasStack, state, direction);
    }
}
