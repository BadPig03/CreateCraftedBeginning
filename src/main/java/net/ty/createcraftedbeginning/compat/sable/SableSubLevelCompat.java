package net.ty.createcraftedbeginning.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.platform.CCBSubLevelBridge;
import net.ty.createcraftedbeginning.platform.CCBSubLevelBridge.EntityArea;
import net.ty.createcraftedbeginning.platform.CCBSubLevelBridge.Projection;
import net.ty.createcraftedbeginning.platform.CCBSubLevelBridge.Service;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SableSubLevelCompat implements Service {
    private static final double SAT_EPSILON = 1.0E-7;
    private static final Vec3 X_AXIS = new Vec3(1, 0, 0);
    private static final Vec3 Y_AXIS = new Vec3(0, 1, 0);
    private static final Vec3 Z_AXIS = new Vec3(0, 0, 1);

    private SableSubLevelCompat() {
    }

    public static void install() {
        CCBSubLevelBridge.install(new SableSubLevelCompat());
    }

    private static OrientedBox createOrientedBox(AABB bounds, @Nullable SubLevelAccess subLevel) {
        Vec3 center = bounds.getCenter();
        double extentX = (bounds.maxX - bounds.minX) * 0.5;
        double extentY = (bounds.maxY - bounds.minY) * 0.5;
        double extentZ = (bounds.maxZ - bounds.minZ) * 0.5;
        if (subLevel == null) {
            return new OrientedBox(center, X_AXIS, Y_AXIS, Z_AXIS, extentX, extentY, extentZ);
        }

        Pose3dc pose = subLevel.logicalPose();
        Vec3 transformedX = pose.transformNormal(X_AXIS);
        Vec3 transformedY = pose.transformNormal(Y_AXIS);
        Vec3 transformedZ = pose.transformNormal(Z_AXIS);
        double scaleX = transformedX.length();
        double scaleY = transformedY.length();
        double scaleZ = transformedZ.length();
        return new OrientedBox(pose.transformPosition(center), normalizeOrFallback(transformedX, X_AXIS), normalizeOrFallback(transformedY, Y_AXIS), normalizeOrFallback(transformedZ, Z_AXIS), extentX * scaleX, extentY * scaleY, extentZ * scaleZ);
    }

    private static Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback) {
        double length = vector.length();
        return length > SAT_EPSILON ? vector.scale(1.0 / length) : fallback;
    }

    @Override
    public Projection resolve(Level level, Position position) {
        boolean inSubLevel = SableCompanion.INSTANCE.getContaining(level, position) != null;
        Vec3 projectedPosition = SableCompanion.INSTANCE.projectOutOfSubLevel(level, position);
        return new Projection(projectedPosition, inSubLevel);
    }

    @Override
    public EntityArea createEntityArea(Level level, BlockPos origin, AABB localBounds) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, origin);
        return new SableEntityArea(createOrientedBox(localBounds, subLevel));
    }

    private static final class SableEntityArea implements EntityArea {
        private final OrientedBox bounds;
        private final double[] rotation;
        private final double[] absoluteRotation;
        private final double[] translatedCenter;

        private SableEntityArea(OrientedBox bounds) {
            this.bounds = bounds;
            rotation = new double[9];
            absoluteRotation = new double[9];
            translatedCenter = new double[3];
        }

        private static boolean intersects(OrientedBox first, OrientedBox second, double[] rotation, double[] absoluteRotation, double[] translatedCenter) {
            for (int firstAxis = 0; firstAxis < 3; firstAxis++) {
                for (int secondAxis = 0; secondAxis < 3; secondAxis++) {
                    int rotationIndex = firstAxis * 3 + secondAxis;
                    double dot = first.axis(firstAxis).dot(second.axis(secondAxis));
                    rotation[rotationIndex] = dot;
                    absoluteRotation[rotationIndex] = Math.abs(dot) + SAT_EPSILON;
                }
            }

            Vec3 centerDelta = second.center.subtract(first.center);
            for (int firstAxis = 0; firstAxis < 3; firstAxis++) {
                translatedCenter[firstAxis] = centerDelta.dot(first.axis(firstAxis));
            }

            for (int firstAxis = 0; firstAxis < 3; firstAxis++) {
                double secondRadius = 0;
                for (int secondAxis = 0; secondAxis < 3; secondAxis++) {
                    secondRadius += second.extent(secondAxis) * absoluteRotation[firstAxis * 3 + secondAxis];
                }
                if (Math.abs(translatedCenter[firstAxis]) > first.extent(firstAxis) + secondRadius) {
                    return false;
                }
            }

            for (int secondAxis = 0; secondAxis < 3; secondAxis++) {
                double firstRadius = 0;
                double projectedCenter = 0;
                for (int firstAxis = 0; firstAxis < 3; firstAxis++) {
                    int rotationIndex = firstAxis * 3 + secondAxis;
                    firstRadius += first.extent(firstAxis) * absoluteRotation[rotationIndex];
                    projectedCenter += translatedCenter[firstAxis] * rotation[rotationIndex];
                }
                if (Math.abs(projectedCenter) > firstRadius + second.extent(secondAxis)) {
                    return false;
                }
            }

            for (int firstAxis = 0; firstAxis < 3; firstAxis++) {
                int firstNext = (firstAxis + 1) % 3;
                int firstLast = (firstAxis + 2) % 3;
                for (int secondAxis = 0; secondAxis < 3; secondAxis++) {
                    int secondNext = (secondAxis + 1) % 3;
                    int secondLast = (secondAxis + 2) % 3;
                    double firstRadius = first.extent(firstNext) * absoluteRotation[firstLast * 3 + secondAxis] + first.extent(firstLast) * absoluteRotation[firstNext * 3 + secondAxis];
                    double secondRadius = second.extent(secondNext) * absoluteRotation[firstAxis * 3 + secondLast] + second.extent(secondLast) * absoluteRotation[firstAxis * 3 + secondNext];
                    double projectedCenter = Math.abs(translatedCenter[firstLast] * rotation[firstNext * 3 + secondAxis] - translatedCenter[firstNext] * rotation[firstLast * 3 + secondAxis]);
                    if (projectedCenter > firstRadius + secondRadius) {
                        return false;
                    }
                }
            }

            return true;
        }

        @Override
        public boolean intersects(Entity entity) {
            SubLevelAccess entitySubLevel = SableCompanion.INSTANCE.getContaining(entity);
            OrientedBox entityBounds = createOrientedBox(entity.getBoundingBox(), entitySubLevel);
            return intersects(bounds, entityBounds, rotation, absoluteRotation, translatedCenter);
        }
    }

    private record OrientedBox(Vec3 center, Vec3 axisX, Vec3 axisY, Vec3 axisZ, double extentX, double extentY, double extentZ) {
        private Vec3 axis(int axisIndex) {
            return switch (axisIndex) {
                case 0 -> axisX;
                case 1 -> axisY;
                default -> axisZ;
            };
        }

        private double extent(int axisIndex) {
            return switch (axisIndex) {
                case 0 -> extentX;
                case 1 -> extentY;
                default -> extentZ;
            };
        }
    }
}
