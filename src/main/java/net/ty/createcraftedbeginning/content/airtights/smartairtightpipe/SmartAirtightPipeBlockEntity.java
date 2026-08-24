package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AbstractAirtightPipeBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasFilteringBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SmartAirtightPipeBlockEntity extends AbstractAirtightPipeBlockEntity {
    @Nullable
    private GasFilteringBehaviour filter;

    public SmartAirtightPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void addPipeBehaviours(List<BlockEntityBehaviour> behaviours) {
        filter = new GasFilteringBehaviour(this, new SmartAirtightPipeFilterSlot());
        behaviours.add(filter);
    }

    @Override
    protected GasTransportBehaviour createTransportBehaviour() {
        return new SmartAirtightPipeTransportBehaviour(this);
    }

    @Override
    public boolean canTransport(Level level, BlockState blockState, BlockPos blockPos, Direction direction) {
        return AxisGasPipeBlock.isOpenAt(blockState, direction);
    }

    @Nullable
    GasFilteringBehaviour getFilter() {
        return filter;
    }
}
