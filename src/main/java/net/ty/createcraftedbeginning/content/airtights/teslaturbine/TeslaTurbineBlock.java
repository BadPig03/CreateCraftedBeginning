package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
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
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock.TeslaTurbineStructuralPosition;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineBlock extends RotatedPillarKineticBlock implements IBE<TeslaTurbineBlockEntity>, SimpleWaterloggedBlock, SpecialBlockItemRequirement {
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final int ROTOR_MAX_COUNT = 8;

    public static final IntegerProperty ROTOR = IntegerProperty.create("rotor", 0, ROTOR_MAX_COUNT);

    public TeslaTurbineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(ROTOR, 0));
    }

    @Contract("_, _, _, _ -> new")
    public static BlockPos calculateStructurePos(BlockPos pos, Axis axis, int u, int v) {
        switch (axis) {
            case X -> {
                return new BlockPos(pos.getX(), pos.getY() + v, pos.getZ() + u);
            }
            case Z -> {
                return new BlockPos(pos.getX() + u, pos.getY() + v, pos.getZ());
            }
            default -> {
                return new BlockPos(pos.getX() + u, pos.getY(), pos.getZ() + v);
            }
        }
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

        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(CCBItems.TESLA_TURBINE_ROTOR.asItem(), rotorCount));
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack itemStack = player.getItemInHand(hand);
        int rotorCount = state.getValue(ROTOR);
        if (itemStack.is(CCBItems.TESLA_TURBINE_ROTOR)) {
            if (rotorCount >= ROTOR_MAX_COUNT) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            level.setBlockAndUpdate(pos, state.setValue(ROTOR, rotorCount + 1));
            CCBSoundEvents.ROTOR_ADDED.playOnServer(level, pos, 1.0f, 1.0f);
            if (!player.isCreative()) {
                itemStack.shrink(1);
            }
            return ItemInteractionResult.SUCCESS;
        }
        else if (itemStack.is(AllItems.WRENCH)) {
            if (rotorCount == 0) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            level.setBlockAndUpdate(pos, state.setValue(ROTOR, rotorCount - 1));
            CCBSoundEvents.ROTOR_REMOVED.playOnServer(level, pos, 1.0f, 1.0f);
            if (!player.isCreative()) {
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(CCBItems.TESLA_TURBINE_ROTOR.asItem()));
            }
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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

                BlockPos structurePos = calculateStructurePos(pos, axis, i, j);
                TeslaTurbineStructuralPosition structuralPos = TeslaTurbineStructuralPosition.fromOffset(i, j);
                BlockState structureState = ProperWaterloggedBlock.withWater(level, CCBBlocks.TESLA_TURBINE_STRUCTURAL_BLOCK.getDefaultState().setValue(TeslaTurbineStructuralBlock.AXIS, axis).setValue(TeslaTurbineStructuralBlock.STRUCTURAL_POSITION, structuralPos), structurePos);
                BlockState occupiedState = level.getBlockState(structurePos);
                if (!occupiedState.canBeReplaced()) {
                    if (!(occupiedState.getBlock() instanceof TeslaTurbineStructuralBlock) || occupiedState.getValue(TeslaTurbineStructuralBlock.AXIS) != axis || occupiedState.getValue(TeslaTurbineStructuralBlock.STRUCTURAL_POSITION) != structuralPos) {
                        level.destroyBlock(pos, false);
                        return;
                    }
                    continue;
                }

                level.setBlockAndUpdate(structurePos, structureState);
            }
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED).add(ROTOR);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
        List<StackRequirement> requirements = new ArrayList<>();
        requirements.add(new StackRequirement(new ItemStack(asItem()), ItemUseType.CONSUME));
        int rotorCount = state.getValue(ROTOR);
        if (rotorCount > 0) {
            requirements.add(new StackRequirement(new ItemStack(CCBItems.TESLA_TURBINE_ROTOR.asItem(), rotorCount), ItemUseType.CONSUME));
        }
        return new ItemRequirement(requirements);
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

                BlockState occupiedState = level.getBlockState(calculateStructurePos(pos, axis, i, j));
                if (!occupiedState.canBeReplaced()) {
                    return null;
                }
            }
        }
        return ProperWaterloggedBlock.withWater(level, state, pos);
    }

    public static class TeslaTurbineRenderProperties implements IClientBlockExtensions, MultiPosDestructionHandler {
        @Override
        @Nullable
        public Set<BlockPos> getExtraPositions(ClientLevel level, BlockPos pos, BlockState blockState, int progress) {
            Axis axis = blockState.getValue(AXIS);
            HashSet<BlockPos> positions = new HashSet<>();
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0) {
                        continue;
                    }

                    positions.add(calculateStructurePos(pos, axis, i, j));
                }
            }
            return positions;
        }
    }
}
