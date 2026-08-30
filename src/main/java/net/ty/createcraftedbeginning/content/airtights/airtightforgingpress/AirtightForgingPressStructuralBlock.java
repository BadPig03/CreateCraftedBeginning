package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressStructuralBlock extends Block implements IBE<AirtightForgingPressStructuralBlockEntity>, IWrenchable, IProxyHoveringInformation, IAirtightComponent, IAirtightForgingPressStructural {
    public static final EnumProperty<AirtightForgingPressStructuralPosition> STRUCTURAL_POSITION = EnumProperty.create("structural_position", AirtightForgingPressStructuralPosition.class);

    public AirtightForgingPressStructuralBlock(Properties properties) {
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
            return IWrenchable.super.onSneakWrenched(state, context);
        }

        BlockPos masterPos = AirtightForgingPressUtils.getMaster(clickedPos, state);
        BlockHitResult masterHitResult = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), masterPos, context.isInside());
        UseOnContext masterContext = new UseOnContext(level, context.getPlayer(), context.getHand(), context.getItemInHand(), masterHitResult);
        BlockState masterState = level.getBlockState(masterPos);
        return IWrenchable.super.onSneakWrenched(masterState, masterContext);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!stillValid(level, pos, state)) {
            return super.playerWillDestroy(level, pos, state, player);
        }

        BlockPos masterPos = AirtightForgingPressUtils.getMaster(pos, state);
        level.destroyBlockProgress(masterPos.hashCode(), masterPos, -1);
        if (level.isClientSide || !player.isCreative()) {
            return super.playerWillDestroy(level, pos, state, player);
        }

        level.destroyBlock(masterPos, false);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(STRUCTURAL_POSITION);
        super.createBlockStateDefinition(builder);
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
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor accessor, BlockPos pos, BlockPos neighborPos) {
        if (!stillValid(accessor, pos, state)) {
            if (accessor instanceof Level level && !level.isClientSide && !level.getBlockTicks().hasScheduledTick(pos, this)) {
                level.scheduleTick(pos, this, 1);
            }
            return state;
        }

        BlockPos masterPos = AirtightForgingPressUtils.getMaster(pos, state);
        Block masterBlock = CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK.get();
        if (accessor.getBlockTicks().hasScheduledTick(masterPos, masterBlock)) {
            return state;
        }

        accessor.scheduleTick(masterPos, masterBlock, 1);
        return state;
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
        if (!blockState.getValue(STRUCTURAL_POSITION).isLowerStore() || hitResult.getDirection() == Direction.DOWN) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return onBlockEntityUseItemOn(level, blockPos, structuralPart -> AirtightForgingPressUtils.getUseItemOnResult(structuralPart, level, player, blockPos, hand, stack));
    }

    @Override
    protected VoxelShape getShape(BlockState blockState, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        return AirtightForgingPressVoxelShapes.getShape(blockState.getValue(STRUCTURAL_POSITION));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (stillValid(level, pos, state)) {
            return;
        }

        level.destroyBlock(pos, false);
    }

    @Override
    protected void entityInside(BlockState blockState, Level level, BlockPos blockPos, Entity entity) {
        if (!blockState.getValue(STRUCTURAL_POSITION).isLowerStore() || !(entity instanceof ItemEntity itemEntity) || !itemEntity.isAlive() || !itemEntity.onGround()) {
            return;
        }

        withBlockEntityDo(level, blockPos, structural -> AirtightForgingPressUtils.insertItemEntity(structural, itemEntity));
    }

    @Override
    public boolean stillValid(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof AirtightForgingPressStructuralBlock)) {
            return false;
        }

        BlockPos masterPos = AirtightForgingPressUtils.getMaster(pos, state);
        return level.getBlockState(masterPos).getBlock() instanceof AirtightForgingPressBlock;
    }

    @Override
    public EnumProperty<AirtightForgingPressStructuralPosition> getStructuralPosition() {
        return STRUCTURAL_POSITION;
    }

    @Override
    public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        if (!stillValid(level, pos, state)) {
            return pos;
        }
        return AirtightForgingPressUtils.getMaster(pos, state);
    }

    @Override
    public Class<AirtightForgingPressStructuralBlockEntity> getBlockEntityClass() {
        return AirtightForgingPressStructuralBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightForgingPressStructuralBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL.get();
    }

    @Override
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return true;
    }
}
