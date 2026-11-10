package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AmethystMirrorGeometry(Vec3 normal, Vec3 horizontal, Vec3 vertical) {
    public static final double FRAME_HALF_SIZE = 0.4375;
    public static final double MIRROR_HALF_SIZE = 0.3125;
    public static final double HALF_THICKNESS = 0.125;
    public static final double MIRROR_HALF_THICKNESS = 0.03125;
    public static final double HIT_TOLERANCE = 1.0E-6;

    public static Quaternionf createRotation(Direction facing, float angle, float tilt) {
        return createRotation(facing, angle, tilt, 0);
    }

    public static Quaternionf createRotation(Direction facing, float angle, float tilt, float roll) {
        return facing.getRotation().rotateY((float) Math.toRadians(angle)).rotateX((float) Math.toRadians(tilt)).rotateZ((float) Math.toRadians(roll));
    }

    public static AmethystMirrorGeometry create(Direction facing, float angle, float tilt) {
        return create(createRotation(facing, angle, tilt));
    }

    public static AmethystMirrorGeometry create(Quaternionf rotation) {
        Vector3f normal = rotation.transform(new Vector3f(0, 0, -1));
        Vector3f horizontal = rotation.transform(new Vector3f(1, 0, 0));
        Vector3f vertical = rotation.transform(new Vector3f(0, 1, 0));
        return new AmethystMirrorGeometry(new Vec3(normal).normalize(), new Vec3(horizontal).normalize(), new Vec3(vertical).normalize());
    }

    public static Quaternionf faceDirection(Quaternionf rotation, Vec3 direction) {
        double length = direction.length();
        if (!Double.isFinite(length) || length < HIT_TOLERANCE) {
            throw new IllegalArgumentException("Mirror target direction length must be finite and at least " + HIT_TOLERANCE + "; got " + length + " for direction " + direction + '.');
        }

        Vec3 target = direction.scale(1 / length);
        AmethystMirrorGeometry geometry = create(rotation);
        Vec3 normal = geometry.normal();
        double dot = Math.clamp(normal.dot(target), -1, 1);
        Vec3 cross = normal.cross(target);
        double sine = cross.length();
        Quaternionf turn = new Quaternionf();
        if (sine > HIT_TOLERANCE) {
            Vector3f axis = cross.scale(1 / sine).toVector3f();
            turn.rotationAxis((float) Math.atan2(sine, dot), axis);
        }
        else if (dot < 0) {
            turn.rotationAxis((float) Math.PI, geometry.vertical().toVector3f());
        }
        return turn.mul(rotation).normalize();
    }

    public Vec3 toLocal(Vec3 vector) {
        return new Vec3(vector.dot(horizontal), vector.dot(vertical), -vector.dot(normal));
    }

    public Vec3 toWorld(Vec3 vector) {
        return horizontal.scale(vector.x).add(vertical.scale(vector.y)).subtract(normal.scale(vector.z));
    }

    public boolean isReflectivePoint(Vec3 fromCenter) {
        return Math.abs(Math.abs(fromCenter.dot(normal)) - MIRROR_HALF_THICKNESS) <= HIT_TOLERANCE && Math.abs(fromCenter.dot(horizontal)) <= MIRROR_HALF_SIZE + HIT_TOLERANCE && Math.abs(fromCenter.dot(vertical)) <= MIRROR_HALF_SIZE + HIT_TOLERANCE;
    }
}
