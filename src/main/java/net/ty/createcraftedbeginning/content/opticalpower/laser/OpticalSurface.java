package net.ty.createcraftedbeginning.content.opticalpower.laser;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalAperture;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record OpticalSurface(Vec3 normal, @Nullable OpticalAperture aperture, OpticalInteractor interactor, @Nullable OpticalApertureFrame apertureFrame) {
    private static final double MIN_NORMAL_LENGTH = 1.0E-6;

    public OpticalSurface(Vec3 normal, @Nullable OpticalAperture aperture, OpticalInteractor interactor) {
        this(normal, aperture, interactor, null);
    }

    public OpticalSurface {
        Objects.requireNonNull(interactor, "Optical surface interactor must not be null.");
        double length = normal.length();
        if (!Double.isFinite(length) || length < MIN_NORMAL_LENGTH) {
            throw new IllegalArgumentException("Optical surface normal must have finite length of at least " + MIN_NORMAL_LENGTH + "; got " + normal + '.');
        }

        normal = normal.scale(1 / length);
    }

    public static OpticalSurface opaque(BlockHitResult hit) {
        return new OpticalSurface(Vec3.atLowerCornerOf(hit.getDirection().getNormal()), null, OpticalInteractor.ABSORBING);
    }

    public OpticalSurface transform(CoordinateTransform transform) {
        OpticalAperture transformedAperture = null;
        if (aperture != null) {
            transformedAperture = new OpticalAperture(transform.transformPosition(aperture.center()), transform.transformNormal(aperture.horizontal()).normalize(), transform.transformNormal(aperture.vertical()).normalize(), aperture.halfSize());
        }
        OpticalApertureFrame transformedFrame = null;
        if (apertureFrame != null) {
            transformedFrame = apertureFrame.transform(transform);
        }
        return new OpticalSurface(transform.transformNormal(normal), transformedAperture, interactor, transformedFrame);
    }
}
