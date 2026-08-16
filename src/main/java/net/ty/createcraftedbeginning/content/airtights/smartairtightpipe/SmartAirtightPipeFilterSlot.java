package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IDirectionalPipe;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IDirectionalPipe.DirectionalFacing;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SmartAirtightPipeFilterSlot extends ValueBoxTransform {
    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
        if (axis != Axis.Y) {
            return VecHelper.voxelSpace(8, 14.5, 8);
        }

        DirectionalFacing facing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
        return switch (facing) {
            case SOUTH -> VecHelper.voxelSpace(8, 8, 1.5);
            case WEST -> VecHelper.voxelSpace(14.5, 8, 8);
            case EAST -> VecHelper.voxelSpace(1.5, 8, 8);
            default -> VecHelper.voxelSpace(8, 8, 14.5);
        };
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
        DirectionalFacing facing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
        TransformStack<PoseTransformStack> transform = TransformStack.of(ms);
        int yAngle = DirectionalFacing.getYAngle(facing);
        switch (axis) {
            case Y -> transform.rotateYDegrees(yAngle);
            case Z -> transform.rotateYDegrees(yAngle).rotateXDegrees(90);
            case X -> transform.rotateYDegrees(yAngle + 90).rotateXDegrees(90);
        }
    }

    @Override
    public float getScale() {
        return super.getScale() * 1.02f;
    }
}
