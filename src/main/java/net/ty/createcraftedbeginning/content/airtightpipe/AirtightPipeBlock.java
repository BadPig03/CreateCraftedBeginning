package net.ty.createcraftedbeginning.content.airtightpipe;

import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class AirtightPipeBlock extends AxisPipeBlock implements IBE<AirtightPipeBlockEntity>, SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirtightPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @NotNull BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return super.getStateForPlacement(context).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        return CCBBlocks.AIRTIGHT_PIPE_BLOCK.asStack();
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public Class<AirtightPipeBlockEntity> getBlockEntityClass() {
        return AirtightPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightPipeBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_PIPE.get();
    }
}
