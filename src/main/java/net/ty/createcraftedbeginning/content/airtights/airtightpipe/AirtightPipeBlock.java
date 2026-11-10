package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.equipment.wrench.IWrenchableWithBracket;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockRotation;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.visual.GasPlacementHelper;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPipeBlock extends PipeBlock implements IBE<AirtightPipeBlockEntity>, GasConnectable, SimpleWaterloggedBlock, IWrenchableWithBracket, TransformableBlock {
    public static final BooleanProperty CASED = BooleanProperty.create("cased");

    public AirtightPipeBlock(Properties properties) {
        super(0.25F, properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(UP, true).setValue(DOWN, true).setValue(CASED, false).setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    public Class<AirtightPipeBlockEntity> getBlockEntityClass() {
        return AirtightPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightPipeBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_PIPE.get();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!state.getValue(CASED)) {
            BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(level, pos, BracketedBlockEntityBehaviour.TYPE);
            if (bracket == null || !bracket.isBracketPresent()) {
                return InteractionResult.FAIL;
            }

            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            tryRemoveBracket(context);
            BlockState updated = AirtightPipeConnections.update(level.getBlockState(pos), level, pos, context.getClickedFace().getAxis());
            level.setBlockAndUpdate(pos, updated);
            level.scheduleTick(pos, this, 1);
            AirtightPipeAdvancement.checkPlayerAction(level, pos, context.getPlayer());
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Direction face = context.getClickedFace();
        boolean wasOpen = AirtightPipeConnections.isOpen(state, face);
        level.setBlockAndUpdate(pos, state.setValue(PROPERTY_BY_DIRECTION.get(face), !wasOpen));
        AirtightPipeAdvancement.checkPlayerAction(level, pos, context.getPlayer());
        if (wasOpen) {
            CCBSoundEvents.SHEET_ADDED.playOnServer(level, pos, 1, 1);
        }
        else {
            CCBSoundEvents.SHEET_REMOVED.playOnServer(level, pos, 1, 1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (!state.getValue(CASED)) {
            return IWrenchableWithBracket.super.onSneakWrenched(state, context);
        }

        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        BlockState bare = AirtightPipeConnections.update(state.setValue(CASED, false), level, pos, context.getClickedFace().getAxis());
        level.setBlockAndUpdate(pos, bare);
        AirtightPipeAdvancement.checkPlayerAction(level, pos, context.getPlayer());
        CCBSoundEvents.SHEET_REMOVED.playOnServer(level, pos, 1, 1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FluidPipeBlockRotation.rotate(state, rotation);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FluidPipeBlockRotation.mirror(state, mirror);
    }

    @Override
    public BlockState transform(BlockState state, StructureTransform transform) {
        return FluidPipeBlockRotation.transform(state, transform);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, CASED, BlockStateProperties.WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(CCBItems.AIRTIGHT_SHEET) || state.getValue(CASED)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        removeBracket(level, pos, false).ifPresent(bracket -> {
            if (!player.isCreative()) {
                player.getInventory().placeItemBackInInventory(bracket);
            }
        });
        level.setBlockAndUpdate(pos, state.setValue(CASED, true).setValue(BlockStateProperties.WATERLOGGED, false));
        AirtightPipeAdvancement.checkPlayerAction(level, pos, player);
        CCBSoundEvents.SHEET_ADDED.playOnServer(level, pos, 1, 1);
        return ItemInteractionResult.sidedSuccess(false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(CASED)) {
            return Shapes.block();
        }

        return AirtightPipeShapes.getShape(state);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return !state.getValue(CASED) && SimpleWaterloggedBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return !state.getValue(CASED) && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
    }

    @Override
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return AirtightPipeConnections.isOpen(currentState, localFace);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Axis axis = GasPlacementHelper.chooseAxis(context, EnumSet.noneOf(Axis.class));
        BlockState state = AirtightPipeConnections.straight(defaultBlockState(), axis);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        state = AirtightPipeConnections.update(state, level, pos, axis);
        return ProperWaterloggedBlock.withWater(level, state, pos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
        AirtightPipeAdvancement.checkPlayerAction(level, pos, entity);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return AirtightPipeConnections.update(state, level, pos, direction.getAxis());
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            return Fluids.WATER.defaultFluidState();
        }

        return super.getFluidState(state);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }

        GasTransportBehaviour transport = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
        if (transport != null) {
            transport.markConnectionsDirty();
        }
        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide || state == oldState) {
            return;
        }

        GasTransportBehaviour transport = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
        if (transport != null) {
            transport.markConnectionsDirty();
        }
        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState updated = AirtightPipeConnections.update(state, level, pos, Axis.Y);
        if (updated != state) {
            level.setBlockAndUpdate(pos, updated);
        }
        GasTransportBehaviour transport = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
        if (transport != null) {
            transport.markConnectionsDirty();
        }
        GasNetworkTopology.invalidate(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            GasTransportBehaviour transport = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
            if (transport != null) {
                transport.finalizePendingTransfersBeforeBlockRemoval();
            }
            GasNetworkTopology.invalidate(level, pos);
        }
        if (!state.is(newState.getBlock()) && !isMoving) {
            removeBracket(level, pos, true).ifPresent(stack -> popResource(level, pos, stack));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public Optional<ItemStack> removeBracket(BlockGetter level, BlockPos pos, boolean inOnReplacedContext) {
        BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(level, pos, BracketedBlockEntityBehaviour.TYPE);
        if (bracket == null) {
            return Optional.empty();
        }

        BlockState removed = bracket.removeBracket(inOnReplacedContext);
        if (removed == null) {
            return Optional.empty();
        }

        return Optional.of(new ItemStack(removed.getBlock()));
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return simpleCodec(AirtightPipeBlock::new);
    }
}
