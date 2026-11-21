package net.ty.createcraftedbeginning.gas.flow;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasFlowGeometry {
    private static final double RAY_EPSILON = 0.0001;

    private GasFlowGeometry() {
    }

    static Vec3 closestPoint(Entity entity, CoordinateTransform transform, Vec3 worldPoint) {
        AABB bounds = entity.getBoundingBox();
        Vec3 local = transform.inverseTransformPosition(worldPoint);
        return transform.transformPosition(new Vec3(Mth.clamp(local.x, bounds.minX + RAY_EPSILON, bounds.maxX - RAY_EPSILON), Mth.clamp(local.y, bounds.minY + RAY_EPSILON, bounds.maxY - RAY_EPSILON), Mth.clamp(local.z, bounds.minZ + RAY_EPSILON, bounds.maxZ - RAY_EPSILON)));
    }

    static double projectedRadius(AABB bounds, CoordinateTransform transform, Vec3 axis) {
        return Math.abs(transform.transformNormal(new Vec3(bounds.getXsize() * 0.5, 0, 0)).dot(axis)) + Math.abs(transform.transformNormal(new Vec3(0, bounds.getYsize() * 0.5, 0)).dot(axis)) + Math.abs(transform.transformNormal(new Vec3(0, 0, bounds.getZsize() * 0.5)).dot(axis));
    }

    static boolean canSee(ServerLevel level, Entity entity, BlockPos sourcePos, Vec3 start, Vec3 end) {
        boolean loaded = BlockGetter.traverseBlocks(start, end, level, (world, pos) -> {
            if (world.isLoaded(pos)) {
                return null;
            }

            return false;
        }, world -> world.isLoaded(BlockPos.containing(start)));
        return loaded && level.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, new GasFlowCollisionContext(entity, sourcePos))).getType() == Type.MISS;
    }
}
