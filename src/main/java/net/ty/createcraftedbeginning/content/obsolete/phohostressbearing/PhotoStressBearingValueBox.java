package net.ty.createcraftedbeginning.content.obsolete.phohostressbearing;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.Pointing;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class PhotoStressBearingValueBox extends Sided {
    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction side = getSide();
        Direction facing = Direction.UP;

        float roll = 0;
        for (Pointing p : Pointing.values()) {
            if (p.getCombinedDirection(facing) == side) {
                roll = p.getXRotation();
            }
        }
        roll += 180;

        float horizontalAngle = AngleHelper.horizontalAngle(facing);
        float verticalAngle = AngleHelper.verticalAngle(facing);
        Vec3 local = VecHelper.voxelSpace(8, 15.5, 6);

        local = VecHelper.rotateCentered(local, roll, Axis.Z);
        local = VecHelper.rotateCentered(local, horizontalAngle, Axis.Y);
        local = VecHelper.rotateCentered(local, verticalAngle, Axis.X);

        return local;
    }

    @Override
    protected Vec3 getSouthLocation() {
        return Vec3.ZERO;
    }

    @Override
    protected boolean isSideActive(BlockState state, @NotNull Direction side) {
        return side.getAxis() != Axis.Y;
    }
}
