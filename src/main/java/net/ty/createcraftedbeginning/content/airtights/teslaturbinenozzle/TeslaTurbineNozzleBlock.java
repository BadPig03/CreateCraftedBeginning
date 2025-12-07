package net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
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
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock.StructuralPosition;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.data.CCBShapes;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock.calculateStructurePos;

public class TeslaTurbineNozzleBlock extends DirectionalBlock implements IBE<TeslaTurbineNozzleBlockEntity>, SimpleWaterloggedBlock, IWrenchable {
    public static final BooleanProperty CLOCKWISE = BooleanProperty.create("clockwise");

    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Set<Pair<Integer, Integer>> COUNTER_CLOCKWISE_OFFSETS = Set.of(Pair.of(-2, 1), Pair.of(-1, -2), Pair.of(1, 2), Pair.of(2, -1));

    public TeslaTurbineNozzleBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(CLOCKWISE, false));
    }

    public static boolean isClockwise(@NotNull Level level, Direction facing, @NotNull BlockPos nozzlePos) {
        BlockPos structurePos = nozzlePos.relative(facing);
        BlockState structuralState = level.getBlockState(structurePos);
        Axis structuralAxis = structuralState.getValue(TeslaTurbineStructuralBlock.AXIS);
        BlockPos masterPos = TeslaTurbineStructuralBlock.getMaster(structurePos, structuralState);
        for (Pair<Integer, Integer> offset : COUNTER_CLOCKWISE_OFFSETS) {
            BlockPos candidatePos = calculateStructurePos(masterPos, structuralAxis, offset.getFirst(), offset.getSecond());
            if (!candidatePos.equals(nozzlePos)) {
                continue;
            }

            return false;
        }

        return true;
    }

    public static boolean isInvalidPlacement(@NotNull Level level, Direction facing, @NotNull BlockPos nozzlePos) {
        BlockPos structurePos = nozzlePos.relative(facing);
        BlockState structuralState = level.getBlockState(structurePos);
        if (!(structuralState.getBlock() instanceof TeslaTurbineStructuralBlock)) {
            return true;
        }

        Axis structuralAxis = structuralState.getValue(TeslaTurbineStructuralBlock.AXIS);
        if (structuralAxis == facing.getAxis()) {
            return true;
        }

        StructuralPosition structuralPos = structuralState.getValue(TeslaTurbineStructuralBlock.STRUCTURAL_POSITION);
        if (StructuralPosition.isMid(structuralPos)) {
            return true;
        }

        Set<Direction> directionsToCheck = StructuralPosition.getPossiblePosition(structuralPos, structuralAxis);
        for (Direction direction : directionsToCheck) {
            BlockPos candidatePos = structurePos.relative(direction);
            if (candidatePos.equals(nozzlePos)) {
                continue;
            }

            BlockState candidateState = level.getBlockState(candidatePos);
            if (candidateState.getBlock() == CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK.get()) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected @NotNull MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(TeslaTurbineNozzleBlock::new);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        Level level = context.getLevel();
        Direction direction = context.getClickedFace().getOpposite();
        BlockPos clickedPos = context.getClickedPos();
        if (isInvalidPlacement(level, direction, clickedPos)) {
            return null;
        }

        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return null;
        }

        state = state.setValue(FACING, direction).setValue(CLOCKWISE, isClockwise(level, direction, clickedPos));
        return ProperWaterloggedBlock.withWater(level, state, clickedPos);
    }

    @Override
    protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, CLOCKWISE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull BlockState blockState, LivingEntity placer, @NotNull ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, placer, itemStack);
        CCBAdvancementBehaviour.setPlacedBy(level, blockPos, placer);
    }

    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return facing.getAxis() == Axis.Y ? CCBShapes.TESLA_TURBINE_NOZZLE_VERTICAL.get(facing) : CCBShapes.TESLA_TURBINE_NOZZLE.get(facing);
    }

    @Override
    public Class<TeslaTurbineNozzleBlockEntity> getBlockEntityClass() {
        return TeslaTurbineNozzleBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TeslaTurbineNozzleBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.TESLA_TURBINE_NOZZLE.get();
    }
}