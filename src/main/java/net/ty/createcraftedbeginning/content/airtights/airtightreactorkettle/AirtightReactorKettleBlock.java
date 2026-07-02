package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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
public class AirtightReactorKettleBlock extends Block implements IBE<AirtightReactorKettleBlockEntity>, IWrenchable {
    public AirtightReactorKettleBlock(Properties properties) {
        super(properties);
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
    protected VoxelShape getShape(BlockState blockState, BlockGetter level, BlockPos blockPos, CollisionContext context) {
        return CCBShapes.AIRTIGHT_REACTOR_KETTLE_CENTER_SHAPE;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    if (i == 0 && j == 0 && k == 0) {
                        continue;
                    }

                    BlockPos structurePos = pos.offset(i, j, k);
                    AirtightReactorKettleStructuralPosition structuralPos = AirtightReactorKettleStructuralPosition.fromOffset(i, j, k);
                    BlockState structureState;
                    if (structuralPos.isCog()) {
                        structureState = CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG_BLOCK.getDefaultState().setValue(AirtightReactorKettleStructuralCogBlock.STRUCTURAL_POSITION, structuralPos);
                    }
                    else {
                        structureState = CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_BLOCK.getDefaultState().setValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION, structuralPos);
                    }
                    BlockState occupiedState = level.getBlockState(structurePos);
                    if (!occupiedState.canBeReplaced()) {
                        if (!(occupiedState.getBlock() instanceof IAirtightReactorKettleStructural structural) || occupiedState.getValue(structural.getStructuralPosition()) != structuralPos) {
                            level.destroyBlock(pos, false);
                            return;
                        }
                        continue;
                    }

                    level.setBlockAndUpdate(structurePos, structureState);
                }
            }
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
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    if (i == 0 && j == 0 && k == 0) {
                        continue;
                    }

                    BlockState occupiedState = level.getBlockState(new BlockPos(pos.getX() + i, pos.getY() + j, pos.getZ() + k));
                    if (!occupiedState.canBeReplaced()) {
                        return null;
                    }
                }
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
    public Class<AirtightReactorKettleBlockEntity> getBlockEntityClass() {
        return AirtightReactorKettleBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightReactorKettleBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get();
    }

    public static class AirtightReactorKettleRenderProperties implements IClientBlockExtensions, MultiPosDestructionHandler {
        @Override
        @Nullable
        public Set<BlockPos> getExtraPositions(ClientLevel level, BlockPos pos, BlockState blockState, int progress) {
            HashSet<BlockPos> positions = new HashSet<>();
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    for (int k = -1; k <= 1; k++) {
                        if (i == 0 && j == 0 && k == 0) {
                            continue;
                        }

                        positions.add(new BlockPos(pos.getX() + i, pos.getY() + j, pos.getZ() + k));
                    }
                }
            }
            return positions;
        }
    }
}
