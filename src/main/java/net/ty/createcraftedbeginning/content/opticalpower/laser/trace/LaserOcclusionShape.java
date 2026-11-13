package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserOcclusionShape {
    private static final double CELLS_PER_BLOCK = 16;
    static final int TRANSFORM_BOUNDARY_ULPS = 4;

    private LaserOcclusionShape() {
    }

    static boolean isPrecise(BlockState state) {
        return state.getBlock() instanceof OpticalElement;
    }

    static @Unmodifiable List<AABB> boxes(VoxelShape shape) {
        Set<AABB> boxes = new LinkedHashSet<>();
        for (AABB box : shape.toAabbs()) {
            if (box.getXsize() <= 0 || box.getYsize() <= 0 || box.getZsize() <= 0) {
                continue;
            }

            boxes.add(new AABB(Math.floor(box.minX * CELLS_PER_BLOCK) / CELLS_PER_BLOCK, Math.floor(box.minY * CELLS_PER_BLOCK) / CELLS_PER_BLOCK, Math.floor(box.minZ * CELLS_PER_BLOCK) / CELLS_PER_BLOCK, Math.ceil(box.maxX * CELLS_PER_BLOCK) / CELLS_PER_BLOCK, Math.ceil(box.maxY * CELLS_PER_BLOCK) / CELLS_PER_BLOCK, Math.ceil(box.maxZ * CELLS_PER_BLOCK) / CELLS_PER_BLOCK));
        }
        return List.copyOf(boxes);
    }

    static @Nullable BlockHitResult clip(VoxelShape shape, Vec3 start, Vec3 end) {
        return clip(boxes(shape), start, end);
    }

    static @Nullable BlockHitResult clip(List<AABB> boxes, Vec3 start, Vec3 end) {
        return clip(boxes, start, end, BlockPos.ZERO, false);
    }

    static @Nullable BlockHitResult clip(List<AABB> boxes, Vec3 start, Vec3 end, BlockPos pos, boolean transformed) {
        Vec3 error = Vec3.ZERO;
        if (transformed) {
            error = new Vec3(Math.max(Math.ulp(start.x), Math.ulp(end.x)), Math.max(Math.ulp(start.y), Math.ulp(end.y)), Math.max(Math.ulp(start.z), Math.ulp(end.z))).scale(TRANSFORM_BOUNDARY_ULPS);
        }
        return clip(boxes, start, end, pos, error);
    }

    static @Nullable BlockHitResult clip(List<AABB> boxes, Vec3 start, Vec3 end, BlockPos pos, Vec3 error) {
        if (boxes.isEmpty()) {
            return null;
        }

        Vec3 blockOrigin = Vec3.atLowerCornerOf(pos);
        Vec3 localStart = start.subtract(blockOrigin);
        Vec3 localEnd = end.subtract(blockOrigin);
        if (!error.equals(Vec3.ZERO)) {
            localStart = snapTransformBoundary(localStart, error);
            localEnd = snapTransformBoundary(localEnd, error);
        }
        Vec3 delta = localEnd.subtract(localStart);
        if (delta.lengthSqr() == 0) {
            return null;
        }

        double nearest = Double.POSITIVE_INFINITY;
        Direction incomingNormal = Direction.getNearest(delta.x, delta.y, delta.z).getOpposite();
        Direction nearestNormal = incomingNormal;
        boolean inside = false;
        for (AABB box : boxes) {
            double entry = 0;
            double exit = 1;
            Direction normal = incomingNormal;
            for (Axis axis : Axis.values()) {
                double origin = axis.choose(localStart.x, localStart.y, localStart.z);
                double velocity = axis.choose(delta.x, delta.y, delta.z);
                double minimum = axis.choose(box.minX, box.minY, box.minZ);
                double maximum = axis.choose(box.maxX, box.maxY, box.maxZ);
                if (velocity == 0) {
                    if (origin < minimum || origin >= maximum) {
                        exit = -1;
                        break;
                    }

                    continue;
                }

                double first = (minimum - origin) / velocity;
                double last = (maximum - origin) / velocity;
                if (velocity < 0) {
                    double swap = first;
                    first = last;
                    last = swap;
                }
                if (first > entry) {
                    entry = first;
                    normal = Direction.fromAxisAndDirection(axis, velocity > 0 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE);
                }
                exit = Math.min(exit, last);
            }
            if (entry >= exit || entry >= nearest) {
                continue;
            }

            nearest = entry;
            nearestNormal = normal;
            inside = box.contains(localStart);
        }
        if (!Double.isFinite(nearest)) {
            return null;
        }

        return new BlockHitResult(start.lerp(end, nearest), nearestNormal, pos, inside);
    }

    private static Vec3 snapTransformBoundary(Vec3 point, Vec3 error) {
        double x = Math.rint(point.x * CELLS_PER_BLOCK) / CELLS_PER_BLOCK;
        double y = Math.rint(point.y * CELLS_PER_BLOCK) / CELLS_PER_BLOCK;
        double z = Math.rint(point.z * CELLS_PER_BLOCK) / CELLS_PER_BLOCK;
        if (Math.abs(point.x - x) > error.x) {
            x = point.x;
        }
        if (Math.abs(point.y - y) > error.y) {
            y = point.y;
        }
        if (Math.abs(point.z - z) > error.z) {
            z = point.z;
        }
        return new Vec3(x, y, z);
    }
}
