package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock.TeslaTurbineStructuralPosition;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineBlock extends RotatedPillarKineticBlock implements IBE<TeslaTurbineBlockEntity>, SimpleWaterloggedBlock, SpecialBlockItemRequirement {
    public static final IntegerProperty ROTOR = IntegerProperty.create("rotor", 0, TeslaTurbineUtils.MAX_ROTORS);
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public TeslaTurbineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(ROTOR, 0));
    }

    public @Nullable Axis getAxisForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        return state.getValue(AXIS);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.getBlockTicks().hasScheduledTick(pos, this)) {
            return;
        }

        level.scheduleTick(pos, this, 1);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }

        super.onRemove(state, level, pos, newState, isMoving);
        int rotorCount = state.getValue(ROTOR);
        if (rotorCount == 0) {
            return;
        }

        ItemStack rotors = new ItemStack(CCBItems.TESLA_TURBINE_ROTOR.asItem(), rotorCount);
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, rotors);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public Class<TeslaTurbineBlockEntity> getBlockEntityClass() {
        return TeslaTurbineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TeslaTurbineBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.TESLA_TURBINE.get();
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack heldItem = player.getItemInHand(hand);
        int rotorCount = state.getValue(ROTOR);
        if (heldItem.is(CCBItems.TESLA_TURBINE_ROTOR)) {
            if (rotorCount >= TeslaTurbineUtils.MAX_ROTORS) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            level.setBlockAndUpdate(pos, state.setValue(ROTOR, rotorCount + 1));
            CCBSoundEvents.ROTOR_ADDED.playOnServer(level, pos, 1, 1);
            if (!player.isCreative()) {
                heldItem.shrink(1);
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (!heldItem.is(AllItems.WRENCH) || rotorCount == 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        level.setBlockAndUpdate(pos, state.setValue(ROTOR, rotorCount - 1));
        CCBSoundEvents.ROTOR_REMOVED.playOnServer(level, pos, 1, 1);
        if (player.isCreative()) {
            return ItemInteractionResult.SUCCESS;
        }

        ItemStack rotor = new ItemStack(CCBItems.TESLA_TURBINE_ROTOR.asItem());
        ItemHandlerHelper.giveItemToPlayer(player, rotor);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.TESLA_TURBINE.get(state.getValue(BlockStateProperties.AXIS));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Axis axis = state.getValue(AXIS);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }

                BlockPos partPos = TeslaTurbineUtils.calculateStructurePos(pos, axis, i, j);
                TeslaTurbineStructuralPosition structuralPosition = TeslaTurbineStructuralPosition.fromOffset(i, j);
                BlockState partState = CCBBlocks.TESLA_TURBINE_STRUCTURAL_BLOCK.getDefaultState().setValue(TeslaTurbineStructuralBlock.AXIS, axis).setValue(TeslaTurbineStructuralBlock.STRUCTURAL_POSITION, structuralPosition);
                partState = ProperWaterloggedBlock.withWater(level, partState, partPos);

                BlockState existingState = level.getBlockState(partPos);
                if (!existingState.canBeReplaced()) {
                    boolean matchesStructure = existingState.getBlock() instanceof TeslaTurbineStructuralBlock && existingState.getValue(TeslaTurbineStructuralBlock.AXIS) == axis && existingState.getValue(TeslaTurbineStructuralBlock.STRUCTURAL_POSITION) == structuralPosition;
                    if (!matchesStructure) {
                        level.destroyBlock(pos, false);
                        return;
                    }

                    continue;
                }

                level.setBlockAndUpdate(partPos, partState);
            }
        }

        if (!(level.getBlockEntity(pos) instanceof TeslaTurbineBlockEntity turbine)) {
            return;
        }

        turbine.refreshStructure();
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED).add(ROTOR);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        Level level = context.getLevel();
        Axis axis = state.getValue(AXIS);
        BlockPos pos = context.getClickedPos();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }

                BlockPos partPos = TeslaTurbineUtils.calculateStructurePos(pos, axis, i, j);
                BlockState existingState = level.getBlockState(partPos);
                if (!existingState.canBeReplaced()) {
                    return null;
                }
            }
        }
        return ProperWaterloggedBlock.withWater(level, state, pos);
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
        List<StackRequirement> requirements = new ArrayList<>();
        requirements.add(new StackRequirement(new ItemStack(asItem()), ItemUseType.CONSUME));
        int rotorCount = state.getValue(ROTOR);
        if (rotorCount <= 0) {
            return new ItemRequirement(requirements);
        }

        ItemStack rotors = new ItemStack(CCBItems.TESLA_TURBINE_ROTOR.asItem(), rotorCount);
        requirements.add(new StackRequirement(rotors, ItemUseType.CONSUME));
        return new ItemRequirement(requirements);
    }
}
