package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.gas.visual.GasPipeOrientation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class SmartAirtightPipeFilterSlot extends ValueBoxTransform {
    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction panel = GasPipeOrientation.panel(state, state.getValue(SmartAirtightPipeBlock.AXIS));
        return new Vec3(0.5, 0.5, 0.5).relative(panel, 6.5 / 16.0);
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack poseStack) {
        Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
        Direction panel = GasPipeOrientation.panel(state, axis);
        TransformStack<PoseTransformStack> transform = TransformStack.of(poseStack);
        if (panel.getAxis() == Axis.Y) {
            transform.rotateYDegrees(-GasPipeOrientation.rotationY(axis, panel));
        }
        if (panel == Direction.UP) {
            transform.rotateXDegrees(90);
            return;
        }

        if (panel == Direction.DOWN) {
            transform.rotateXDegrees(-90);
            return;
        }

        transform.rotateYDegrees(180 - panel.toYRot());
    }

    @Override
    public float getScale() {
        return super.getScale() * 1.02F;
    }
}
