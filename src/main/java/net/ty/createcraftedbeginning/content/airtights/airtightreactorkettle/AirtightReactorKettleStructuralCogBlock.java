package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

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
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleStructuralCogBlock extends KineticBlock implements IBE<AirtightReactorKettleStructuralCogBlockEntity>, IWrenchable, IProxyHoveringInformation, ICogWheel, IAirtightReactorKettleStructural {
    public static final EnumProperty<AirtightReactorKettleStructuralPosition> STRUCTURAL_POSITION = EnumProperty.create("structural_position", AirtightReactorKettleStructuralPosition.class);

    public AirtightReactorKettleStructuralCogBlock(Properties properties) {
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
            return super.onSneakWrenched(state, context);
        }

        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(clickedPos, state);
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();
        ItemStack stack = context.getItemInHand();
        BlockHitResult masterHit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), masterPos, context.isInside());
        UseOnContext masterContext = new UseOnContext(level, player, hand, stack, masterHit);
        BlockState masterState = level.getBlockState(masterPos);
        return super.onSneakWrenched(masterState, masterContext);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK);
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        AirtightReactorKettleStructuralPosition position = state.getValue(STRUCTURAL_POSITION);
        VoxelShape shape = AirtightReactorKettleVoxelShapes.getShape(position);
        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(pos, state);
        if (!(level.getBlockEntity(masterPos) instanceof AirtightReactorKettleBlockEntity master) || master.getWindowsOpenState()) {
            return shape;
        }

        if (!position.isWindow(-1)) {
            return shape;
        }
        return CCBShapes.AIRTIGHT_REACTOR_KETTLE_TOP_MID_CLOSED.get(position.getDirection());
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

        level.destroyBlock(AirtightReactorKettleUtils.getMaster(pos, state), true);
    }

    @Override
    public boolean stillValid(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof AirtightReactorKettleStructuralCogBlock)) {
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
        return stillValid(level, pos, state) ? AirtightReactorKettleUtils.getMaster(pos, state) : pos;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.FAST;
    }

    @Override
    public Class<AirtightReactorKettleStructuralCogBlockEntity> getBlockEntityClass() {
        return AirtightReactorKettleStructuralCogBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightReactorKettleStructuralCogBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG.get();
    }
}
