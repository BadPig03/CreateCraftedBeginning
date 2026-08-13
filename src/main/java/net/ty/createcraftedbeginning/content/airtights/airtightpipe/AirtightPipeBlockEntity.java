package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPipeBlockEntity extends AbstractAirtightPipeBlockEntity {
    public AirtightPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void addPipeBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new BracketedBlockEntityBehaviour(this));
    }

    @Override
    protected GasTransportBehaviour createTransportBehaviour() {
        return new AirtightPipeTransportBehaviour(this);
    }

    @Override
    public boolean canTransport(Level level, BlockState blockState, BlockPos blockPos, Direction direction) {
        return AxisGasPipeBlock.isOpenAt(blockState, direction);
    }
}
