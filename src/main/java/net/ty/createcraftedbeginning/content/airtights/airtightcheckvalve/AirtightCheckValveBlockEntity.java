package net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AbstractAirtightPipeBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightCheckValveBlockEntity extends AbstractAirtightPipeBlockEntity {
    public AirtightCheckValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected GasTransportBehaviour createTransportBehaviour() {
        return new AirtightCheckValveTransportBehaviour(this);
    }

    @Override
    public boolean canTransport(Level level, BlockState state, BlockPos pos, Direction direction) {
        return AirtightCheckValveBlock.isInputSide(state, direction);
    }
}
