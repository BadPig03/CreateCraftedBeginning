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
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.getInBlockState().is(CCBBlocks.AIR_VENT_BLOCK)) {
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.getInBlockState().is(CCBBlocks.AIR_VENT_BLOCK)) {
            return InteractionResult.FAIL;
        }

        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        BlockState inBlockState = player.getInBlockState();
        return inBlockState.is(CCBBlocks.AIR_VENT_BLOCK) && (inBlockState.getValue(PROPERTY_BY_DIRECTION.get(Direction.UP)).isConnected() || inBlockState.getValue(PROPERTY_BY_DIRECTION.get(Direction.DOWN)).isConnected());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
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
    protected boolean skipRendering(BlockState blockState, BlockState adjacentState, Direction direction) {
        return adjacentState.is(CCBBlocks.AIR_VENT_BLOCK);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
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
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState blockState, BlockGetter level, BlockPos blockPos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState blockState, BlockGetter level, BlockPos blockPos) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getShape(BlockState blockState, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        return getShape(blockState);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState blockState, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        return getCollisionShape(blockState);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState blockState, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return ProperWaterloggedBlock.withWater(context.getLevel(), defaultBlockState(), context.getClickedPos());
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

    public enum VentState implements StringRepresentable {
        OPENED,
        CLOSED,
        EMPTY,
        CONNECTED;

        public static final Codec<VentState> CODEC = StringRepresentable.fromEnum(VentState::values);

        @Contract(pure = true)
        public static VentState getHandInteractedState(VentState oldState) {
            return switch (oldState) {
                case CLOSED -> OPENED;
                case OPENED -> CLOSED;
                case EMPTY -> EMPTY;
                case CONNECTED -> CONNECTED;
            };
        }

        @Contract(pure = true)
        public static VentState getWrenchInteractedState(VentState oldState) {
            return switch (oldState) {
                case CLOSED, OPENED -> EMPTY;
                case EMPTY -> CLOSED;
                case CONNECTED -> CONNECTED;
            };
        }

        @Override
        public String getSerializedName() {
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
