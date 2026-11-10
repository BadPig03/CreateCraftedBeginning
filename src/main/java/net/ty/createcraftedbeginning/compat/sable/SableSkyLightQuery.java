package net.ty.createcraftedbeginning.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.platform.SkyLightCollisionContext;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class SableSkyLightQuery {
    private static final double RAY_EPSILON = 1.0E-4;
    private final List<Candidate> candidates;
    private final SkyLightCollisionContext collisionContext;

    SableSkyLightQuery(Level level, Iterable<? extends SubLevelAccess> subLevels, int sampleCount) {
        collisionContext = SkyLightCollisionContext.createQuery(level, sampleCount);
        List<Candidate> snapshot = new ArrayList<>();
        for (SubLevelAccess subLevel : subLevels) {
            snapshot.add(new Candidate(subLevel, new BoundingBox3d(subLevel.boundingBox()), new Pose3d(subLevel.logicalPose())));
        }
        candidates = List.copyOf(snapshot);
    }

    boolean isVisible(Level level, Vec3 position, BoundingBox3dc skyColumn) {
        BlockPos samplePos = BlockPos.containing(position);
        if (!level.isLoaded(samplePos)) {
            return false;
        }

        double skyTop = Math.max(position.y, level.getHeight(Types.WORLD_SURFACE, samplePos.getX(), samplePos.getZ()));
        for (Candidate candidate : candidates) {
            BoundingBox3dc bounds = candidate.bounds();
            if (!bounds.intersects(skyColumn)) {
                continue;
            }

            skyTop = Math.max(skyTop, bounds.maxY() + RAY_EPSILON);
        }
        if (skyTop <= position.y + RAY_EPSILON) {
            return true;
        }

        Vec3 start = position.add(0, RAY_EPSILON, 0);
        Vec3 end = new Vec3(position.x, skyTop, position.z);
        SkyLightCollisionContext context = collisionContext;
        if (context.clip(level, new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, context)).getType() != Type.MISS) {
            return false;
        }

        BoundingBox3d rayBounds = new BoundingBox3d(start.x, start.y, start.z, end.x, end.y, end.z);
        for (Candidate candidate : candidates) {
            if (!candidate.bounds().intersects(rayBounds)) {
                continue;
            }

            Pose3dc pose = candidate.pose();
            Vec3 localStart = pose.transformPositionInverse(start);
            if (SableCompanion.INSTANCE.getContaining(level, localStart) != candidate.subLevel()) {
                continue;
            }

            Vec3 localEnd = pose.transformPositionInverse(end);
            if (context.clip(level, new ClipContext(localStart, localEnd, Block.COLLIDER, Fluid.NONE, context)).getType() == Type.MISS) {
                continue;
            }

            return false;
        }
        return true;
    }

    private record Candidate(SubLevelAccess subLevel, BoundingBox3dc bounds, Pose3dc pose) {}
}
