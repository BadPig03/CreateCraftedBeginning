package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.GasPropagator;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEncasedPipeBlock extends PipeBlock implements IBE<AirtightEncasedPipeBlockEntity>, IWrenchable, IAirtightComponent {
    private static final float PIPE_APOTHEM = 0.5f;

    public AirtightEncasedPipeBlock(Properties properties) {
        super(PIPE_APOTHEM, properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    public static boolean isOpenAt(BlockState state, Direction direction) {
        return state.getValue(PROPERTY_BY_DIRECTION.get(direction));
    }

    private static boolean hasPlacementConnection(Level level, BlockPos pos, Direction direction) {
        BlockPos otherPos = pos.relative(direction);
        BlockState otherState = level.getBlockState(otherPos);
        return !otherState.isAir() && (!otherState.canBeReplaced() || CCBBlockTags.GAS_SOURCES.matches(otherState)) && GasTransportBehaviour.isValidAirtightComponents(level, otherPos, otherState, direction);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return state;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState replacedState = level.getBlockState(pos);
        for (Direction direction : Iterate.directions) {
            boolean shouldOpen = hasPlacementConnection(level, pos, direction);
            if (replacedState.getBlock() instanceof AirtightPipeBlock) {
                shouldOpen |= replacedState.getValue(AirtightPipeBlock.AXIS) == direction.getAxis();
            }
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), shouldOpen);
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor world, BlockPos pos, BlockPos neighbourPos) {
        if (isOpenAt(state, direction) && neighbourState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            world.scheduleTick(pos, this, 1, TickPriority.HIGH);
        }
        return state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, otherBlock, neighborPos, isMoving);
        Direction direction = GasPropagator.getChangedNeighbourSide(level, pos, neighborPos);
        if (direction == null) {
            return;
        }

        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos blockPos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide || state == oldState) {
            return;
        }

        level.scheduleTick(blockPos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        boolean changed = !state.is(newState.getBlock());
        if (changed && !level.isClientSide) {
            GasPropagator.propagatePipe(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        Property<Boolean> property = PROPERTY_BY_DIRECTION.get(hitResult.getDirection());
        boolean opened = state.getValue(property);
        ItemStack held = player.getItemInHand(hand);
        boolean adding = opened && held.is(AllItems.WRENCH.asItem());
        boolean removing = !opened && held.is(AllItems.WRENCH.asItem());
        if (!adding && !removing) {
            return ItemInteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        BlockState newState = state.setValue(property, !opened);
        level.setBlockAndUpdate(pos, newState);
        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
        if (adding) {
            CCBSoundEvents.SHEET_ADDED.playOnServer(level, pos, 1.0f, 1.0f);
        }
        else {
            CCBSoundEvents.SHEET_REMOVED.playOnServer(level, pos, 1.0f, 1.0f);
        }
        return ItemInteractionResult.sidedSuccess(false);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState blockState, BlockGetter level, BlockPos blockPos) {
        return CCBShapes.ENCASED_PIPE_SHAPE;
    }

    @Override
    public void tick(BlockState blockState, ServerLevel level, BlockPos blockPos, RandomSource random) {
        GasPropagator.propagatePipe(level, blockPos, blockState);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public Class<AirtightEncasedPipeBlockEntity> getBlockEntityClass() {
        return AirtightEncasedPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightEncasedPipeBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_ENCASED_PIPE.get();
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return simpleCodec(AirtightEncasedPipeBlock::new);
    }

    @Override
    public boolean isAirtight(BlockPos currentPos, BlockState currentState, Direction oppositeDirection) {
        return currentState.getValue(PROPERTY_BY_DIRECTION.get(oppositeDirection.getOpposite()));
    }
}