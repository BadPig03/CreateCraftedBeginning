package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import dev.engine_room.flywheel.lib.transform.TransformStack;
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
class AirtightHatchValueBox extends Sided {
    private static final int COLOR = 0x191C26;

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Vec3 location = VecHelper.rotateCentered(getSouthLocation(), -90, Axis.X);
        return VecHelper.rotateCentered(location, AngleHelper.horizontalAngle(state.getValue(AirtightHatchBlock.FACING)), Axis.Y);
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 5, 15.5);
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack poseStack) {
        super.rotate(level, pos, state, poseStack);
        TransformStack.of(poseStack).rotateZDegrees(180 - AngleHelper.horizontalAngle(state.getValue(AirtightHatchBlock.FACING)));
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        return direction == Direction.UP;
    }

    @Override
    public int getOverrideColor() {
        return COLOR;
    }
}
