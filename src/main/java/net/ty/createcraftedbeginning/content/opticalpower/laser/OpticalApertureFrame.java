package net.ty.createcraftedbeginning.content.opticalpower.laser;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalAperture;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record OpticalApertureFrame(OpticalAperture opening, double outerHalfSize, double halfThickness) {
    public @Unmodifiable List<OpticalAperture> boundaries() {
        List<OpticalAperture> boundaries = new ArrayList<>();
        Vec3 horizontal = opening.horizontal();
        Vec3 vertical = opening.vertical();
        Vec3 center = opening.center();
        double innerHalfSize = opening.halfSize();
        Vec3 normal = horizontal.cross(vertical);
        for (double depth : new double[]{-halfThickness, 0, halfThickness}) {
            Vec3 plane = center.add(normal.scale(depth));
            boundaries.add(new OpticalAperture(plane, horizontal, vertical, innerHalfSize));
            boundaries.add(new OpticalAperture(plane, horizontal, vertical, outerHalfSize));
        }
        return List.copyOf(boundaries);
    }

    public OpticalApertureFrame transform(CoordinateTransform transform) {
        OpticalAperture transformed = new OpticalAperture(transform.transformPosition(opening.center()), transform.transformNormal(opening.horizontal()).normalize(), transform.transformNormal(opening.vertical()).normalize(), opening.halfSize());
        return new OpticalApertureFrame(transformed, outerHalfSize, halfThickness);
    }
}
