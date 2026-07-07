package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandler;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates.IOverheatState;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates.NormalOverheatState;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates.OverheatManager;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirCompressorBlock extends HorizontalKineticBlock implements IBE<AirCompressorBlockEntity>, SimpleWaterloggedBlock, IWrenchable, IAirtightComponent {
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final float COOLANT_CONSUME_CHANCE = 0.5f;

    public AirCompressorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    public static Direction getInputSide(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getClockWise();
    }

    private static BlockState getStateForBasicPlacement(BlockPlaceContext context, BlockState state) {
        Direction opposite = context.getHorizontalDirection().getOpposite();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return state.setValue(HORIZONTAL_FACING, opposite);
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
                return facing.getAxis() == Axis.Y ? state.setValue(HORIZONTAL_FACING, opposite) : state.setValue(HORIZONTAL_FACING, facing.getClockWise());
            }
            case AirtightPipeBlock ignored -> {
                Axis axis = otherState.getValue(AirtightPipeBlock.AXIS);
                boolean reverse = clickDirection.getAxisDirection() == AxisDirection.NEGATIVE;
                return switch (axis) {
                    case X -> state.setValue(HORIZONTAL_FACING, reverse ? Direction.SOUTH : Direction.NORTH);
                    case Y -> state.setValue(HORIZONTAL_FACING, opposite);
                    case Z -> state.setValue(HORIZONTAL_FACING, reverse ? Direction.EAST : Direction.WEST);
                };
            }
            case SmartAirtightPipeBlock ignored -> {
                Axis axis = otherState.getValue(AirtightPipeBlock.AXIS);
                boolean reverse = clickDirection.getAxisDirection() == AxisDirection.NEGATIVE;
                return switch (axis) {
                    case X -> state.setValue(HORIZONTAL_FACING, reverse ? Direction.SOUTH : Direction.NORTH);
                    case Y -> state.setValue(HORIZONTAL_FACING, opposite);
                    case Z -> state.setValue(HORIZONTAL_FACING, reverse ? Direction.EAST : Direction.WEST);
                };
            }
            case AirtightCheckValveBlock ignored -> {
                Axis axis = otherState.getValue(AirtightCheckValveBlock.AXIS);
                boolean inverted = otherState.getValue(AirtightCheckValveBlock.INVERTED);
                return switch (axis) {
                    case X -> state.setValue(HORIZONTAL_FACING, inverted ? Direction.NORTH : Direction.SOUTH);
                    case Y -> state.setValue(HORIZONTAL_FACING, opposite);
                    case Z -> state.setValue(HORIZONTAL_FACING, inverted ? Direction.WEST : Direction.EAST);
                };
            }
            default -> {
                return state.setValue(HORIZONTAL_FACING, opposite);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        return ProperWaterloggedBlock.withWater(context.getLevel(), getStateForBasicPlacement(context, state), context.getClickedPos());
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor && !(compressor.getOverheatState() instanceof NormalOverheatState)) {
            ItemStack item = new ItemStack(this);
            compressor.saveToItem(item);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item);
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    @Override
    public void appendHoverText(ItemStack compressor, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        IOverheatState overheatState = OverheatManager.getStateByItem(compressor);
        tooltips.add(CCBLang.translate("gui.tooltips.air_compressor.overheat_state").style(ChatFormatting.GRAY).add(CCBLang.translate(overheatState.getTranslationKey()).style(overheatState.getDisplayColor())).component());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, otherBlock, neighborPos, isMoving);
        if (!pos.below().equals(neighborPos) || !(level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor)) {
            return;
        }

        compressor.updateCoolant(neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        if (!state.getValue(WATERLOGGED)) {
            return super.getFluidState(state);
        }

        return Fluids.WATER.defaultFluidState();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.@NotNull Builder params) {
        if (!(params.getParameter(LootContextParams.BLOCK_ENTITY) instanceof AirCompressorBlockEntity compressor) || compressor.getOverheatState() instanceof NormalOverheatState) {
            return super.getDrops(state, params);
        }

        ItemStack item = new ItemStack(this);
        compressor.saveToItem(item);
        return Collections.singletonList(item);
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor)) {
            return 0;
        }

        return compressor.getAnalogOutputSignal();
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (!(level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor)) {
            return;
        }

        BlockPos coolantPos = pos.below();
        compressor.updateCoolant(coolantPos);
        CoolantEfficiency efficiency = compressor.getCoolantEfficiency();
        if (efficiency == CoolantEfficiency.NONE || !compressor.shouldConsumeCoolant() || random.nextFloat() >= COOLANT_CONSUME_CHANCE) {
            return;
        }

        BlockState coolantState = level.getBlockState(coolantPos);
        AirtightCoolantHandler coolantStrategy = AirtightCoolantHandlerUtils.of(coolantState.getBlock());
        BlockState newState = coolantStrategy.getMeltBlockState(level, coolantPos, coolantState);
        if (newState != null) {
            level.destroyBlock(coolantPos, false);
            if (!newState.isAir()) {
                level.setBlockAndUpdate(coolantPos, newState);
            }
        }
        compressor.updateCoolant(coolantPos);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.UP;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
        if (!(level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor)) {
            return;
        }

        compressor.loadFromItem(stack);
        compressor.updateCoolant(pos.below());
    }

    @Override
    public boolean isAirtight(BlockPos currentPos, BlockState currentState, Direction oppositeDirection) {
        return getInputSide(currentState).getAxis() == oppositeDirection.getAxis();
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
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        level.setBlockAndUpdate(pos, state.setValue(HORIZONTAL_FACING, getInputSide(state).getOpposite().getCounterClockWise()));
        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack item = new ItemStack(this);
        if (!(level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor) || compressor.getOverheatState() instanceof NormalOverheatState || !player.isShiftKeyDown()) {
            return item;
        }

        compressor.saveToItem(item);
        return item;
    }
}
