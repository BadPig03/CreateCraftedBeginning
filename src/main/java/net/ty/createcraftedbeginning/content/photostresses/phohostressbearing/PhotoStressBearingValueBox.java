package net.ty.createcraftedbeginning.content.photostresses.phohostressbearing;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import net.createmod.catnip.math.AngleHelper;
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
public class PhotoStressBearingValueBox extends Sided {
    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Vec3 local = VecHelper.voxelSpace(8, 15.5, 6);
        local = VecHelper.rotateCentered(local, 180 + AngleHelper.horizontalAngle(getSide()), Axis.Z);
        local = VecHelper.rotateCentered(local, AngleHelper.horizontalAngle(Direction.UP), Axis.Y);
        local = VecHelper.rotateCentered(local, AngleHelper.verticalAngle(Direction.UP), Axis.X);
        return local;
    }

    @Override
    protected Vec3 getSouthLocation() {
        return Vec3.ZERO;
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction side) {
        return side.getAxis() != Axis.Y;
    }
}
