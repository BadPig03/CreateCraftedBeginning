package net.ty.createcraftedbeginning.content.airtightintakeport;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import org.jetbrains.annotations.NotNull;

public class AirtightIntakePortBlock extends Block implements IBE<AirtightIntakePortBlockEntity>, SimpleWaterloggedBlock, IWrenchable {
    public static final SoundType SILENCED_METAL = new DeferredSoundType(0.1F, 1.5F, () -> SoundEvents.METAL_BREAK, () -> SoundEvents.METAL_STEP, () -> SoundEvents.METAL_PLACE, () -> SoundEvents.METAL_HIT, () -> SoundEvents.METAL_FALL);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirtightIntakePortBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState toPlace = super.getStateForPlacement(context);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player == null) {
            return toPlace;
        }

        toPlace = ProperWaterloggedBlock.withWater(level, toPlace, pos);

        Direction nearestLookingDirection = context.getNearestLookingDirection();
        Direction facing = nearestLookingDirection.getOpposite();

        if (toPlace != null) {
            return toPlace.setValue(FACING, facing);
        }

        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext collisionContext) {
        return CCBShapes.AIRTIGHT_INTAKE_PORT.get(state.getValue(FACING));
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        AdvancementBehaviour.setPlacedBy(world, pos, placer);
    }

    @Override
    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public @NotNull SoundType getSoundType(@NotNull BlockState state, @NotNull LevelReader world, @NotNull BlockPos pos, Entity entity) {
        SoundType soundType = super.getSoundType(state, world, pos, entity);
        if (entity != null && entity.getPersistentData().contains("SilenceTankSound")) {
            return SILENCED_METAL;
        }
        return soundType;
    }

    @Override
    public Class<AirtightIntakePortBlockEntity> getBlockEntityClass() {
        return AirtightIntakePortBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightIntakePortBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_INTAKE_PORT.get();
    }
}
