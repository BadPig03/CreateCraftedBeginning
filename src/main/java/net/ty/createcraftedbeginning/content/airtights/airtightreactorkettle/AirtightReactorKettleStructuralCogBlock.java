package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

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
    public InteractionResult onSneakWrenched(BlockState state, @NotNull UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();
        if (!stillValid(level, clickedPos, state)) {
            return super.onSneakWrenched(state, context);
        }

        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(clickedPos, state);
        context = new UseOnContext(level, context.getPlayer(), context.getHand(), context.getItemInHand(), new BlockHitResult(context.getClickLocation(), context.getClickedFace(), masterPos, context.isInside()));
        state = level.getBlockState(masterPos);
        return super.onSneakWrenched(state, context);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        return CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.asStack();
    }

    @Override
    public boolean addLandingEffects(@NotNull BlockState blockState1, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState blockState2, @NotNull LivingEntity entity, int numberOfParticles) {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction(@NotNull BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        if (!stillValid(level, pos, state)) {
            return super.playerWillDestroy(level, pos, state, player);
        }

        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(pos, state);
        level.destroyBlockProgress(masterPos.hashCode(), masterPos, -1);
        if (!level.isClientSide && player.isCreative()) {
            level.destroyBlock(masterPos, false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> builder) {
        builder.add(STRUCTURAL_POSITION);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor accessor, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
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

        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext context) {
        AirtightReactorKettleStructuralPosition structuralPosition = blockState.getValue(STRUCTURAL_POSITION);
        VoxelShape shape = AirtightReactorKettleVoxelShapes.getShape(structuralPosition);
        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(blockPos, blockState);
        if (!(level.getBlockEntity(masterPos) instanceof AirtightReactorKettleBlockEntity masterBlockEntity) || masterBlockEntity.getWindowsOpenState()) {
            return shape;
        }
        if (!structuralPosition.isWindow(-1)) {
            return shape;
        }

        return CCBShapes.AIRTIGHT_REACTOR_KETTLE_TOP_MID_CLOSED.get(structuralPosition.getDirection());
    }

    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (stillValid(level, pos, state)) {
            return;
        }

        level.destroyBlock(pos, false);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!stillValid(level, pos, state)) {
            return;
        }

        level.destroyBlock(AirtightReactorKettleUtils.getMaster(pos, state), true);
    }

    @Override
    public boolean stillValid(BlockGetter level, BlockPos pos, @NotNull BlockState state) {
        return state.is(this) && level.getBlockState(AirtightReactorKettleUtils.getMaster(pos, state)).is(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK);
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
