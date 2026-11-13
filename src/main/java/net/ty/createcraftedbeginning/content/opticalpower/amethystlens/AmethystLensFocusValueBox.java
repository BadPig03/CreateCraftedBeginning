package net.ty.createcraftedbeginning.content.opticalpower.amethystlens;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystLensFocusValueBox extends Sided {
    @Override
    public float getScale() {
        return 0.3F;
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 8, 8.5);
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction side) {
        return side.getAxis() == state.getValue(AmethystLensBlock.FACING).getAxis();
    }
}
