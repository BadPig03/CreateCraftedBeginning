package net.ty.createcraftedbeginning.content.airtights.airvents;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

public class AirVentBlock extends Block implements IBE<AirVentBlockEntity>, SimpleWaterloggedBlock, IWrenchable {
    public static final EnumProperty<VentState> NORTH = EnumProperty.create("north", VentState.class);
    public static final EnumProperty<VentState> EAST = EnumProperty.create("east", VentState.class);
    public static final EnumProperty<VentState> SOUTH = EnumProperty.create("south", VentState.class);
    public static final EnumProperty<VentState> WEST = EnumProperty.create("west", VentState.class);
    public static final EnumProperty<VentState> UP = EnumProperty.create("up", VentState.class);
    public static final EnumProperty<VentState> DOWN = EnumProperty.create("down", VentState.class);
    public static final Map<Direction, EnumProperty<VentState>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(Util.make(Maps.newEnumMap(Direction.class), property -> {
        property.put(Direction.NORTH, NORTH);
        property.put(Direction.EAST, EAST);
        property.put(Direction.SOUTH, SOUTH);
        property.put(Direction.WEST, WEST);
        property.put(Direction.UP, UP);
        property.put(Direction.DOWN, DOWN);
    }));
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirVentBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(NORTH, VentState.EMPTY).setValue(EAST, VentState.EMPTY).setValue(SOUTH, VentState.EMPTY).setValue(WEST, VentState.EMPTY).setValue(UP, VentState.EMPTY).setValue(DOWN, VentState.EMPTY));
    }

    private static VoxelShape getShape(BlockState blockState) {
        int mask = Arrays.stream(Iterate.directions).filter(direction -> blockState.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected()).mapToInt(direction -> 1 << direction.get3DDataValue()).reduce(0, (a, b) -> a | b);
        return AirVentVoxelShapes.getShape(mask);
    }

    private static VoxelShape getCollisionShape(BlockState blockState) {
        int mask = Arrays.stream(Iterate.directions).filter(direction -> blockState.getValue(PROPERTY_BY_DIRECTION.get(direction)).canPassThrough()).mapToInt(direction -> 1 << direction.get3DDataValue()).reduce(0, (a, b) -> a | b);
        return AirVentVoxelShapes.getShape(mask);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, @NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.getInBlockState().is(CCBBlocks.AIR_VENT_BLOCK)) {
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, @NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.getInBlockState().is(CCBBlocks.AIR_VENT_BLOCK)) {
            return InteractionResult.FAIL;
        }

        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    public boolean isLadder(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        BlockState inBlockState = player.getInBlockState();
        return inBlockState.is(CCBBlocks.AIR_VENT_BLOCK) && (inBlockState.getValue(PROPERTY_BY_DIRECTION.get(Direction.UP)).isConnected() || inBlockState.getValue(PROPERTY_BY_DIRECTION.get(Direction.DOWN)).isConnected());
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbourState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        EnumProperty<VentState> property = PROPERTY_BY_DIRECTION.get(direction);
        VentState ventState = state.getValue(property);
        boolean isAirVent = neighbourState.is(CCBBlocks.AIR_VENT_BLOCK);
        if (ventState != VentState.CONNECTED && isAirVent) {
            return state.setValue(property, VentState.CONNECTED);
        }
        else if (ventState == VentState.CONNECTED && !isAirVent) {
            return state.setValue(property, VentState.EMPTY);
        }
        return state;
    }

    @Override
    protected boolean skipRendering(@NotNull BlockState blockState, @NotNull BlockState adjacentState, @NotNull Direction direction) {
        return adjacentState.is(CCBBlocks.AIR_VENT_BLOCK);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (player.getInBlockState().is(CCBBlocks.AIR_VENT_BLOCK)) {
            return ItemInteractionResult.FAIL;
        }

        Direction direction = hitResult.getDirection();
        VentState ventState = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
        ItemStack handItem = player.getItemInHand(hand);
        if (handItem.is(AllItems.WRENCH.asItem())) {
            if (ventState.isConnected()) {
                return ItemInteractionResult.FAIL;
            }

            if (level.isClientSide) {
                return ItemInteractionResult.sidedSuccess(true);
            }

            if (ventState == VentState.EMPTY) {
                CCBSoundEvents.AIR_VENT_OUTLET_PLACED.playOnServer(level, pos, 1.0f, 1.0f);
            }
            else {
                CCBSoundEvents.AIR_VENT_OUTLET_REMOVED.playOnServer(level, pos, 1.0f, 1.0f);
            }
            level.setBlockAndUpdate(pos, state.setValue(PROPERTY_BY_DIRECTION.get(direction), VentState.getWrenchInteractedState(ventState)));
            return ItemInteractionResult.sidedSuccess(false);
        }

        if (!handItem.isEmpty() || !ventState.canHandInteract()) {
            return ItemInteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        if (ventState == VentState.CLOSED) {
            CCBSoundEvents.AIR_VENT_OUTLET_OPENED.playOnServer(level, pos, 1.0f, 1.0f);
        }
        else {
            CCBSoundEvents.AIR_VENT_OUTLET_CLOSED.playOnServer(level, pos, 1.0f, 1.0f);
        }
        level.setBlockAndUpdate(pos, state.setValue(PROPERTY_BY_DIRECTION.get(direction), VentState.getHandInteractedState(ventState)));
        return ItemInteractionResult.sidedSuccess(false);
    }

    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    protected @NotNull VoxelShape getOcclusionShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos) {
        return Shapes.empty();
    }

    @Override
    protected @NotNull VoxelShape getBlockSupportShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos) {
        return Shapes.block();
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext context) {
        if (context instanceof EntityCollisionContext entityCollisionContext && !(entityCollisionContext.getEntity() instanceof Player)) {
            return Shapes.block();
        }

        return getShape(blockState);
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext context) {
        return getCollisionShape(blockState);
    }

    @Override
    protected @NotNull VoxelShape getVisualShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return ProperWaterloggedBlock.withWater(context.getLevel(), defaultBlockState(), context.getClickedPos());
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull BlockState blockState, LivingEntity entity, @NotNull ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, entity, itemStack);
        CCBAdvancementBehaviour.setPlacedBy(level, blockPos, entity);
    }

    @Override
    protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> builder) {
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

    public enum VentState implements StringRepresentable {
        OPENED,
        CLOSED,
        EMPTY,
        CONNECTED;

        public static final Codec<VentState> CODEC = StringRepresentable.fromEnum(VentState::values);

        @Contract(pure = true)
        public static VentState getHandInteractedState(@NotNull VentState oldState) {
            return switch (oldState) {
                case CLOSED -> OPENED;
                case OPENED -> CLOSED;
                case EMPTY -> EMPTY;
                case CONNECTED -> CONNECTED;
            };
        }

        @Contract(pure = true)
        public static VentState getWrenchInteractedState(@NotNull VentState oldState) {
            return switch (oldState) {
                case CLOSED, OPENED -> EMPTY;
                case EMPTY -> CLOSED;
                case CONNECTED -> CONNECTED;
            };
        }

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }

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
