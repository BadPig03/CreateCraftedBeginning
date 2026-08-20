package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressBlock extends Block implements IBE<AirtightForgingPressBlockEntity>, IWrenchable {
    public AirtightForgingPressBlock(Properties properties) {
        super(properties);
    }

    private static BlockState getStructureState(AirtightForgingPressStructuralPosition structuralPosition) {
        if (structuralPosition.isShaft()) {
            return CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT_BLOCK.getDefaultState().setValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION, structuralPosition);
        }
        return CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_BLOCK.getDefaultState().setValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION, structuralPosition);
    }

    private static boolean isExpectedStructure(BlockState state, AirtightForgingPressStructuralPosition structuralPosition) {
        return state.getBlock() instanceof IAirtightForgingPressStructural structural && state.getValue(structural.getStructuralPosition()) == structuralPosition;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return true;
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
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return onBlockEntityUseItemOn(level, blockPos, press -> AirtightForgingPressUtils.getUseItemOnResult(press, level, player, blockPos, hand, stack));
    }

    @Override
    protected VoxelShape getShape(BlockState blockState, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        return CCBShapes.AIRTIGHT_FORGING_PRESS_CENTER_SHAPE;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (AirtightForgingPressStructuralPosition structuralPosition : AirtightForgingPressStructuralPosition.all()) {
            BlockState structureState = level.getBlockState(pos.offset(structuralPosition.getStructureOffset()));
            if (!structureState.canBeReplaced() && !isExpectedStructure(structureState, structuralPosition)) {
                abortStructureFormation(level, pos);
                return;
            }
        }

        for (AirtightForgingPressStructuralPosition structuralPosition : AirtightForgingPressStructuralPosition.all()) {
            BlockPos structurePos = pos.offset(structuralPosition.getStructureOffset());
            BlockState structureState = level.getBlockState(structurePos);
            if (isExpectedStructure(structureState, structuralPosition)) {
                continue;
            }

            if (structureState.canBeReplaced() && level.setBlockAndUpdate(structurePos, getStructureState(structuralPosition)) && isExpectedStructure(level.getBlockState(structurePos), structuralPosition)) {
                continue;
            }

            abortStructureFormation(level, pos);
            return;
        }
    }

    private void abortStructureFormation(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(this)) {
            level.destroyBlock(pos, true);
        }

        for (AirtightForgingPressStructuralPosition structuralPosition : AirtightForgingPressStructuralPosition.all()) {
            BlockPos structurePos = pos.offset(structuralPosition.getStructureOffset());
            if (!isExpectedStructure(level.getBlockState(structurePos), structuralPosition)) {
                continue;
            }

            level.destroyBlock(structurePos, false);
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placementState = super.getStateForPlacement(context);
        if (placementState == null) {
            return null;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (AirtightForgingPressStructuralPosition structuralPosition : AirtightForgingPressStructuralPosition.all()) {
            BlockState occupiedState = level.getBlockState(pos.offset(structuralPosition.getStructureOffset()));
            if (occupiedState.canBeReplaced()) {
                continue;
            }

            return null;
        }
        return placementState;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    public Class<AirtightForgingPressBlockEntity> getBlockEntityClass() {
        return AirtightForgingPressBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightForgingPressBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_FORGING_PRESS.get();
    }
}
