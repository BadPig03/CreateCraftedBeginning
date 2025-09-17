package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.api.gas.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlock;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirtightEncasedPipeBlockEntity extends SmartBlockEntity {
    public AirtightEncasedPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        GasEncasedPipeTransportBehaviour transportBehaviour = new GasEncasedPipeTransportBehaviour(this);
        behaviours.add(transportBehaviour);
    }

    class GasEncasedPipeTransportBehaviour extends GasTransportBehaviour {
        public GasEncasedPipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        private boolean isValidAirtightComponents(Block block) {
            if (block instanceof AirtightPipeBlock || block instanceof AirtightEncasedPipeBlock || block instanceof AirtightPumpBlock) {
                return true;
            }
            return block instanceof AirtightTankBlock || block instanceof AirCompressorBlock || block instanceof GasInjectionChamberBlock;
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            BlockState otherState = getWorld().getBlockState(worldPosition.relative(direction));
            Block otherBlock = otherState.getBlock();

            if (!(otherBlock instanceof AirBlock || isValidAirtightComponents(otherBlock))) {
                return false;
            }

            return AirtightEncasedPipeBlock.isPipe(state) && state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction));
        }
    }
}
