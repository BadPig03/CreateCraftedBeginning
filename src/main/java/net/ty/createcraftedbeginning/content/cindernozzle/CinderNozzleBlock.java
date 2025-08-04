package net.ty.createcraftedbeginning.content.cindernozzle;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class CinderNozzleBlock extends KineticBlock implements IBE<CinderNozzleBlockEntity> {
    public CinderNozzleBlock(Properties properties) {
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
    public Class<CinderNozzleBlockEntity> getBlockEntityClass() {
        return CinderNozzleBlockEntity.class;
    }

    @Override
    public BlockEntityType<CinderNozzleBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.CINDER_NOZZLE.get();
    }
}
