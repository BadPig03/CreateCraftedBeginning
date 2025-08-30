package net.ty.createcraftedbeginning.content.airtightencasedpipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.NotNull;

public class AirtightEncasedPipeBlock extends PipeBlock implements IBE<AirtightEncasedPipeBlockEntity>, IWrenchable {
    public static final MapCodec<AirtightEncasedPipeBlock> CODEC = simpleCodec(AirtightEncasedPipeBlock::new);
    private static final VoxelShape OCCLUSION_BOX = Block.box(0, 0, 0, 16, 16, 16);

    public AirtightEncasedPipeBlock(Properties properties) {
        super(0.5f, properties);
    }

    public static boolean isPipe(BlockState state) {
        return state.getBlock() instanceof AirtightEncasedPipeBlock;
    }

    public static boolean isOpenAt(BlockState state, Direction direction) {
        return state.getValue(PROPERTY_BY_DIRECTION.get(direction));
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull BlockState blockState, LivingEntity placer, @NotNull ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, placer, itemStack);
        AdvancementBehaviour.setPlacedBy(level, blockPos, placer);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
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
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos blockPos, @NotNull Block otherBlock, @NotNull BlockPos neighborPos, boolean isMoving) {
        DebugPackets.sendNeighborsUpdatePacket(world, blockPos);
        Direction direction = FluidPropagator.validateNeighbourChange(state, world, blockPos, otherBlock, neighborPos, isMoving);
        if (direction == null) {
            return;
        }
        world.scheduleTick(blockPos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void onPlace(@NotNull BlockState state, Level world, @NotNull BlockPos blockPos, @NotNull BlockState oldState, boolean isMoving) {
        if (world.isClientSide) {
            return;
        }
        if (state != oldState) {
            world.scheduleTick(blockPos, this, 1, TickPriority.HIGH);
        }
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
        boolean changed = state.getBlock() != newState.getBlock();
        if (changed && !level.isClientSide) {
            FluidPropagator.propagateChangedPipe(level, pos, state);
        }
        if (state.hasBlockEntity() && (changed || !newState.hasBlockEntity())) {
            level.removeBlockEntity(pos);
        }
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        boolean used = false;
        boolean added = false;
        Item sheet = CCBItems.AIRTIGHT_SHEET.asItem();

        if (!level.isClientSide()) {
            Direction direction = hitResult.getDirection();
            Property<Boolean> property = PROPERTY_BY_DIRECTION.get(direction);

            boolean currentValue = state.getValue(property);
            ItemStack usedStack = player.getItemInHand(hand);

            if (currentValue) {
                if (!usedStack.is(sheet)) {
                    return ItemInteractionResult.FAIL;
                }
                if (usedStack.is(AllItems.WRENCH)) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                added = true;
            } else {
                if (!usedStack.is(AllItems.WRENCH)) {
                    return ItemInteractionResult.FAIL;
                }
                if (usedStack.is(sheet)) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
            }

            BlockState newState = state.setValue(property, !currentValue);
            level.setBlock(pos, newState, Block.UPDATE_ALL);
            level.scheduleTick(pos, this, 1, TickPriority.HIGH);
            used = true;
        }

        if (added) {
            CCBSoundEvents.SHEET_ADDED.playOnServer(level, pos, 1f, 1f);
        } else {
            CCBSoundEvents.SHEET_REMOVED.playOnServer(level, pos, 1f, 1f);
        }

        return used ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos) {
        return OCCLUSION_BOX;
    }

    @Override
    public void tick(@NotNull BlockState blockState, @NotNull ServerLevel serverLevel, @NotNull BlockPos blockPos, @NotNull RandomSource random) {
        FluidPropagator.propagateChangedPipe(serverLevel, blockPos, blockState);
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
        return CODEC;
    }
}
