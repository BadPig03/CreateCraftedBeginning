package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressStructuralShaftBlock extends KineticBlock implements IBE<AirtightForgingPressStructuralShaftBlockEntity>, IWrenchable, IProxyHoveringInformation, ICogWheel, IAirtightForgingPressStructural {
    public static final EnumProperty<AirtightForgingPressStructuralPosition> STRUCTURAL_POSITION = EnumProperty.create("structural_position", AirtightForgingPressStructuralPosition.class);

    public AirtightForgingPressStructuralShaftBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STRUCTURAL_POSITION, AirtightForgingPressStructuralPosition.TOP_CENTER));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();
        if (!stillValid(level, clickedPos, state)) {
            return super.onSneakWrenched(state, context);
        }

        BlockPos masterPos = AirtightForgingPressUtils.getMaster(clickedPos, state);
        context = new UseOnContext(level, context.getPlayer(), context.getHand(), context.getItemInHand(), new BlockHitResult(context.getClickLocation(), context.getClickedFace(), masterPos, context.isInside()));
        state = level.getBlockState(masterPos);
        return super.onSneakWrenched(state, context);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK);
    }

    @Override
    public boolean addLandingEffects(BlockState blockState1, ServerLevel level, BlockPos pos, BlockState blockState2, LivingEntity entity, int numberOfParticles) {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!stillValid(level, pos, state)) {
            return super.playerWillDestroy(level, pos, state, player);
        }

        BlockPos masterPos = AirtightForgingPressUtils.getMaster(pos, state);
        level.destroyBlockProgress(masterPos.hashCode(), masterPos, -1);
        if (!level.isClientSide && player.isCreative()) {
            level.destroyBlock(masterPos, false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(STRUCTURAL_POSITION);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor accessor, BlockPos pos, BlockPos neighborPos) {
        if (stillValid(accessor, pos, state)) {
            BlockPos masterPos = AirtightForgingPressUtils.getMaster(pos, state);
            if (!accessor.getBlockTicks().hasScheduledTick(masterPos, CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK.get())) {
                accessor.scheduleTick(masterPos, CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK.get(), 1);
            }
            return state;
        }
        if (!(accessor instanceof Level level) || level.isClientSide) {
            return state;
        }

        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState blockState, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        AirtightForgingPressStructuralPosition structuralPosition = blockState.getValue(STRUCTURAL_POSITION);
        return AirtightForgingPressVoxelShapes.getShape(structuralPosition);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (stillValid(level, pos, state)) {
            return;
        }

        level.destroyBlock(pos, false);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (state.is(newState.getBlock()) || !stillValid(level, pos, state)) {
            return;
        }

        level.destroyBlock(AirtightForgingPressUtils.getMaster(pos, state), true);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (blockState.getValue(STRUCTURAL_POSITION) != AirtightForgingPressStructuralPosition.TOP_CENTER || hitResult.getDirection() == Direction.DOWN) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return onBlockEntityUseItemOn(level, blockPos, be -> AirtightForgingPressUtils.getUseItemOnResult(be, level, player, blockPos, hand, stack));
    }

    @Override
    public boolean stillValid(BlockGetter level, BlockPos pos, BlockState state) {
        return state.is(this) && level.getBlockState(AirtightForgingPressUtils.getMaster(pos, state)).is(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK);
    }

    @Override
    public EnumProperty<AirtightForgingPressStructuralPosition> getStructuralPosition() {
        return STRUCTURAL_POSITION;
    }

    @Override
    public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        return stillValid(level, pos, state) ? AirtightForgingPressUtils.getMaster(pos, state) : pos;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        AirtightForgingPressStructuralPosition structuralPosition = state.getValue(STRUCTURAL_POSITION);
        return structuralPosition.getAxis();
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.FAST;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction direction) {
        AirtightForgingPressStructuralPosition structuralPosition = state.getValue(STRUCTURAL_POSITION);
        Axis axis = direction.getAxis();
        if (structuralPosition == AirtightForgingPressStructuralPosition.TOP_CENTER) {
            return axis != Axis.Y;
        }

        return axis == structuralPosition.getAxis();
    }

    @Override
    public Class<AirtightForgingPressStructuralShaftBlockEntity> getBlockEntityClass() {
        return AirtightForgingPressStructuralShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightForgingPressStructuralShaftBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT.get();
    }
}
