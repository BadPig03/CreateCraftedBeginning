package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorDragGeometry {
    private static final double MIN_PROJECTION_LENGTH = 0.1;
    private static final double MIN_CAMERA_DEPTH = 0.001;
    private static final double MIN_DIRECTION_COMPONENT = 1.0E-6;

    private AmethystMirrorDragGeometry() {
    }

    static Vec3 projectDirection(Quaternionf rotation, Vec3 localHandle, boolean horizontalAxis, Vec3 cameraToCenter, Quaternionf cameraRotation) {
        return projectDirection(new Matrix3f().rotation(rotation), localHandle, horizontalAxis, cameraToCenter, cameraRotation);
    }

    static Vec3 projectDirection(Matrix3fc rotation, Vec3 localHandle, boolean horizontalAxis, Vec3 cameraToCenter, Quaternionf cameraRotation) {
        Vec3 axis = new Vec3(0, 1, 0);
        if (horizontalAxis) {
            axis = new Vec3(1, 0, 0);
        }
        Vec3 localTangent = axis.cross(localHandle).normalize();
        Vec3 offset = new Vec3(rotation.transform(localHandle.toVector3f()));
        Vector3f tangent = rotation.transform(localTangent.toVector3f());
        Quaternionf view = new Quaternionf(cameraRotation).conjugate();
        Vector3f position = view.transform(cameraToCenter.add(offset).toVector3f());
        view.transform(tangent);
        double depth = -position.z;
        if (depth <= MIN_CAMERA_DEPTH) {
            return Vec3.ZERO;
        }

        double screenX = tangent.x + position.x * tangent.z / depth;
        double screenY = -tangent.y - position.y * tangent.z / depth;
        double length = Math.hypot(screenX, screenY);
        Vector3f radial = view.transform(offset.toVector3f());
        if (length < MIN_PROJECTION_LENGTH) {
            screenX = radial.x;
            screenY = -radial.y;
        }

        double component = screenX;
        double fallback = radial.x;
        double localFallback = localHandle.x;
        if (horizontalAxis) {
            component = screenY;
            fallback = -radial.y;
            localFallback = -localHandle.y;
        }
        if (Math.abs(component) < MIN_DIRECTION_COMPONENT) {
            component = fallback;
        }
        if (Math.abs(component) < MIN_DIRECTION_COMPONENT) {
            component = localFallback;
        }
        double sign = Math.copySign(1, component);
        if (horizontalAxis) {
            return new Vec3(0, sign, 0);
        }

        return new Vec3(sign, 0, 0);
    }
}
