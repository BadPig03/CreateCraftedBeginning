package net.ty.createcraftedbeginning.content.airtights.airtightvalve;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightValveTransportBehaviour extends AxisGasTransportBehaviour {
    private final AirtightValveBlockEntity valve;

    AirtightValveTransportBehaviour(AirtightValveBlockEntity valve) {
        super(valve);
        this.valve = valve;
    }

    @Override
    public boolean allowsInboundFlow(BlockState state, Direction direction) {
        return valve.isOpen() && canConnectOnFace(state, direction);
    }

    @Override
    public boolean allowsOutboundFlow(BlockState state, Direction direction) {
        return valve.isOpen() && canConnectOnFace(state, direction);
    }
}
