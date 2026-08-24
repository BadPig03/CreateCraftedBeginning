package net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock.TeslaTurbineStructuralPosition;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineUtils;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineUtils.NozzlePort;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineNozzleBlock extends DirectionalBlock implements IBE<TeslaTurbineNozzleBlockEntity>, SimpleWaterloggedBlock, IWrenchable {
    public static final BooleanProperty CLOCKWISE = BooleanProperty.create("clockwise");

    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public TeslaTurbineNozzleBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(CLOCKWISE, false));
    }

    public static boolean isClockwise(Level level, Direction inwardDirection, BlockPos nozzlePos) {
        BlockPos structurePos = nozzlePos.relative(inwardDirection);
        BlockState structureState = level.getBlockState(structurePos);
        Axis structureAxis = structureState.getValue(TeslaTurbineStructuralBlock.AXIS);
        BlockPos masterPos = TeslaTurbineStructuralBlock.getMaster(structurePos, structureState);
        NozzlePort port = TeslaTurbineUtils.findNozzlePort(masterPos, structureAxis, nozzlePos);
        if (port == null) {
            throw new IllegalArgumentException("Invalid Tesla Turbine nozzle position: " + nozzlePos);
        }
        return port.clockwise();
    }

    public static boolean isInvalidPlacement(BlockGetter level, Direction inwardDirection, BlockPos nozzlePos) {
        BlockPos structurePos = nozzlePos.relative(inwardDirection);
        BlockState structureState = level.getBlockState(structurePos);
        if (!(structureState.getBlock() instanceof TeslaTurbineStructuralBlock)) {
            return true;
        }

        Axis structureAxis = structureState.getValue(TeslaTurbineStructuralBlock.AXIS);
        if (structureAxis == inwardDirection.getAxis()) {
            return true;
        }

        TeslaTurbineStructuralPosition structurePosition = structureState.getValue(TeslaTurbineStructuralBlock.STRUCTURAL_POSITION);
        return TeslaTurbineStructuralPosition.isMid(structurePosition) || hasOtherNozzle(level, structurePos, nozzlePos, structureAxis, structurePosition);
    }

    public static boolean hasOtherNozzle(BlockGetter level, BlockPos structurePos, BlockPos nozzlePos, Axis structureAxis, TeslaTurbineStructuralPosition structurePosition) {
        for (Direction candidateDirection : TeslaTurbineStructuralPosition.getPossiblePosition(structurePosition, structureAxis)) {
            BlockPos candidatePos = structurePos.relative(candidateDirection);
            if (candidatePos.equals(nozzlePos)) {
                continue;
            }

            if (level.getBlockState(candidatePos).getBlock() instanceof TeslaTurbineNozzleBlock) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(TeslaTurbineNozzleBlock::new);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placementState = super.getStateForPlacement(context);
        if (placementState == null) {
            return null;
        }

        Level level = context.getLevel();
        Direction inwardDirection = context.getClickedFace().getOpposite();
        BlockPos nozzlePos = context.getClickedPos();
        if (isInvalidPlacement(level, inwardDirection, nozzlePos)) {
            return null;
        }

        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return null;
        }
        return ProperWaterloggedBlock.withWater(level, placementState.setValue(FACING, inwardDirection.getOpposite()).setValue(CLOCKWISE, isClockwise(level, inwardDirection, nozzlePos)), nozzlePos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, CLOCKWISE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == state.getValue(FACING).getOpposite() && level instanceof Level concreteLevel) {
            scheduleValidation(concreteLevel, pos);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (state.is(oldState.getBlock()) && state.getValue(FACING) == oldState.getValue(FACING)) {
            return;
        }

        scheduleValidation(level, pos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        if (facing.getAxis() == Axis.Y) {
            return CCBShapes.TESLA_TURBINE_NOZZLE_VERTICAL.get(facing);
        }
        return CCBShapes.TESLA_TURBINE_NOZZLE.get(facing);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction inwardDirection = state.getValue(FACING).getOpposite();
        if (!isInvalidPlacement(level, inwardDirection, pos)) {
            return;
        }

        level.destroyBlock(pos, true);
    }

    @Override
    public Class<TeslaTurbineNozzleBlockEntity> getBlockEntityClass() {
        return TeslaTurbineNozzleBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TeslaTurbineNozzleBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.TESLA_TURBINE_NOZZLE.get();
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return null;
    }

    protected void scheduleValidation(Level level, BlockPos nozzlePos) {
        if (level.isClientSide || level.getBlockTicks().hasScheduledTick(nozzlePos, this)) {
            return;
        }

        level.scheduleTick(nozzlePos, this, 1);
    }
}