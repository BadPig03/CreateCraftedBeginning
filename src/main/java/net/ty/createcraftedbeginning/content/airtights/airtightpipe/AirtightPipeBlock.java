package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.IAirtightComponent;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

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
    public boolean isAirtight(BlockPos currentPos, @NotNull BlockState currentState, @NotNull Direction oppositeDirection) {
        return currentState.getValue(AXIS) == oppositeDirection.getAxis();
    }
}
