package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorReflectionGeometry {
    private static final int MAX_ITERATIONS = 48;
    private static final double MIN_INCIDENCE = 1.0E-4;
    private static final double POSITION_TOLERANCE_SQR = 1.0E-12;

    private AmethystMirrorReflectionGeometry() {
    }

    public static @Nullable Quaternionf solve(Quaternionf initial, Vec3 center, BeamFrame incoming, Vec3 target) {
        Vec3 point = center;
        Vec3 origin = incoming.origin();
        Vec3 direction = incoming.direction();
        Vec3 originalNormal = AmethystMirrorGeometry.create(initial).normal();
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            Vec3 outgoing = target.subtract(point);
            double lengthSqr = outgoing.lengthSqr();
            if (!Double.isFinite(lengthSqr) || lengthSqr < POSITION_TOLERANCE_SQR) {
                return null;
            }

            Vec3 normal = outgoing.normalize().subtract(direction);
            if (normal.lengthSqr() < MIN_INCIDENCE * MIN_INCIDENCE) {
                return null;
            }

            normal = normal.normalize();
            double incidence = normal.dot(direction);
            if (incidence >= -MIN_INCIDENCE) {
                return null;
            }

            double distance = (AmethystMirrorGeometry.MIRROR_HALF_THICKNESS - normal.dot(origin.subtract(center))) / incidence;
            if (!Double.isFinite(distance) || distance <= 0) {
                return null;
            }

            Vec3 next = origin.add(direction.scale(distance));
            if (next.distanceToSqr(point) <= POSITION_TOLERANCE_SQR) {
                if (normal.dot(originalNormal) < 0) {
                    normal = normal.scale(-1);
                }
                return AmethystMirrorGeometry.faceDirection(initial, normal);
            }

            point = next;
        }
        return null;
    }
}
