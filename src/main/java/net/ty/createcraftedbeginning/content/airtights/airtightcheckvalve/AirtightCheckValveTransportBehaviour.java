package net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightCheckValveTransportBehaviour extends AxisGasTransportBehaviour {
    AirtightCheckValveTransportBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public boolean allowsInboundFlow(BlockState state, Direction direction) {
        return canHaveFlowToward(state, direction) && AirtightCheckValveBlock.isInputSide(state, direction);
    }

    @Override
    public boolean allowsOutboundFlow(BlockState state, Direction direction) {
        return canHaveFlowToward(state, direction) && AirtightCheckValveBlock.isOutputSide(state, direction);
    }
}
