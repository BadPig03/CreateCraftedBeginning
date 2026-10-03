package net.ty.createcraftedbeginning.content.opticalpower.laser;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamGeometry {
    private static final double PARALLEL_TOLERANCE = 1.0E-8;

    private LaserBeamGeometry() {
    }

    public static double intersectPlaneDistance(Vec3 start, Vec3 direction, Vec3 planePoint, Vec3 planeNormal, double fallbackDistance, double maxDistance) {
        double denominator = direction.dot(planeNormal);
        if (Math.abs(denominator) < PARALLEL_TOLERANCE) {
            return Mth.clamp(fallbackDistance, 0, maxDistance);
        }

        double distance = planePoint.subtract(start).dot(planeNormal) / denominator;
        if (!Double.isFinite(distance)) {
            return Mth.clamp(fallbackDistance, 0, maxDistance);
        }

        return Mth.clamp(distance, 0, maxDistance);
    }
}
