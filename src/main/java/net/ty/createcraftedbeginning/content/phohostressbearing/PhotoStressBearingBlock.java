package net.ty.createcraftedbeginning.content.phohostressbearing;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class PhotoStressBearingBlock extends KineticBlock implements IBE<PhotoStressBearingBlockEntity> {
    public PhotoStressBearingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
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
