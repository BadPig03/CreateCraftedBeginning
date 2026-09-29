package net.ty.createcraftedbeginning.content.airtights.balloon;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.foundation.BoundedMath;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ChainBalloonPose(Vec3 offset, float yaw, float xRotation, float zRotation) {
    public static ChainBalloonPose calculate(Vec3 position, Vec3 target, float yaw, BlockPos origin) {
        Vec3 offset = target.subtract(origin.getX(), origin.getY(), origin.getZ());
        Vec3 dangle = VecHelper.rotate(target.add(0, 0.5, 0).subtract(position), -yaw, Axis.Y);
        float zRotation = BoundedMath.clampMagnitude(Mth.wrapDegrees((float) Mth.atan2(-dangle.x, dangle.y) * Mth.RAD_TO_DEG) / 2, 25);
        float xRotation = BoundedMath.clampMagnitude(Mth.wrapDegrees((float) Mth.atan2(dangle.z, dangle.y) * Mth.RAD_TO_DEG) / 2, 25);
        return new ChainBalloonPose(offset, yaw, xRotation, zRotation);
    }
}
