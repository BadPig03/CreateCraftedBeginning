package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserEmitterRangeValueBox extends Sided {
    private static final double PANEL_OFFSET_FROM_CENTER = 0.125;

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(DirectionalBlock.FACING);
        return super.getLocalOffset(level, pos, state).subtract(Vec3.atLowerCornerOf(facing.getNormal()).scale(PANEL_OFFSET_FROM_CENTER));
    }

    @Override
    public float getScale() {
        return 0.375F;
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack poseStack) {
        Vector3f top = state.getValue(DirectionalBlock.FACING).step();
        Vector3f inward = getSide().getOpposite().step();
        Vector3f horizontal = top.cross(inward, new Vector3f());
        poseStack.mulPose(new Quaternionf().setFromNormalized(new Matrix3f(horizontal, top, inward)));
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 8, 16);
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction side) {
        return side.getAxis() != state.getValue(DirectionalBlock.FACING).getAxis();
    }
}
