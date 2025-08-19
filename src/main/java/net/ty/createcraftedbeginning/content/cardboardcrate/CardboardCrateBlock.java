package net.ty.createcraftedbeginning.content.cardboardcrate;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import net.ty.createcraftedbeginning.util.Helpers;
import org.jetbrains.annotations.NotNull;

public class CardboardCrateBlock extends HorizontalDirectionalBlock implements IBE<CardboardCrateBlockEntity>, IWrenchable {
    public static final int SLOT_LIMIT = 64;
    static final int MAX_SLOT = 2;

    public static final MapCodec<CardboardCrateBlock> CODEC = simpleCodec(CardboardCrateBlock::new);

    public CardboardCrateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public Class<CardboardCrateBlockEntity> getBlockEntityClass() {
        return CardboardCrateBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CardboardCrateBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.CARDBOARD_CRATE.get();
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos) {
        return Helpers.calculateRedstoneSignal(this, pLevel, pPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return CCBShapes.CRATE;
	}

    @Override
	protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}
}
