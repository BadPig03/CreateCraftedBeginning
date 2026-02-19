package net.ty.createcraftedbeginning.content.end.endincinerationblower;

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

public class EndIncinerationBlowerValueBox extends Sided {
    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 8, 15.5);
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, @NotNull BlockState state, PoseStack ms) {
        super.rotate(level, pos, state, ms);
        TransformStack.of(ms).rotateZDegrees(-AngleHelper.horizontalAngle(Direction.UP));
    }

    @Override
    protected boolean isSideActive(BlockState state, @NotNull Direction direction) {
        return direction.getAxis() != Axis.Y;
    }
}
