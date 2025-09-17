package net.ty.createcraftedbeginning.content.breezes.breezecooler;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
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
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class BreezeCoolerBlock extends HorizontalDirectionalBlock implements IBE<BreezeCoolerBlockEntity>, SimpleWaterloggedBlock, IWrenchable {
    public static final EnumProperty<FrostLevel> FROST_LEVEL = EnumProperty.create("frost_level", FrostLevel.class);
    public static final BooleanProperty COOLER = BooleanProperty.create("cooler");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final MapCodec<BreezeCoolerBlock> CODEC = simpleCodec(BreezeCoolerBlock::new);

    public BreezeCoolerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(FROST_LEVEL, FrostLevel.RIMING).setValue(COOLER, false));
    }

    public static @NotNull FrostLevel getFrostLevelOf(@NotNull BlockState blockState) {
        return blockState.getValue(FROST_LEVEL);
    }

    public static InteractionResultHolder<ItemStack> tryInsert(@NotNull BlockState state, Level world, BlockPos pos, ItemStack stack, boolean doNotConsume, boolean forceOverflow, boolean simulate) {
        if (!state.hasBlockEntity()) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof BreezeCoolerBlockEntity bcbe)) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }

        if (!bcbe.tryUpdateCoolantByItem(stack, forceOverflow, simulate)) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }

        if (!doNotConsume) {
            ItemStack container;
            if (stack.getItem() instanceof DispensibleContainerItem) {
                container = new ItemStack(Items.BUCKET);
            } else {
                container = stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
            }
            if (!world.isClientSide) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(container);
        }
        return InteractionResultHolder.success(ItemStack.EMPTY);
    }

    @Override
    public Class<BreezeCoolerBlockEntity> getBlockEntityClass() {
        return BreezeCoolerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BreezeCoolerBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.BREEZE_COOLER.get();
    }

    @Override
    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
        return false;
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
                if (bcbe.goggles) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                bcbe.goggles = true;
                bcbe.notifyUpdate();
                return ItemInteractionResult.SUCCESS;
            });
        }

        if (stack.isEmpty()) {
            return onBlockEntityUseItemOn(level, pos, bcbe -> {
                if (!bcbe.goggles) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                bcbe.goggles = false;
                bcbe.notifyUpdate();
                return ItemInteractionResult.SUCCESS;
            });
        }

        boolean doNotConsume = player.isCreative();
        boolean forceOverflow = !(player instanceof FakePlayer);

        InteractionResultHolder<ItemStack> resultHolder = tryInsert(state, level, pos, stack, doNotConsume, forceOverflow, false);
        ItemStack leftover = resultHolder.getObject();
        if (!level.isClientSide && !doNotConsume && !leftover.isEmpty()) {
            if (stack.isEmpty()) {
                player.setItemInHand(hand, leftover);
            } else if (!player.getInventory().add(leftover)) {
                player.drop(leftover, false);
            }
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
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos blockPos) {
        return Math.max(0, state.getValue(FROST_LEVEL).ordinal() - 1);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return state.getValue(COOLER) ? CCBShapes.COOLER_BLOCK_COOLER_SHAPE : CCBShapes.COOLER_BLOCK_SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext context) {
        if (context == CollisionContext.empty()) {
            return CCBShapes.COOLER_BLOCK_SPECIAL_COLLISION_SHAPE;
        }
        return getShape(blockState, level, blockPos, context);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (random.nextInt(10) != 0) {
            return;
        }
        world.playLocalSound((float) pos.getX() + 0.5f, (float) pos.getY() + 0.5f, (float) pos.getZ() + 0.5f, SoundEvents.BREEZE_IDLE_GROUND, SoundSource.BLOCKS, 0.1f, random.nextFloat() * 0.7f + 0.6f, false);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        state = state.setValue(FROST_LEVEL, FrostLevel.RIMING).setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(COOLER, false);

        return ProperWaterloggedBlock.withWater(context.getLevel(), state, context.getClickedPos());
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        AdvancementBehaviour.setPlacedBy(world, pos, placer);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FROST_LEVEL, FACING, WATERLOGGED, COOLER);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public enum FrostLevel implements StringRepresentable {
        RIMING,
        WANING,
        CHILLED;

        public static final Codec<FrostLevel> CODEC = StringRepresentable.fromEnum(FrostLevel::values);

        public boolean isAtLeast(@NotNull FrostLevel frostLevel) {
            return this.ordinal() >= frostLevel.ordinal();
        }

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }

        @Contract(pure = true)
        public @NotNull String getTranslatable() {
            return switch (this) {
                case RIMING -> "gui.goggles.breeze_cooler.riming";
                case WANING, CHILLED -> "gui.goggles.breeze_cooler.chilled";
            };
        }

        public ChatFormatting getChatFormatting() {
            return switch (this) {
                case RIMING -> ChatFormatting.GRAY;
                case WANING, CHILLED -> ChatFormatting.AQUA;
            };
        }
    }
}
