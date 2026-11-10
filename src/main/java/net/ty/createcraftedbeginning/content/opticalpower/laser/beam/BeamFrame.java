package net.ty.createcraftedbeginning.content.opticalpower.laser.beam;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record BeamFrame(Vec3 origin, Vec3 direction, Vec3 transverseAxis) {
    private static final double MIN_AXIS_LENGTH = 1.0E-6;

    public BeamFrame {
        Objects.requireNonNull(origin, "Beam origin must not be null.");
        if (!Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z)) {
            throw new IllegalArgumentException("Beam origin must have finite coordinates; got " + origin + '.');
        }

        direction = normalizeAxis(direction, "Beam direction");
        transverseAxis = normalizeAxis(transverseAxis, "Beam transverse axis");
        transverseAxis = normalizeAxis(transverseAxis.subtract(direction.scale(transverseAxis.dot(direction))), "Projected beam transverse axis");
    }

    public static BeamFrame fromDirection(Vec3 origin, Vec3 direction) {
        Vec3 normalized = normalizeAxis(direction, "Beam direction");
        double x = Math.abs(normalized.x);
        double y = Math.abs(normalized.y);
        double z = Math.abs(normalized.z);
        Vec3 transverseAxis = new Vec3(1, 0, 0);
        if (y < x && y <= z) {
            transverseAxis = new Vec3(0, 1, 0);
        }
        else if (z < x && z < y) {
            transverseAxis = new Vec3(0, 0, 1);
        }
        return new BeamFrame(origin, normalized, transverseAxis);
    }

    private static Vec3 normalizeAxis(Vec3 axis, String field) {
        Objects.requireNonNull(axis, () -> field + " must not be null.");
        double length = Math.hypot(Math.hypot(axis.x, axis.y), axis.z);
        if (!Double.isFinite(length) || length < MIN_AXIS_LENGTH) {
            throw new IllegalArgumentException(field + " must have finite length of at least " + MIN_AXIS_LENGTH + "; got " + axis + '.');
        }

        return axis.scale(1 / length);
    }

    public Vec3 getSecondTransverseAxis() {
        return direction.cross(transverseAxis);
    }

    public BeamFrame advance(double distance) {
        if (!Double.isFinite(distance) || distance < 0) {
            throw new IllegalArgumentException("Beam propagation distance must be finite and non-negative; got " + distance + " blocks.");
        }

        return new BeamFrame(origin.add(direction.scale(distance)), direction, transverseAxis);
    }
}
