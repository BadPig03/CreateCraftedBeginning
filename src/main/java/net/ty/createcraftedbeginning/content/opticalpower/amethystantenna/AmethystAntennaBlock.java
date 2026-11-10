package net.ty.createcraftedbeginning.content.opticalpower.amethystantenna;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystAntennaBlock extends DirectionalBlock implements IBE<AmethystAntennaBlockEntity>, IWrenchable {
    public AmethystAntennaBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    public static void registerMovementChecks(AmethystAntennaBlock block) {
        BlockMovementChecks.registerBrittleCheck(state -> state.is(block) ? CheckResult.SUCCESS : CheckResult.PASS);
        BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) -> {
            if (!state.is(block)) {
                return CheckResult.PASS;
            }

            if (direction != state.getValue(FACING).getOpposite()) {
                return CheckResult.FAIL;
            }

            return CheckResult.SUCCESS;
        });
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(AmethystAntennaBlock::new);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState placement = defaultBlockState().setValue(FACING, facing);
        BlockItemStateProperties itemState = context.getItemInHand().getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        if (itemState.apply(placement).getValue(FACING) != facing || !AmethystAntennaConnection.canAttach(context)) {
            return null;
        }

        return placement;
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return AmethystAntennaConnection.canSurvive(state, level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }

        return state;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.AMETHYST_ANTENNA.get(state.getValue(FACING));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        LoadingNetworkBinding binding = stack.get(CCBDataComponents.LOADING_NETWORK);
        UUID owner = placer instanceof Player player ? player.getUUID() : null;
        withBlockEntityDo(serverLevel, pos, antenna -> antenna.place(serverLevel, owner, binding));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moved) {
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.restoringBlockSnapshots || previous.is(this)) {
            return;
        }

        if (!(serverLevel.getChunkAt(pos).getBlockEntity(pos) instanceof AmethystAntennaBlockEntity antenna)) {
            return;
        }

        antenna.commitPlacement(serverLevel);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock()) && level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof AmethystAntennaBlockEntity antenna) {
            AmethystAntennaConnection.remove(serverLevel, pos, state, antenna.identity());
        }
        IBE.onRemove(state, level, pos, replacement);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<AmethystAntennaBlockEntity> getBlockEntityClass() {
        return AmethystAntennaBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AmethystAntennaBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AMETHYST_ANTENNA.get();
    }
}
