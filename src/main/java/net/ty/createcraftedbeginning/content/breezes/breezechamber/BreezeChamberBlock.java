package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.IChamberGasTank;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class BreezeChamberBlock extends HorizontalDirectionalBlock implements IBE<BreezeChamberBlockEntity>, SimpleWaterloggedBlock, IWrenchable, IAirtightComponent {
    public static final EnumProperty<WindLevel> WIND_LEVEL = EnumProperty.create("wind_level", WindLevel.class);

    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public BreezeChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(WIND_LEVEL, WindLevel.CALM));
    }

    public static @NotNull WindLevel getWindLevelOf(@NotNull BlockState blockState) {
        return blockState.getValue(WIND_LEVEL);
    }

    public static InteractionResultHolder<ItemStack> tryInsert(@NotNull Level level, BlockPos pos, ItemStack stack, boolean doNotConsume, boolean forceOverflow, boolean simulate) {
        if (!(level.getBlockEntity(pos) instanceof BreezeChamberBlockEntity bcbe) || !bcbe.tryUpdateChargerByItem(stack, forceOverflow, simulate)) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }

        if (!doNotConsume) {
            ItemStack container = ItemStack.EMPTY;
            FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, null);
            if (foodProperties != null) {
                container = foodProperties.usingConvertsTo().orElse(ItemStack.EMPTY);
            }
            if (container.isEmpty()) {
                container = stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
            }
            if (!level.isClientSide) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(container);
        }

        return InteractionResultHolder.success(ItemStack.EMPTY);
    }

    @Override
    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return level.getBlockEntity(pos.below()) instanceof IChamberGasTank;
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block otherBlock, @NotNull BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, otherBlock, neighborPos, isMoving);
        if (canSurvive(state, level, pos)) {
            return;
        }

        level.destroyBlock(pos, true);
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbourState, @NotNull LevelAccessor world, @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        return state;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (AllItems.GOGGLES.isIn(stack)) {
            return onBlockEntityUseItemOn(level, pos, bcbe -> {
                if (bcbe.hasGoggles()) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                bcbe.setGoggles(true);
                bcbe.notifyUpdate();
                return ItemInteractionResult.SUCCESS;
            });
        }

        if (stack.isEmpty()) {
            return onBlockEntityUseItemOn(level, pos, bcbe -> {
                if (!bcbe.hasGoggles()) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                bcbe.setGoggles(false);
                bcbe.notifyUpdate();
                return ItemInteractionResult.SUCCESS;
            });
        }

        boolean doNotConsume = player.isCreative();
        boolean forceOverflow = !(player instanceof FakePlayer);
        InteractionResultHolder<ItemStack> resultHolder = tryInsert(level, pos, stack, doNotConsume, forceOverflow, false);
        ItemStack leftover = resultHolder.getObject();
        if (level.isClientSide || doNotConsume || leftover.isEmpty()) {
            return resultHolder.getResult() == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.isEmpty()) {
            player.setItemInHand(hand, leftover);
        }
        else if (!player.getInventory().add(leftover)) {
            player.drop(leftover, false);
        }
        return resultHolder.getResult() == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState blockState) {
        return true;
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder params) {
        if (!(params.getParameter(LootContextParams.BLOCK_ENTITY) instanceof BreezeChamberBlockEntity chamber) || chamber.getWindRemainingTime() == 0) {
            return super.getDrops(state, params);
        }

        ItemStack chamberItemEntity = new ItemStack(this);
        chamber.saveToItem(chamberItemEntity);
        return Collections.singletonList(chamberItemEntity);
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos blockPos) {
        return Math.max(0, state.getValue(WIND_LEVEL).ordinal() - 1);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return CCBShapes.CHAMBER_BLOCK_SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext context) {
        return context == CollisionContext.empty() ? CCBShapes.CHAMBER_BLOCK_SPECIAL_COLLISION_SHAPE : getShape(blockState, level, blockPos, context);
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(BreezeChamberBlock::new);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (random.nextInt(10) != 0) {
            return;
        }

        level.playLocalSound(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, SoundEvents.BREEZE_IDLE_GROUND, SoundSource.BLOCKS, 0.1f, random.nextFloat() * 0.7f + 0.6f, false);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        state = state.setValue(WIND_LEVEL, WindLevel.CALM).setValue(FACING, context.getHorizontalDirection().getOpposite());
        return ProperWaterloggedBlock.withWater(context.getLevel(), state, context.getClickedPos());
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity entity, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
        if (!(level.getBlockEntity(pos) instanceof BreezeChamberBlockEntity chamber)) {
            return;
        }

        chamber.loadFromItem(stack);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof BreezeChamberBlockEntity chamber && chamber.getWindRemainingTime() != 0) {
            ItemStack chamberItemEntity = new ItemStack(this);
            chamber.saveToItem(chamberItemEntity);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, chamberItemEntity);
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    @Override
    protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, WIND_LEVEL);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack chamber, @NotNull TooltipContext context, @NotNull List<Component> tooltips, @NotNull TooltipFlag flag) {
        int time = chamber.getOrDefault(CCBDataComponents.BREEZE_TIME, 0);
        WindLevel windLevel = WindLevel.CALM;
        if (time > 0) {
            windLevel = WindLevel.GALE;
        }
        else if (time < 0) {
            windLevel = WindLevel.ILL;
        }
        tooltips.add(CCBLang.translate("gui.tooltips.breeze_chamber.state").style(ChatFormatting.GRAY).add(CCBLang.translate(windLevel.getTranslatable()).style(windLevel.getChatFormatting())).component());
        if (time == 0) {
            return;
        }

        tooltips.add(CCBLang.translate("gui.tooltips.breeze_chamber.time").style(ChatFormatting.GRAY).add(CCBLang.seconds(Mth.abs(time), context.tickRate()).style(ChatFormatting.GOLD)).component());
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        ItemStack chamberItemEntity = new ItemStack(this);
        if (!(level.getBlockEntity(pos) instanceof BreezeChamberBlockEntity chamber) || chamber.getWindRemainingTime() == 0 || !player.isShiftKeyDown()) {
            return chamberItemEntity;
        }

        chamber.saveToItem(chamberItemEntity);
        return chamberItemEntity;
    }

    @Override
    public Class<BreezeChamberBlockEntity> getBlockEntityClass() {
        return BreezeChamberBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BreezeChamberBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.BREEZE_CHAMBER.get();
    }

    @Override
    public boolean isAirtight(BlockPos currentPos, BlockState currentState, Direction oppositeDirection) {
        return true;
    }

    public enum WindLevel implements StringRepresentable {
        ILL,
        CALM,
        GALE;

        public static final Codec<WindLevel> CODEC = StringRepresentable.fromEnum(WindLevel::values);

        public boolean isAtLeast(@NotNull WindLevel windLevel) {
            return ordinal() >= windLevel.ordinal();
        }

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }

        @Contract(pure = true)
        public @NotNull String getTranslatable() {
            return switch (this) {
                case ILL -> "gui.goggles.breeze_chamber.ill";
                case CALM -> "gui.goggles.breeze_chamber.calm";
                case GALE -> "gui.goggles.breeze_chamber.gale";
            };
        }

        public ChatFormatting getChatFormatting() {
            return switch (this) {
                case ILL -> ChatFormatting.RED;
                case CALM -> ChatFormatting.GRAY;
                case GALE -> ChatFormatting.AQUA;
            };
        }
    }
}
