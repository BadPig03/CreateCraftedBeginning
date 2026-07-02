package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPipeBlock extends AxisGasPipeBlock implements IBE<AirtightPipeBlockEntity>, IAirtightComponent {
    public AirtightPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<AirtightPipeBlockEntity> getBlockEntityClass() {
        return AirtightPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightPipeBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_PIPE.get();
    }

    @Override
    public boolean isAirtight(BlockPos currentPos, BlockState currentState, Direction oppositeDirection) {
        return currentState.getValue(AXIS) == oppositeDirection.getAxis();
    }
}
