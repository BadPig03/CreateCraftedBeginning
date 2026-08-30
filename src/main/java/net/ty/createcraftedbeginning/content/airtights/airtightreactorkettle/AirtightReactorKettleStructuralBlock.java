package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleStructuralBlock extends Block implements IBE<AirtightReactorKettleStructuralBlockEntity>, IWrenchable, IProxyHoveringInformation, IAirtightComponent, IAirtightReactorKettleStructural {
    public static final EnumProperty<AirtightReactorKettleStructuralPosition> STRUCTURAL_POSITION = EnumProperty.create("structural_position", AirtightReactorKettleStructuralPosition.class);

    public AirtightReactorKettleStructuralBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STRUCTURAL_POSITION, AirtightReactorKettleStructuralPosition.TOP_CENTER));
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

        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(clickedPos, state);
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();
        ItemStack heldStack = context.getItemInHand();
        BlockHitResult masterHit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), masterPos, context.isInside());
        UseOnContext masterContext = new UseOnContext(level, player, hand, heldStack, masterHit);
        BlockState masterState = level.getBlockState(masterPos);
        return IWrenchable.super.onSneakWrenched(masterState, masterContext);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!stillValid(level, pos, state)) {
            return super.playerWillDestroy(level, pos, state, player);
        }

        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(pos, state);
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
        return CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.asStack();
    }

    @Override
    public boolean addLandingEffects(BlockState state, ServerLevel level, BlockPos pos, BlockState landingState, LivingEntity entity, int particleCount) {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor accessor, BlockPos pos, BlockPos neighborPos) {
        if (stillValid(accessor, pos, state)) {
            BlockPos masterPos = AirtightReactorKettleUtils.getMaster(pos, state);
            if (!accessor.getBlockTicks().hasScheduledTick(masterPos, CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.get())) {
                accessor.scheduleTick(masterPos, CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.get(), 1);
            }
            return state;
        }

        if (!(accessor instanceof Level level) || level.isClientSide) {
            return state;
        }

        if (level.getBlockTicks().hasScheduledTick(pos, this)) {
            return state;
        }

        level.scheduleTick(pos, this, 1);
        return state;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (state.is(newState.getBlock()) || !stillValid(level, pos, state)) {
            return;
        }

        level.destroyBlock(AirtightReactorKettleUtils.getMaster(pos, state), true);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(STRUCTURAL_POSITION).canStore() || hit.getDirection() == Direction.DOWN) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return onBlockEntityUseItemOn(level, pos, structural -> AirtightReactorKettleUtils.getUseItemOnResult(structural, level, player, pos, hand, stack));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        AirtightReactorKettleStructuralPosition structuralPosition = state.getValue(STRUCTURAL_POSITION);
        VoxelShape baseShape = AirtightReactorKettleVoxelShapes.getShape(structuralPosition);
        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(pos, state);
        if (!(level.getBlockEntity(masterPos) instanceof AirtightReactorKettleBlockEntity kettle) || kettle.getWindowsOpenState()) {
            return baseShape;
        }

        if (!structuralPosition.isWindow(0)) {
            return baseShape;
        }
        return CCBShapes.AIRTIGHT_REACTOR_KETTLE_MID_MID_CLOSED.get(structuralPosition.getDirection());
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
        if (!blockState.getValue(STRUCTURAL_POSITION).canStore()) {
            return;
        }

        if (entity instanceof ItemEntity itemEntity && itemEntity.isAlive() && itemEntity.onGround()) {
            withBlockEntityDo(level, blockPos, structuralEntity -> AirtightReactorKettleUtils.insertItemEntity(structuralEntity, itemEntity));
            return;
        }

        if (!(entity instanceof LivingEntity livingEntity) || !new AABB(blockPos).deflate(0.1).intersects(livingEntity.getBoundingBox())) {
            return;
        }

        withBlockEntityDo(level, blockPos, structuralEntity -> AirtightReactorKettleUtils.hurtInsideLivingEntities(structuralEntity, livingEntity));
    }

    @Override
    public boolean stillValid(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof AirtightReactorKettleStructuralBlock)) {
            return false;
        }

        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(pos, state);
        return level.getBlockState(masterPos).getBlock() instanceof AirtightReactorKettleBlock;
    }

    @Override
    public EnumProperty<AirtightReactorKettleStructuralPosition> getStructuralPosition() {
        return STRUCTURAL_POSITION;
    }

    @Override
    public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        if (!stillValid(level, pos, state)) {
            return pos;
        }
        return AirtightReactorKettleUtils.getMaster(pos, state);
    }

    @Override
    public Class<AirtightReactorKettleStructuralBlockEntity> getBlockEntityClass() {
        return AirtightReactorKettleStructuralBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightReactorKettleStructuralBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL.get();
    }

    @Override
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return true;
    }
}
