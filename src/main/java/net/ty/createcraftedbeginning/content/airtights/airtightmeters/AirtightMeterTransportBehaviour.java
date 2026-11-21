package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightMeterTransportBehaviour extends AxisGasTransportBehaviour {
    AirtightMeterTransportBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public boolean isOrdinaryPressurePipe() {
        return true;
    }

    @Override
    protected long getFlowResistanceUnits(BlockState state, Direction entryFace, Direction exitFace) {
        return 0;
    }
}
