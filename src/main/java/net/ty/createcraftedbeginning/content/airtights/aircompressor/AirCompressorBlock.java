package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.coolantstrategy.CoolantStrategyHandler;
import net.ty.createcraftedbeginning.api.gas.gases.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity.CoolantEfficiency;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates.IOverheatState;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates.NormalOverheatState;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates.OverheatManager;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class AirCompressorBlock extends HorizontalKineticBlock implements IBE<AirCompressorBlockEntity>, SimpleWaterloggedBlock, IWrenchable, IAirtightComponent {
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final float RANDOM_TICK_POSSIBILITY = 0.5f;

    public AirCompressorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    public static @NotNull Direction getInputSide(@NotNull BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getClockWise();
    }

    private static @NotNull BlockState getStateForBasicPlacement(@NotNull BlockPlaceContext context, BlockState state) {
        Direction horizontalDirection = context.getHorizontalDirection().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            return state.setValue(HORIZONTAL_FACING, horizontalDirection);
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clickDirection = context.getClickedFace().getOpposite();
        BlockPos otherPos = pos.relative(clickDirection);
        BlockState otherState = level.getBlockState(otherPos);
        Block otherBlock = otherState.getBlock();
        switch (otherBlock) {
            case AirtightPumpBlock ignored -> {
                Direction facing = otherState.getValue(AirtightPumpBlock.FACING);
                return facing.getAxis() == Axis.Y ? state.setValue(HORIZONTAL_FACING, horizontalDirection) : state.setValue(HORIZONTAL_FACING, facing.getClockWise());
            }
            case AirtightPipeBlock ignored -> {
                Axis axis = otherState.getValue(AirtightPipeBlock.AXIS);
                boolean reverse = clickDirection.getAxisDirection() == AxisDirection.NEGATIVE;
                return switch (axis) {
                    case X -> state.setValue(HORIZONTAL_FACING, reverse ? Direction.SOUTH : Direction.NORTH);
                    case Y -> state.setValue(HORIZONTAL_FACING, horizontalDirection);
                    case Z -> state.setValue(HORIZONTAL_FACING, reverse ? Direction.EAST : Direction.WEST);
                };
            }
            case SmartAirtightPipeBlock ignored -> {
                Axis axis = otherState.getValue(AirtightPipeBlock.AXIS);
                boolean reverse = clickDirection.getAxisDirection() == AxisDirection.NEGATIVE;
                return switch (axis) {
                    case X -> state.setValue(HORIZONTAL_FACING, reverse ? Direction.SOUTH : Direction.NORTH);
                    case Y -> state.setValue(HORIZONTAL_FACING, horizontalDirection);
                    case Z -> state.setValue(HORIZONTAL_FACING, reverse ? Direction.EAST : Direction.WEST);
                };
            }
            case AirtightCheckValveBlock ignored -> {
                Axis axis = otherState.getValue(AirtightCheckValveBlock.AXIS);
                boolean inverted = otherState.getValue(AirtightCheckValveBlock.INVERTED);
                return switch (axis) {
                    case X -> state.setValue(HORIZONTAL_FACING, inverted ? Direction.NORTH : Direction.SOUTH);
                    case Y -> state.setValue(HORIZONTAL_FACING, horizontalDirection);
                    case Z -> state.setValue(HORIZONTAL_FACING, inverted ? Direction.WEST : Direction.EAST);
                };
            }
            default -> {
                return state.setValue(HORIZONTAL_FACING, horizontalDirection);
            }
        }
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public InteractionResult onWrenched(@NotNull BlockState state, @NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        level.setBlockAndUpdate(pos, state.setValue(HORIZONTAL_FACING, getInputSide(state).getOpposite().getCounterClockWise()));
        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.UP;
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        ItemStack compressorItemEntity = new ItemStack(this);
        if (!(level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor) || compressor.getOverheatState() instanceof NormalOverheatState || !player.isShiftKeyDown()) {
            return compressorItemEntity;
        }

        compressor.saveToItem(compressorItemEntity);
        return compressorItemEntity;
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity entity, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
        if (!(level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor)) {
            return;
        }

        compressor.loadFromItem(stack);
        compressor.updateCoolant(pos.below());
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor && !(compressor.getOverheatState() instanceof NormalOverheatState)) {
            ItemStack compressorItemEntity = new ItemStack(this);
            compressor.saveToItem(compressorItemEntity);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, compressorItemEntity);
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack compressor, @NotNull TooltipContext context, @NotNull List<Component> tooltips, @NotNull TooltipFlag flag) {
        String stateName = compressor.getOrDefault(CCBDataComponents.AIR_COMPRESSOR_OVERHEAT_STATE, OverheatManager.NORMAL.getSerializedName());
        IOverheatState overheatState = OverheatManager.getStateByName(stateName);
        tooltips.add(CCBLang.translate("gui.tooltips.air_compressor.overheat_state").style(ChatFormatting.GRAY).add(CCBLang.translate(overheatState.getTranslationKey()).style(overheatState.getDisplayColor())).component());
    }

    @Override
    public Class<AirCompressorBlockEntity> getBlockEntityClass() {
        return AirCompressorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirCompressorBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIR_COMPRESSOR.get();
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbourState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block otherBlock, @NotNull BlockPos neighborPos, boolean isMoving) {
        if (!pos.below().equals(neighborPos) || !(level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor)) {
            return;
        }

        compressor.updateCoolant(neighborPos);
    }

    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder params) {
        if (!(params.getParameter(LootContextParams.BLOCK_ENTITY) instanceof AirCompressorBlockEntity compressor) || compressor.getOverheatState() instanceof NormalOverheatState) {
            return super.getDrops(state, params);
        }

        ItemStack compressorItemEntity = new ItemStack(this);
        compressor.saveToItem(compressorItemEntity);
        return Collections.singletonList(compressorItemEntity);
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor ? compressor.getAnalogOutputSignal() : 0;
    }

    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (!(level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor)) {
            return;
        }

        compressor.updateCoolant(pos.below());
        if (random.nextFloat() < RANDOM_TICK_POSSIBILITY) {
            return;
        }

        CoolantEfficiency coolantEfficiency = compressor.getCoolantEfficiency();
        if (coolantEfficiency == CoolantEfficiency.NONE) {
            return;
        }

        BlockPos coolantPos = pos.below();
        BlockState coolantState = level.getBlockState(coolantPos);
        CoolantStrategyHandler coolantStrategy = CoolantStrategyHandler.REGISTRY.get(coolantState.getBlock());
        BlockState newState = coolantStrategy == null ? Blocks.AIR.defaultBlockState() : coolantStrategy.getMeltBlockState(level, coolantPos, coolantState);
        if (newState != null) {
            level.destroyBlock(coolantPos, false);
            if (!newState.isAir()) {
                level.setBlockAndUpdate(coolantPos, newState);
            }
        }
        compressor.updateCoolant(coolantPos);
    }

    @Override
    public boolean isRandomlyTicking(@NotNull BlockState state) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        return ProperWaterloggedBlock.withWater(context.getLevel(), getStateForBasicPlacement(context, state), context.getClickedPos());
    }

    @Override
    public boolean isAirtight(BlockPos currentPos, @NotNull BlockState currentState, @NotNull Direction oppositeDirection) {
        return getInputSide(currentState).getAxis() == oppositeDirection.getAxis();
    }
}
