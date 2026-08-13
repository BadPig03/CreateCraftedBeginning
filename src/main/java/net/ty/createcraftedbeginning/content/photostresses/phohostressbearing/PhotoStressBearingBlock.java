package net.ty.createcraftedbeginning.content.photostresses.phohostressbearing;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PhotoStressBearingBlock extends KineticBlock implements IBE<PhotoStressBearingBlockEntity> {
    public PhotoStressBearingBlock(Properties properties) {
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
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public Class<PhotoStressBearingBlockEntity> getBlockEntityClass() {
        return PhotoStressBearingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PhotoStressBearingBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.PHOTO_STRESS_BEARING.get();
    }
}
