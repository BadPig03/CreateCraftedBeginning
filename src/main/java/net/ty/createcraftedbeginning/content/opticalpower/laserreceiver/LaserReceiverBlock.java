package net.ty.createcraftedbeginning.content.opticalpower.laserreceiver;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LaserReceiverBlock extends DirectionalKineticBlock implements IBE<LaserReceiverBlockEntity>, IWrenchable {
    public LaserReceiverBlock(Properties properties) {
        super(properties);
    }

    public static boolean canReceiveLaser(BlockState state, Direction hitFace) {
        return state.getBlock() instanceof LaserReceiverBlock && state.getValue(FACING) == hitFace;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return super.areStatesKineticallyEquivalent(oldState, newState) && oldState.getValue(FACING) == newState.getValue(FACING);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public Class<LaserReceiverBlockEntity> getBlockEntityClass() {
        return LaserReceiverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LaserReceiverBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.LASER_RECEIVER.get();
    }
}
