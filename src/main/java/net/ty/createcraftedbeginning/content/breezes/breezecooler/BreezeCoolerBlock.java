package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeCoolerBlock extends HorizontalDirectionalBlock implements IBE<BreezeCoolerBlockEntity>, SimpleWaterloggedBlock, IWrenchable {
    public static final EnumProperty<FrostLevel> FROST_LEVEL = EnumProperty.create("frost_level", FrostLevel.class);
    public static final BooleanProperty ATTACHED = BooleanProperty.create("attached");
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public BreezeCoolerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(FROST_LEVEL, FrostLevel.RIMING).setValue(ATTACHED, false));
    }

    public static FrostLevel getFrostLevelOf(BlockState blockState) {
        return blockState.getValue(FROST_LEVEL);
    }

    private static ItemInteractionResult setGoggles(BreezeCoolerBlockEntity cooler, boolean goggles) {
        if (cooler.hasGoggles() == goggles) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        cooler.setGoggles(goggles);
        cooler.notifyUpdate();
        return ItemInteractionResult.SUCCESS;
    }

    public static InteractionResultHolder<ItemStack> tryInsert(BlockState state, Level level, BlockPos pos, ItemStack stack, boolean doNotConsume, boolean forceOverflow, boolean simulate) {
        if (!state.hasBlockEntity()) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }

        if (!(level.getBlockEntity(pos) instanceof BreezeCoolerBlockEntity cooler)) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }

        if (!cooler.tryUpdateCoolantByItem(stack, forceOverflow, simulate)) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }

        if (doNotConsume) {
            return InteractionResultHolder.success(ItemStack.EMPTY);
        }

        ItemStack container;
        if (stack.getItem() instanceof DispensibleContainerItem) {
            container = new ItemStack(Items.BUCKET);
        }
        else {
            container = stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
        }
        if (simulate || level.isClientSide) {
            return InteractionResultHolder.success(container);
        }

        stack.shrink(1);
        return InteractionResultHolder.success(container);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor world, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        if (direction != Direction.UP) {
            return state;
        }

        state = state.setValue(ATTACHED, neighbourState.is(CCBBlocks.AIR_COMPRESSOR_BLOCK.get()));
        return state;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (AllItems.GOGGLES.isIn(stack)) {
            return onBlockEntityUseItemOn(level, pos, cooler -> setGoggles(cooler, true));
        }

        BreezeCoolerBlockEntity cooler = getBlockEntity(level, pos);
        if (cooler != null && cooler.isStockKeeper()) {
            StockTickerBlockEntity stockTicker = BlazeBurnerBlockEntity.getStockTicker(level, pos);
            if (stockTicker != null) {
                StockTickerInteractionHandler.interactWithLogisticsManagerAt(player, level, stockTicker.getBlockPos());
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.isEmpty()) {
            return onBlockEntityUseItemOn(level, pos, blockEntity -> setGoggles(blockEntity, false));
        }

        boolean doNotConsume = player.isCreative();
        boolean forceOverflow = !(player instanceof FakePlayer);
        InteractionResultHolder<ItemStack> resultHolder = tryInsert(state, level, pos, stack, doNotConsume, forceOverflow, false);
        ItemInteractionResult interactionResult = resultHolder.getResult() == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        ItemStack leftover = resultHolder.getObject();
        if (level.isClientSide || doNotConsume || leftover.isEmpty()) {
            return interactionResult;
        }

        if (stack.isEmpty()) {
            player.setItemInHand(hand, leftover);
        }
        else if (!player.getInventory().add(leftover)) {
            player.drop(leftover, false);
        }
        return interactionResult;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos blockPos) {
        return state.getValue(FROST_LEVEL) == FrostLevel.CHILLED ? 15 : 0;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
        return state.getValue(ATTACHED) ? CCBShapes.COOLER_BLOCK_COOLER_SHAPE : CCBShapes.COOLER_BLOCK_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState blockState, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        return context == CollisionContext.empty() ? CCBShapes.COOLER_BLOCK_SPECIAL_COLLISION_SHAPE : getShape(blockState, level, blockPos, context);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(BreezeCoolerBlock::new);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) != 0) {
            return;
        }

        level.playLocalSound(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, SoundEvents.BREEZE_IDLE_GROUND, SoundSource.BLOCKS, 0.1f, random.nextFloat() * 0.7f + 0.6f, false);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        boolean compressorAttached = context.getLevel().getBlockState(context.getClickedPos().above()).is(CCBBlocks.AIR_COMPRESSOR_BLOCK.get());
        state = state.setValue(FROST_LEVEL, FrostLevel.RIMING).setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(ATTACHED, compressorAttached);
        return ProperWaterloggedBlock.withWater(context.getLevel(), state, context.getClickedPos());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FROST_LEVEL, FACING, WATERLOGGED, ATTACHED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public Class<BreezeCoolerBlockEntity> getBlockEntityClass() {
        return BreezeCoolerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BreezeCoolerBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.BREEZE_COOLER.get();
    }

    public enum FrostLevel implements StringRepresentable {
        RIMING,
        CHILLED;

        public static final Codec<FrostLevel> CODEC = StringRepresentable.fromEnum(FrostLevel::values);

        public boolean isAtLeast(FrostLevel frostLevel) {
            return ordinal() >= frostLevel.ordinal();
        }

        @Override
        public String getSerializedName() {
            return Lang.asId(name());
        }

        @Contract(pure = true)
        public String getTranslatable() {
            return switch (this) {
                case RIMING -> "gui.breeze_cooler.riming";
                case CHILLED -> "gui.breeze_cooler.chilled";
            };
        }

        public ChatFormatting getChatFormatting() {
            return switch (this) {
                case RIMING -> ChatFormatting.GRAY;
                case CHILLED -> ChatFormatting.AQUA;
            };
        }
    }
}
