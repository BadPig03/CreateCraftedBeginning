package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasTransportBehaviour;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirtightEncasedPipeBlockEntity extends SmartBlockEntity {
    public AirtightEncasedPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        AirtightEncasedPipeTransportBehaviour transportBehaviour = new AirtightEncasedPipeTransportBehaviour(this);
        behaviours.add(transportBehaviour);
    }

    class AirtightEncasedPipeTransportBehaviour extends GasTransportBehaviour {
        public AirtightEncasedPipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            BlockPos otherPos = worldPosition.relative(direction);
            return isValidAirtightComponents(otherPos, getWorld().getBlockState(otherPos), direction) && state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction));
        }
    }
}
