package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class CreativeAirtightTankPressureValueBox extends Sided {
    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 8, 15.5);
    }
}
