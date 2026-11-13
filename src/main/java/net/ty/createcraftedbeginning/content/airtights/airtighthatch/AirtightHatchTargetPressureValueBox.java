package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHatchTargetPressureValueBox extends Sided {
    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(AirtightHatchBlock.FACING);
        Vec3 canisterCenter = VecHelper.voxelSpace(8, 8, 8).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.1875));
        Vec3 faceOffset = Vec3.atLowerCornerOf(getSide().getNormal()).scale(0.21875);
        return canisterCenter.add(faceOffset);
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 8, 11);
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        return direction.getAxis() != Axis.Y && direction != state.getValue(AirtightHatchBlock.FACING);
    }
}
