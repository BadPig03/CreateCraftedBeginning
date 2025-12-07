package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
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
import net.ty.createcraftedbeginning.api.gas.GasPropagator;
import net.ty.createcraftedbeginning.api.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.NotNull;

public class AirtightEncasedPipeBlock extends PipeBlock implements IBE<AirtightEncasedPipeBlockEntity>, IWrenchable, IAirtightComponent {
    private static final float PIPE_APOTHEM = 0.5f;

    public AirtightEncasedPipeBlock(Properties properties) {
        super(PIPE_APOTHEM, properties);
    }

    public static boolean isOpenAt(@NotNull BlockState state, Direction direction) {
        return state.getValue(PROPERTY_BY_DIRECTION.get(direction));
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull BlockState blockState, LivingEntity placer, @NotNull ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, placer, itemStack);
        CCBAdvancementBehaviour.setPlacedBy(level, blockPos, placer);
    }

    @Override
    protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbourState, @NotNull LevelAccessor world, @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
        if (isOpenAt(state, direction) && neighbourState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            world.scheduleTick(pos, this, 1, TickPriority.HIGH);
        }
        return state;
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block otherBlock, @NotNull BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, otherBlock, neighborPos, isMoving);
        Direction direction = GasPropagator.validateNeighbourChange(state, level, pos, neighborPos, isMoving);
        if (direction == null) {
            return;
        }

        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull BlockState oldState, boolean isMoving) {
        if (level.isClientSide || state == oldState) {
            return;
        }

        level.scheduleTick(blockPos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        boolean changed = !state.is(newState.getBlock());
        if (changed && !level.isClientSide) {
            GasPropagator.propagateChangedPipe(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        Direction direction = hitResult.getDirection();
        Property<Boolean> property = PROPERTY_BY_DIRECTION.get(direction);
        boolean isOpened = state.getValue(property);
        ItemStack usedStack = player.getItemInHand(hand);
        boolean isAddingSheet = isOpened && usedStack.is(CCBItems.AIRTIGHT_SHEET.asItem());
        boolean isRemovingSheet = !isOpened && usedStack.is(AllItems.WRENCH.asItem());
        if (!isAddingSheet && !isRemovingSheet) {
            return ItemInteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        BlockState newState = state.setValue(property, !isOpened);
        level.setBlockAndUpdate(pos, newState);
        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
        if (isAddingSheet) {
            CCBSoundEvents.SHEET_ADDED.playOnServer(level, pos, 1.0f, 1.0f);
        }
        else {
            CCBSoundEvents.SHEET_REMOVED.playOnServer(level, pos, 1.0f, 1.0f);
        }

        boolean noAdvancement = false;
        for (Direction dir : Iterate.directions) {
            if (newState.getValue(PROPERTY_BY_DIRECTION.get(dir))) {
                noAdvancement = true;
                break;
            }
        }
        if (!noAdvancement) {
            CCBAdvancements.HERMETIC_SEAL_600.awardTo(player);
        }
        return ItemInteractionResult.sidedSuccess(false);
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos) {
        return CCBShapes.ENCASED_PIPE_SHAPE;
    }

    @Override
    public void tick(@NotNull BlockState blockState, @NotNull ServerLevel serverLevel, @NotNull BlockPos blockPos, @NotNull RandomSource random) {
        GasPropagator.propagateChangedPipe(serverLevel, blockPos, blockState);
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
    protected @NotNull MapCodec<? extends PipeBlock> codec() {
        return simpleCodec(AirtightEncasedPipeBlock::new);
    }

    @Override
    public boolean isAirtight(BlockPos currentPos, @NotNull BlockState currentState, @NotNull Direction oppositeDirection) {
        return currentState.getValue(PROPERTY_BY_DIRECTION.get(oppositeDirection.getOpposite()));
    }
}