package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentTypes.AttachmentTypes;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.IAirtightPipeDrain;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPipeConnection;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightPumpTransportBehaviour extends GasTransportBehaviour {
    private final AirtightPumpBlockEntity pump;

    public AirtightPumpTransportBehaviour(AirtightPumpBlockEntity pump) {
        super(pump);
        this.pump = pump;
    }

    @Override
    public boolean canHaveFlowToward(BlockState state, Direction direction) {
        return canHaveFlowTowardWithoutLevel(state, direction);
    }

    @Override
    public boolean canHaveFlowTowardWithoutLevel(BlockState state, Direction direction) {
        return pump.isSideAccessible(direction);
    }

    @Override
    protected void beforeFlowUpdate(Level level, BlockPos pos, Collection<GasPipeConnection> connections) {
        if (level.isClientSide && !pump.isVirtual() || !level.isLoaded(pos) || pump.isRemoved()) {
            return;
        }

        float pressure = pump.getPumpPressure();
        for (GasPipeConnection connection : connections) {
            Direction direction = connection.getSide();
            connection.setPumpPressure(AirtightPumpPressureController.isPullingOnSide(pump.isFront(direction)), pressure);
        }
    }

    @Override
    public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction) {
        BlockPos adjacentPos = pos.relative(direction);
        BlockState adjacentState = level.getBlockState(adjacentPos);
        if (!(adjacentState.getBlock() instanceof IAirtightPipeDrain drain) || !drain.shouldRenderDrain(level, adjacentPos, adjacentState, direction.getOpposite())) {
            return AttachmentTypes.NONE;
        }
        return AttachmentTypes.DRAIN;
    }
}
