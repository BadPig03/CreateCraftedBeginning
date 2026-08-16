package net.ty.createcraftedbeginning.content.photostresses.opticalfiber;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.ty.createcraftedbeginning.content.photostresses.network.PhotoStressNetworkManager;
import net.ty.createcraftedbeginning.content.photostresses.network.PhotoStressSource;
import net.ty.createcraftedbeginning.content.photostresses.phohostressbearing.PhotoStressBearingBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class OpticalFiberBlock extends PipeBlock {
    private static final float FIBER_APOTHEM = 0.125f;
    private static final int FULL_LIGHT_LEVEL = 15;

    public OpticalFiberBlock(Properties properties) {
        super(FIBER_APOTHEM, properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    public static boolean isConnected(BlockState state, Direction direction) {
        return state.getBlock() instanceof OpticalFiberBlock && state.getValue(PROPERTY_BY_DIRECTION.get(direction));
    }

    public static boolean canConnectTo(BlockGetter level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof OpticalFiberBlock || state.getBlock() instanceof PhotoStressBearingBlock || state.getBlock() instanceof PhotoStressSource || state.getLightEmission(level, pos) >= FULL_LIGHT_LEVEL && state.isCollisionShapeFullBlock(level, pos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(level, neighborPos, neighborState));
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean connected = canConnectTo(level, neighborPos, neighborState);
        if (level instanceof ServerLevel serverLevel && state.getValue(PROPERTY_BY_DIRECTION.get(direction)) != connected) {
            PhotoStressNetworkManager.invalidateAt(serverLevel, pos);
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connected);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || oldState.is(state.getBlock())) {
            return;
        }

        PhotoStressNetworkManager.invalidateAround(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            PhotoStressNetworkManager.invalidateAround(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return simpleCodec(OpticalFiberBlock::new);
    }
}
