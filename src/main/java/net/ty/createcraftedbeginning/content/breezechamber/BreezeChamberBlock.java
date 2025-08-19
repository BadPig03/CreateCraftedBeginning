package net.ty.createcraftedbeginning.content.breezechamber;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.data.CoolantDataManager;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import org.jetbrains.annotations.NotNull;

public class BreezeChamberBlock extends HorizontalDirectionalBlock implements IBE<BreezeChamberBlockEntity>, SimpleWaterloggedBlock, IWrenchable {
    public static final EnumProperty<FrostLevel> FROST_LEVEL = EnumProperty.create("breeze", FrostLevel.class);
    public static final BooleanProperty COOLER = BooleanProperty.create("cooler");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final MapCodec<BreezeChamberBlock> CODEC = simpleCodec(BreezeChamberBlock::new);

    public BreezeChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(FROST_LEVEL, FrostLevel.RIMING).setValue(COOLER, false));
    }

    public static FrostLevel getFrostLevelOf(BlockState blockState) {
        return blockState.getValue(BreezeChamberBlock.FROST_LEVEL);
    }

    public static LootTable.Builder buildLootTable() {
        LootItemCondition.Builder survivesExplosion = ExplosionCondition.survivesExplosion();
        BreezeChamberBlock block = CCBBlocks.BREEZE_CHAMBER_BLOCK.get();
        LootTable.Builder builder = LootTable.lootTable();
        LootPool.Builder poolBuilder = LootPool.lootPool();
        for (FrostLevel level : FrostLevel.values()) {
            poolBuilder.add(LootItem.lootTableItem(block).when(survivesExplosion).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(FROST_LEVEL, level))));
        }
        builder.withPool(poolBuilder.setRolls(ConstantValue.exactly(1)));
        return builder;
    }

    public static InteractionResultHolder<ItemStack> tryInsert(BlockState state, Level world, BlockPos pos, ItemStack stack, boolean doNotConsume, boolean forceOverflow, boolean simulate) {
        if (!state.hasBlockEntity()) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof BreezeChamberBlockEntity bcbe)) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }

        if (!bcbe.tryUpdateCoolantByItem(stack, forceOverflow, simulate)) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }

        if (!doNotConsume) {
            ItemStack container;
            CoolantDataManager.CoolantData data = CoolantDataManager.getItemCoolantData(stack);
            if (data != null && data.getRemainingId() != null) {
                Item containerItem = BuiltInRegistries.ITEM.get(data.getRemainingId());
                container = new ItemStack(containerItem);
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
    public Class<BreezeChamberBlockEntity> getBlockEntityClass() {
        return BreezeChamberBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BreezeChamberBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.BREEZE_CHAMBER.get();
    }

    @Override
    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
        return false;
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

        InteractionResultHolder<ItemStack> res = tryInsert(state, level, pos, stack, doNotConsume, forceOverflow, false);
        ItemStack leftover = res.getObject();
        if (!level.isClientSide && !doNotConsume && !leftover.isEmpty()) {
            if (stack.isEmpty()) {
                player.setItemInHand(hand, leftover);
            } else if (!player.getInventory().add(leftover)) {
                player.drop(leftover, false);
            }
        }

        return res.getResult() == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, @NotNull Level level, @NotNull BlockPos blockPos) {
        return Math.max(0, state.getValue(FROST_LEVEL).ordinal() - 1);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return state.getValue(COOLER) ? CCBShapes.CHAMBER_BLOCK_COOLER_SHAPE : CCBShapes.CHAMBER_BLOCK_SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState blockState, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext context) {
        if (context == CollisionContext.empty()) {
            return CCBShapes.CHAMBER_BLOCK_SPECIAL_COLLISION_SHAPE;
        }
        return getShape(blockState, level, blockPos, context);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, RandomSource random) {
        if (random.nextInt(10) != 0) {
            return;
        }
        if (!state.getValue(FROST_LEVEL).isAtLeast(FrostLevel.RIMING)) {
            return;
        }
        world.playLocalSound((float) pos.getX() + 0.5F, (float) pos.getY() + 0.5F, (float) pos.getZ() + 0.5F, SoundEvents.BREEZE_IDLE_GROUND, SoundSource.BLOCKS, 0.1F, random.nextFloat() * 0.7F + 0.6F, false);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        Item item = stack.getItem();
        BlockState defaultState = super.getStateForPlacement(context);

        if (!(item instanceof BreezeChamberBlockItem) || defaultState == null) {
            return defaultState;
        }

        FrostLevel initialFrost = FrostLevel.RIMING;
        defaultState = defaultState.setValue(FROST_LEVEL, initialFrost).setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(COOLER, false);

        return ProperWaterloggedBlock.withWater(context.getLevel(), defaultState, context.getClickedPos());
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        AdvancementBehaviour.setPlacedBy(world, pos, placer);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FROST_LEVEL, FACING, WATERLOGGED, COOLER);
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public enum FrostLevel implements StringRepresentable {
        RIMING,
        WANING,
        CHILLED,
        GALLING;

        public static final Codec<FrostLevel> CODEC = StringRepresentable.fromEnum(FrostLevel::values);

        public boolean isAtLeast(FrostLevel frostLevel) {
            return this.ordinal() >= frostLevel.ordinal();
        }

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }

        public String getTranslatable() {
            return switch (this) {
                case RIMING -> "gui.goggles.breeze_chamber.riming";
                case WANING, CHILLED -> "gui.goggles.breeze_chamber.chilled";
                case GALLING -> "gui.goggles.breeze_chamber.galling";
            };
        }

        public ChatFormatting getChatFormatting() {
            return switch (this) {
                case RIMING -> ChatFormatting.GRAY;
                case WANING, CHILLED -> ChatFormatting.DARK_AQUA;
                case GALLING -> ChatFormatting.AQUA;
            };
        }
    }
}
