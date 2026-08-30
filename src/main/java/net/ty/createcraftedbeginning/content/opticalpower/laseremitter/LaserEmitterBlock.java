package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerConsumer;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkManager;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LaserEmitterBlock extends DirectionalBlock implements IBE<LaserEmitterBlockEntity>, IWrenchable, OpticalPowerConsumer {
    public LaserEmitterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public boolean canConnectOpticalPower(BlockState state, Direction side) {
        return side == state.getValue(FACING).getOpposite();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection();
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            facing = facing.getOpposite();
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || oldState.is(state.getBlock()) && oldState.getValue(FACING) == state.getValue(FACING)) {
            return;
        }

        OpticalPowerNetworkManager.invalidateAround(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            OpticalPowerNetworkManager.invalidateAround(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public Class<LaserEmitterBlockEntity> getBlockEntityClass() {
        return LaserEmitterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LaserEmitterBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.LASER_EMITTER.get();
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(LaserEmitterBlock::new);
    }
}
