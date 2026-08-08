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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.coolantshandlers.CoolantEfficiency;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirCompressorBlock extends HorizontalKineticBlock implements IBE<AirCompressorBlockEntity>, SimpleWaterloggedBlock, IWrenchable, IAirtightComponent {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirCompressorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false).setValue(WATERLOGGED, false));
    }

    public static Direction getInputSide(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getClockWise();
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        return ProperWaterloggedBlock.withWater(context.getLevel(), AirCompressorUtils.getStateForBasicPlacement(context, state), context.getClickedPos());
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
        if (!level.isClientSide && player.isCreative()) {
            dropStoredState(level, pos);
        }

        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        OverheatState overheatState = OverheatState.fromItem(stack);
        tooltips.add(CCBLang.translate("gui.air_compressor.overheat_state").style(ChatFormatting.GRAY).add(CCBLang.translate(overheatState.getTranslationKey()).style(overheatState.getDisplayColor())).component());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (!state.getValue(WATERLOGGED)) {
            return state;
        }

        level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
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
        List<ItemStack> drops = super.getDrops(state, params);
        if (!(params.getParameter(LootContextParams.BLOCK_ENTITY) instanceof AirCompressorBlockEntity compressor) || compressor.getStoredHeat() == 0) {
            return drops;
        }

        for (ItemStack drop : drops) {
            if (!drop.is(asItem())) {
                continue;
            }

            compressor.saveToItem(drop);
            break;
        }
        return drops;
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
        CoolantEfficiency efficiency = AirCompressorUtils.tickCoolant(level, coolantPos, state.getValue(ACTIVE), random);
        compressor.setCoolantEfficiency(efficiency);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(ACTIVE);
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

    private void dropStoredState(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof AirCompressorBlockEntity compressor) || compressor.getStoredHeat() == 0) {
            return;
        }

        ItemStack item = new ItemStack(this);
        compressor.saveToItem(item);
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        level.setBlockAndUpdate(pos, state.setValue(HORIZONTAL_FACING, state.getValue(HORIZONTAL_FACING).getOpposite()));
        if (!level.isClientSide) {
            level.invalidateCapabilities(pos);
        }
        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }
}
