package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
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
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressBlock extends Block implements IBE<AirtightForgingPressBlockEntity>, IWrenchable {
    public AirtightForgingPressBlock(Properties properties) {
        super(properties);
    }

    private static BlockState getStructureState(AirtightForgingPressStructuralPosition position) {
        if (position.isShaft()) {
            return CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT_BLOCK.getDefaultState().setValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION, position);
        }

        return CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_BLOCK.getDefaultState().setValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION, position);
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
        for (AirtightForgingPressStructuralPosition position : AirtightForgingPressStructuralPosition.all()) {
            BlockPos structurePos = pos.offset(position.getStructureOffset());
            BlockState structureState = getStructureState(position);
            BlockState currentState = level.getBlockState(structurePos);
            if (currentState.canBeReplaced()) {
                level.setBlockAndUpdate(structurePos, structureState);
                continue;
            }

            boolean isExpectedStructure = currentState.getBlock() instanceof IAirtightForgingPressStructural structural && currentState.getValue(structural.getStructuralPosition()) == position;
            if (isExpectedStructure) {
                continue;
            }

            level.destroyBlock(pos, false);
            return;
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (AirtightForgingPressStructuralPosition position : AirtightForgingPressStructuralPosition.all()) {
            BlockState occupiedState = level.getBlockState(pos.offset(position.getStructureOffset()));
            if (!occupiedState.canBeReplaced()) {
                return null;
            }
        }
        return state;
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

    public static class AirtightForgingMachineRenderProperties implements IClientBlockExtensions, MultiPosDestructionHandler {
        @Override
        @Nullable
        public Set<BlockPos> getExtraPositions(ClientLevel level, BlockPos pos, BlockState blockState, int progress) {
            HashSet<BlockPos> positions = new HashSet<>();
            for (AirtightForgingPressStructuralPosition position : AirtightForgingPressStructuralPosition.all()) {
                positions.add(pos.offset(position.getStructureOffset()));
            }
            return positions;
        }
    }
}
