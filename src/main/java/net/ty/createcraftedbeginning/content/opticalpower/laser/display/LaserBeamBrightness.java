package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LaserBeamBrightness(Vec3 origin, Vec3 direction, BeamProfile profile) {
    public static final double REFERENCE_RADIUS = 0.03125;

    public static @Nullable LaserBeamBrightness fromBeam(BeamState beam) {
        BeamProfile profile = beam.profile();
        if (profile.widthSlope() == 0) {
            double radius = profile.radius();
            if (radius == REFERENCE_RADIUS) {
                return null;
            }

            return new LaserBeamBrightness(Vec3.ZERO, Vec3.ZERO, new BeamProfile(radius));
        }

        BeamFrame frame = beam.frame();
        return new LaserBeamBrightness(frame.origin(), frame.direction(), profile);
    }

    public double opacityAt(Vec3 point, double exposure) {
        double radius = profile.radius();
        double slope = profile.widthSlope();
        if (slope != 0) {
            radius = new BeamProfile(referenceWidthAt(point), slope).radius();
        }
        return Math.min(1, exposure * REFERENCE_RADIUS / radius);
    }

    public double referenceWidthAt(Vec3 point) {
        double distance = (point.x - origin.x) * direction.x + (point.y - origin.y) * direction.y + (point.z - origin.z) * direction.z;
        return profile.referenceWidth() + profile.widthSlope() * distance;
    }
}
