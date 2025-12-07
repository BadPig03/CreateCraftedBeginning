package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class AirtightHatchValueBox extends Sided {
    private static final int COLOR = 0x191C26;

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 5, 15.5);
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, @NotNull BlockState state) {
        Vec3 location = getSouthLocation();
        Direction facing = state.getValue(AirtightHatchBlock.FACING);
        location = VecHelper.rotateCentered(location, -90, Axis.X);
        location = VecHelper.rotateCentered(location, AngleHelper.horizontalAngle(facing), Axis.Y);
        return location;
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, @NotNull BlockState state, PoseStack ms) {
        super.rotate(level, pos, state, ms);
        TransformStack.of(ms).rotateZDegrees(180 - AngleHelper.horizontalAngle(state.getValue(AirtightHatchBlock.FACING)));
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
