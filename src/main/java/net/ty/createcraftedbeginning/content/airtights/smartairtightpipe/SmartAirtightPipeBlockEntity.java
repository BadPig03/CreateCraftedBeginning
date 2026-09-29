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
import net.ty.createcraftedbeginning.gas.behaviour.GasFilteringBehaviour;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
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
        filter = new GasFilteringBehaviour(this, new SmartAirtightPipeFilterSlot()).withCallback(ignoredStack -> {
            Level level = getLevel();
            if (level == null) {
                return;
            }

            GasNetworkTopology.invalidate(level, getBlockPos());
        });
        behaviours.add(filter);
    }

    @Override
    protected GasTransportBehaviour createTransportBehaviour() {
        return new SmartAirtightPipeTransportBehaviour(this, filter);
    }

    @Override
    public boolean allowsGasTransport(Level level, BlockState blockState, BlockPos blockPos, Direction direction) {
        return AxisGasPipeBlock.isOpenAt(blockState, direction);
    }

}
