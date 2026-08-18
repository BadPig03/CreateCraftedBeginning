package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPipeBlock extends AxisGasPipeBlock implements IBE<AirtightPipeBlockEntity>, IAirtightComponent {
    public static final BooleanProperty CASED = BooleanProperty.create("cased");

    public AirtightPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CASED, false));
    }

    @Override
    public Class<AirtightPipeBlockEntity> getBlockEntityClass() {
        return AirtightPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightPipeBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_PIPE.get();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!state.getValue(CASED)) {
            return super.onWrenched(state, context);
        }

        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockPos pos = context.getClickedPos();
        level.setBlockAndUpdate(pos, state.setValue(CASED, false));
        CCBSoundEvents.SHEET_REMOVED.playOnServer(level, pos, 1, 1);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(CASED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(CCBItems.AIRTIGHT_SHEET) || state.getValue(CASED)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        BlockState casedState = state.setValue(CASED, true).setValue(WATERLOGGED, false);
        level.setBlockAndUpdate(pos, casedState);
        CCBSoundEvents.SHEET_ADDED.playOnServer(level, pos, 1, 1);
        return ItemInteractionResult.sidedSuccess(false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(CASED)) {
            return Shapes.block();
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return !state.getValue(CASED) && super.canPlaceLiquid(player, level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return !state.getValue(CASED) && super.placeLiquid(level, pos, state, fluidState);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        boolean encasedPipeItem = context.getItemInHand().is(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.asItem());
        if (state.getValue(CASED) && encasedPipeItem) {
            return false;
        }

        Player player = context.getPlayer();
        boolean canReplace = super.canBeReplaced(state, context);
        if (player != null && player.isShiftKeyDown()) {
            return canReplace;
        }
        return encasedPipeItem || canReplace;
    }

    @Override
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return currentState.getValue(AXIS) == localFace.getAxis();
    }
}
