package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceGeometry.PreparedBlock;
import net.ty.createcraftedbeginning.mixin.common.accessor.ClipContextAccessor;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Function;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBlockClip {
    public static final int NEIGHBOR_REACH = 1;

    private static final double CELL_EXIT_TOLERANCE_SQR = 1.0E-12;

    private LaserBlockClip() {
    }

    public static @Nullable BlockHitResult clipCell(BlockGetter level, ClipContext context, BlockPos cell) {
        Vec3 start = context.getFrom();
        Vec3 end = context.getTo();
        Function<BlockPos, @Nullable BlockHitResult> query = pos -> {
            if (level instanceof Level world && !world.isLoaded(pos)) {
                return null;
            }

            BlockState state = level.getBlockState(pos);
            VoxelShape shape = context.getBlockShape(state, level, pos);
            if (LaserOcclusionShape.isPrecise(state)) {
                return shape.clip(start, end, pos);
            }

            boolean transformed = level instanceof Level world && SubLevelBridge.resolve(world, pos).inSubLevel();
            return LaserOcclusionShape.clip(LaserOcclusionShape.boxes(shape), start, end, pos, transformed);
        };
        if (((ClipContextAccessor) context).ccb$getCollisionContext() instanceof LaserCollisionContext collision) {
            LaserTraceGeometry geometry = collision.getGeometry();
            float partialTick = collision.getPartialTick();
            query = collision.cacheQuery(level, start, end, pos -> {
                PreparedBlock block = geometry.getWorldBlock(level, pos, context, partialTick);
                if (block == null) {
                    return null;
                }

                return block.clip(start, end, pos);
            });
            List<BlockPos> candidates = geometry.getWorldCandidates(level, cell, context, partialTick);
            if (candidates.isEmpty()) {
                return null;
            }

            return clipCell(start, end, cell, candidates, query);
        }

        Iterable<BlockPos> candidates = BlockPos.betweenClosed(cell.offset(-NEIGHBOR_REACH, -NEIGHBOR_REACH, -NEIGHBOR_REACH), cell.offset(NEIGHBOR_REACH, NEIGHBOR_REACH, NEIGHBOR_REACH));
        return clipCell(start, end, cell, candidates, query);
    }

    static @Nullable BlockHitResult clipCell(Vec3 start, Vec3 end, BlockPos cell, Iterable<BlockPos> candidates, Function<BlockPos, @Nullable BlockHitResult> query) {
        Vec3 delta = end.subtract(start);
        double exit = 1;
        if (delta.x > 0) {
            exit = Math.min(exit, (cell.getX() + 1 - start.x) / delta.x);
        }
        else if (delta.x < 0) {
            exit = Math.min(exit, (cell.getX() - start.x) / delta.x);
        }
        if (delta.y > 0) {
            exit = Math.min(exit, (cell.getY() + 1 - start.y) / delta.y);
        }
        else if (delta.y < 0) {
            exit = Math.min(exit, (cell.getY() - start.y) / delta.y);
        }
        if (delta.z > 0) {
            exit = Math.min(exit, (cell.getZ() + 1 - start.z) / delta.z);
        }
        else if (delta.z < 0) {
            exit = Math.min(exit, (cell.getZ() - start.z) / delta.z);
        }
        if (exit < 0) {
            return null;
        }

        double limit = delta.lengthSqr() * exit * exit;
        BlockHitResult nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (BlockPos cursor : candidates) {
            BlockHitResult hit = query.apply(cursor.immutable());
            if (hit == null) {
                continue;
            }

            double candidateDistance = start.distanceToSqr(hit.getLocation());
            if (candidateDistance > limit + CELL_EXIT_TOLERANCE_SQR || candidateDistance >= distance) {
                continue;
            }

            distance = candidateDistance;
            nearest = hit;
        }
        return nearest;
    }
}
