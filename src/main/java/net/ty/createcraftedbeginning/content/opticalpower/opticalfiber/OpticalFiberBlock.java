package net.ty.createcraftedbeginning.content.opticalpower.opticalfiber;

import com.mojang.serialization.MapCodec;
import net.createmod.catnip.data.Iterate;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerConsumer;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkManager;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerSource;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class OpticalFiberBlock extends PipeBlock {
    private static final float FIBER_APOTHEM = 0.125F;
    private static final int FULL_LIGHT_LEVEL = 15;
    private static final int CONNECTION_BITS = Iterate.directions.length;
    private final Map<Integer, VoxelShape> deviceShapes = new ConcurrentHashMap<>();

    public OpticalFiberBlock(Properties properties) {
        super(FIBER_APOTHEM, properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
        for (BlockState state : getStateDefinition().getPossibleStates()) {
            int shapeIndex = getAABBIndex(state);
            int connections = 0;
            Direction firstDirection = Direction.DOWN;
            boolean sameAxis = true;
            for (Direction direction : Iterate.directions) {
                if (!state.getValue(PROPERTY_BY_DIRECTION.get(direction))) {
                    continue;
                }

                if (connections == 0) {
                    firstDirection = direction;
                }
                sameAxis &= direction.getAxis() == firstDirection.getAxis();
                connections++;
            }

            if (connections == 1) {
                shapeByIndex[shapeIndex] = CCBShapes.OPTICAL_FIBER_END.get(firstDirection);
                continue;
            }
            if (connections == 2 && sameAxis) {
                continue;
            }

            shapeByIndex[shapeIndex] = Shapes.or(shapeByIndex[shapeIndex], CCBShapes.OPTICAL_FIBER_JUNCTION);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (Direction direction : Iterate.directions) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(level, neighborPos, neighborState, direction.getOpposite()));
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
        boolean connected = canConnectTo(level, neighborPos, neighborState, direction.getOpposite());
        if (level instanceof ServerLevel serverLevel && state.getValue(PROPERTY_BY_DIRECTION.get(direction)) != connected) {
            OpticalPowerNetworkManager.invalidateAt(serverLevel, pos);
        }
        if (level instanceof Level world && world.isClientSide) {
            world.sendBlockUpdated(pos, state, state, UPDATE_CLIENTS);
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connected);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || oldState.is(state.getBlock())) {
            return;
        }

        OpticalPowerNetworkManager.invalidateAround(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            OpticalPowerNetworkManager.invalidateAround(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int ports = getDeviceConnections(level, pos, state);
        int shapeIndex = getAABBIndex(state);
        if (ports == 0) {
            return shapeByIndex[shapeIndex];
        }

        int key = shapeIndex | ports << CONNECTION_BITS;
        return deviceShapes.computeIfAbsent(key, ignored -> {
            VoxelShape shape = shapeByIndex[shapeIndex];
            for (Direction direction : Iterate.directions) {
                if ((ports & 1 << direction.get3DDataValue()) == 0) {
                    continue;
                }

                shape = Shapes.or(shape, CCBShapes.OPTICAL_FIBER_DEVICE_PORT.get(direction));
            }
            return shape;
        });
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return simpleCodec(OpticalFiberBlock::new);
    }

    public static boolean isConnected(BlockState state, Direction direction) {
        return state.getBlock() instanceof OpticalFiberBlock && state.getValue(PROPERTY_BY_DIRECTION.get(direction));
    }

    @SuppressWarnings("ConstantValue")
    public static int getDeviceConnections(BlockGetter level, BlockPos pos, BlockState state) {
        int ports = 0;
        for (Direction direction : Iterate.directions) {
            if (!isConnected(state, direction)) {
                continue;
            }

            BlockPos neighborPos = pos.relative(direction);
            if (level instanceof Level world && !world.isLoaded(neighborPos)) {
                continue;
            }

            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState == null || neighborState.getBlock() instanceof OpticalFiberBlock || !canConnectTo(level, neighborPos, neighborState, direction.getOpposite())) {
                continue;
            }

            ports |= 1 << direction.get3DDataValue();
        }
        return ports;
    }

    public static boolean canConnectTo(BlockGetter level, BlockPos pos, BlockState state, Direction sideOnNeighbor) {
        if (state.getBlock() instanceof OpticalPowerConsumer consumer) {
            return consumer.canConnectOpticalPower(state, sideOnNeighbor);
        }

        return state.getBlock() instanceof OpticalFiberBlock || state.getBlock() instanceof OpticalPowerSource || state.getLightEmission(level, pos) >= FULL_LIGHT_LEVEL && state.isCollisionShapeFullBlock(level, pos);
    }
}
