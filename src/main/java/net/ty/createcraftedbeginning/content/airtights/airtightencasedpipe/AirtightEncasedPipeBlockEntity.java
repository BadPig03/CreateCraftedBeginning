package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AbstractAirtightPipeBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEncasedPipeBlockEntity extends AbstractAirtightPipeBlockEntity implements IHaveGoggleInformation {
    public AirtightEncasedPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Level level = getLevel();
        return level != null && AirtightEncasedPipeTooltip.addTo(tooltip, level, worldPosition, getBlockState());
    }

    @Override
    protected GasTransportBehaviour createTransportBehaviour() {
        return new AirtightEncasedPipeTransportBehaviour(this);
    }

    @Override
    public boolean canTransport(Level level, BlockState blockState, BlockPos blockPos, Direction direction) {
        return blockState.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction));
    }
}
