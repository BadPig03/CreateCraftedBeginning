package net.ty.createcraftedbeginning.content.airtights.gascanister;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterItem.GasCanisterBlockItem;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GasCanisterBlock extends Block implements IBE<GasCanisterBlockEntity>, SimpleWaterloggedBlock, IWrenchable, SpecialBlockItemRequirement {
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public GasCanisterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    public @NotNull MapCodec<GasCanisterBlock> codec() {
        return simpleCodec(GasCanisterBlock::new);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity entity, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
        if (level.isClientSide) {
            return;
        }

        withBlockEntityDo(level, pos, be -> be.setCanisterContent(stack));
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        if (!(asItem() instanceof GasCanisterBlockItem) || !(level.getBlockEntity(pos) instanceof GasCanisterBlockEntity canisterBlockEntity)) {
            return ItemStack.EMPTY;
        }

        return canisterBlockEntity.getCanister().copy();
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : ProperWaterloggedBlock.withWater(context.getLevel(), state, context.getClickedPos());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
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
    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, @NotNull Builder builder) {
        List<ItemStack> lootDrops = super.getDrops(state, builder);
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof GasCanisterBlockEntity canister)) {
            return lootDrops;
        }

        return List.of(canister.getCanister().copy());
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext collisionContext) {
        return CCBShapes.GAS_CANISTER_SHAPE;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
        Item item = asItem();
        if (item instanceof GasCanisterBlockItem placeable) {
            item = placeable.getActualItem();
        }
        return new ItemRequirement(ItemUseType.CONSUME, item);
    }

    @Override
    public Class<GasCanisterBlockEntity> getBlockEntityClass() {
        return GasCanisterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GasCanisterBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.GAS_CANISTER.get();
    }
}
