package net.ty.createcraftedbeginning.content.cinderincinerationblower;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class CinderIncinerationBlowerBlock extends KineticBlock implements IBE<CinderIncinerationBlowerBlockEntity> {
    public CinderIncinerationBlowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @Override
    public Class<CinderIncinerationBlowerBlockEntity> getBlockEntityClass() {
        return CinderIncinerationBlowerBlockEntity.class;
    }

    @Override
    public BlockEntityType<CinderIncinerationBlowerBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.CINDER_NOZZLE.get();
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CinderIncinerationBlowerBlockEntity nozzle) {
            return Mth.clamp(nozzle.getRange(), 0, 15);
        }
        return 0;
    }
}
