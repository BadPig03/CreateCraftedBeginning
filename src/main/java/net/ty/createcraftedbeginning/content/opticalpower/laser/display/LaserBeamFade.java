package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LaserBeamFade(Vec3 origin, Vec3 direction, double remainingRange, double length) {
    public double at(Vec3 point) {
        return Math.clamp((remainingRange - point.subtract(origin).dot(direction)) / length, 0, 1);
    }

    public Vec3 start() {
        return origin.add(direction.scale(remainingRange - length));
    }
}
