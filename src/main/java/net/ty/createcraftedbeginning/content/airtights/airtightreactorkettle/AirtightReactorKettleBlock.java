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

    private static boolean canPlaceStructure(Level level, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    if (!level.getBlockState(pos.offset(x, y, z)).canBeReplaced()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static BlockState getStructuralState(AirtightReactorKettleStructuralPosition position) {
        if (position.isCog()) {
            return CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG_BLOCK.getDefaultState().setValue(AirtightReactorKettleStructuralCogBlock.STRUCTURAL_POSITION, position);
        }

        return CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_BLOCK.getDefaultState().setValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION, position);
    }

    private static boolean isMatchingStructure(BlockState state, AirtightReactorKettleStructuralPosition position) {
        return state.getBlock() instanceof IAirtightReactorKettleStructural structural && state.getValue(structural.getStructuralPosition()) == position;
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.AIRTIGHT_REACTOR_KETTLE_CENTER_SHAPE;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    BlockPos structurePos = pos.offset(x, y, z);
                    AirtightReactorKettleStructuralPosition structuralPos = AirtightReactorKettleStructuralPosition.fromOffset(x, y, z);
                    BlockState structureState = getStructuralState(structuralPos);
                    BlockState occupiedState = level.getBlockState(structurePos);
                    if (occupiedState.canBeReplaced()) {
                        level.setBlockAndUpdate(structurePos, structureState);
                        continue;
                    }
                    if (isMatchingStructure(occupiedState, structuralPos)) {
                        continue;
                    }

                    level.destroyBlock(pos, false);
                    return;
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
        if (state == null || !canPlaceStructure(context.getLevel(), context.getClickedPos())) {
            return null;
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
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }

                        positions.add(pos.offset(x, y, z));
                    }
                }
            }
            return positions;
        }
    }
}
