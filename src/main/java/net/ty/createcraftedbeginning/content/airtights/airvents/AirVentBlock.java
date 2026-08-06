package net.ty.createcraftedbeginning.content.airtights.airvents;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirVentBlock extends Block implements IBE<AirVentBlockEntity>, SimpleWaterloggedBlock, IWrenchable {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(Util.make(Maps.newEnumMap(Direction.class), properties -> {
        properties.put(Direction.NORTH, NORTH);
        properties.put(Direction.EAST, EAST);
        properties.put(Direction.SOUTH, SOUTH);
        properties.put(Direction.WEST, WEST);
        properties.put(Direction.UP, UP);
        properties.put(Direction.DOWN, DOWN);
    }));
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirVentBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    private static VoxelShape getConnectedShape(BlockState state) {
        return AirVentVoxelShapes.getShape(getConnectionMask(state));
    }

    public static int getConnectionMask(BlockState state) {
        int mask = 0;
        for (Direction direction : Iterate.directions) {
            if (!isConnected(state, direction)) {
                continue;
            }

            mask |= 1 << direction.get3DDataValue();
        }
        return mask;
    }

    private static VoxelShape getPassableShape(BlockState state, BlockGetter level, BlockPos pos) {
        int mask = getConnectionMask(state);
        AirVentBlockEntity airVent = getAirVentBlockEntity(level, pos);
        if (airVent != null) {
            mask |= airVent.getOpenedLouverMask();
        }
        return AirVentVoxelShapes.getShape(mask);
    }

    public static boolean isConnected(BlockState state, Direction direction) {
        return state.getValue(PROPERTY_BY_DIRECTION.get(direction));
    }

    public static BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction direction : Iterate.directions) {
            boolean connected = level.getBlockState(pos.relative(direction)).getBlock() instanceof AirVentBlock;
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connected);
        }
        return state;
    }

    public static VentState getVentState(BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
        if (isConnected(state, direction)) {
            return VentState.CONNECTED;
        }

        AirVentBlockEntity airVent = getAirVentBlockEntity(level, pos);
        return airVent == null ? VentState.EMPTY : airVent.getLouverState(direction);
    }

    private static @Nullable AirVentBlockEntity getAirVentBlockEntity(BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof AirVentBlockEntity airVent ? airVent : null;
    }

    private static boolean isInsideAirVent(@Nullable Player player) {
        return player != null && player.getInBlockState().getBlock() instanceof AirVentBlock;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Player player = context.getPlayer();
        if (isInsideAirVent(player)) {
            return InteractionResult.FAIL;
        }

        Direction direction = context.getClickedFace();
        if (isConnected(state, direction)) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        AirVentBlockEntity airVent = getAirVentBlockEntity(level, pos);
        if (airVent == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        boolean hadLouver = airVent.hasLouver(direction);
        airVent.toggleLouver(direction);
        if (hadLouver) {
            CCBSoundEvents.AIR_VENT_OUTLET_REMOVED.playOnServer(level, pos, 1, 1);
        }
        else {
            CCBSoundEvents.AIR_VENT_OUTLET_PLACED.playOnServer(level, pos, 1, 1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Player player = context.getPlayer();
        if (isInsideAirVent(player)) {
            return InteractionResult.FAIL;
        }
        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return entity instanceof Player && (isConnected(state, Direction.UP) || isConnected(state, Direction.DOWN));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        BooleanProperty property = PROPERTY_BY_DIRECTION.get(direction);
        boolean connected = neighbourState.getBlock() instanceof AirVentBlock;
        return state.getValue(property) == connected ? state : state.setValue(property, connected);
    }

    @Override
    protected boolean skipRendering(BlockState blockState, BlockState adjacentState, Direction direction) {
        return adjacentState.getBlock() instanceof AirVentBlock && isConnected(blockState, direction) && isConnected(adjacentState, direction.getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.isEmpty() || isInsideAirVent(player)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Direction direction = hitResult.getDirection();
        if (isConnected(state, direction)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        AirVentBlockEntity airVent = getAirVentBlockEntity(level, pos);
        if (airVent == null || !airVent.hasLouver(direction)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        boolean wasOpen = airVent.isLouverOpen(direction);
        airVent.toggleLouverOpen(direction);
        if (wasOpen) {
            CCBSoundEvents.AIR_VENT_OUTLET_CLOSED.playOnServer(level, pos, 1, 1);
        }
        else {
            CCBSoundEvents.AIR_VENT_OUTLET_OPENED.playOnServer(level, pos, 1, 1);
        }
        return ItemInteractionResult.sidedSuccess(false);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return getPassableShape(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getConnectedShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getPassableShape(state, level, pos);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = ProperWaterloggedBlock.withWater(level, defaultBlockState(), pos);
        return withConnections(state, level, pos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, NORTH, EAST, SOUTH, WEST, UP, DOWN);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public Class<AirVentBlockEntity> getBlockEntityClass() {
        return AirVentBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirVentBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIR_VENT.get();
    }

    public enum VentState {
        OPENED,
        CLOSED,
        EMPTY,
        CONNECTED;

        public boolean canHandInteract() {
            return this == OPENED || this == CLOSED;
        }

        public boolean canPassThrough() {
            return this == OPENED || this == CONNECTED;
        }

        public boolean isConnected() {
            return this == CONNECTED;
        }
    }
}
