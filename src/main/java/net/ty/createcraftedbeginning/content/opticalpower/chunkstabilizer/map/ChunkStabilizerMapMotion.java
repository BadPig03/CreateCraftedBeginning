package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerLocation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ChunkStabilizerMapMotion {
    private static final long NANOS_PER_TICK = 50000000;
    private static final long MAX_GAP_TICKS = 10;
    private static final long MAX_INTERPOLATION_TICKS = 5;
    private static final double TELEPORT_DISTANCE_SQUARED = 1024;
    @Nullable
    private ChunkStabilizerLocation target;
    @Nullable
    private Vec3 start;
    private long tick;
    private long received;
    private long duration;

    void accept(@Nullable ChunkStabilizerLocation next, long serverTick, long nanos, boolean reset) {
        Vec3 visible = point(nanos);
        long elapsed = serverTick - tick;
        boolean discontinuity = reset || target == null || next == null || elapsed <= 0 || elapsed > MAX_GAP_TICKS;
        if (!discontinuity) {
            discontinuity = target.dimension() != next.dimension() || !target.host().equals(next.host()) || target.part() != next.part() || target.position().distanceToSqr(next.position()) > TELEPORT_DISTANCE_SQUARED;
        }
        if (!discontinuity && visible != null) {
            BlockPos origin = ChunkStabilizerMapSnapshot.textureOrigin(next.blockPosition().pos());
            int x = origin.getX();
            int z = origin.getZ();
            double minX = x + ChunkStabilizerMapSnapshot.WIDTH / 2.0;
            double minZ = z + ChunkStabilizerMapSnapshot.HEIGHT / 2.0;
            double maxX = x + ChunkStabilizerMapSnapshot.TEXTURE_WIDTH - ChunkStabilizerMapSnapshot.WIDTH / 2.0;
            double maxZ = z + ChunkStabilizerMapSnapshot.TEXTURE_HEIGHT - ChunkStabilizerMapSnapshot.HEIGHT / 2.0;
            discontinuity = visible.x < minX || visible.x > maxX || visible.z < minZ || visible.z > maxZ;
        }
        start = discontinuity || visible == null ? null : visible;
        target = next;
        tick = serverTick;
        received = nanos;
        duration = Math.min(elapsed, MAX_INTERPOLATION_TICKS) * NANOS_PER_TICK;
    }

    @Nullable Vec3 origin(long nanos) {
        Vec3 point = point(nanos);
        if (point == null) {
            return null;
        }

        return new Vec3(point.x - ChunkStabilizerMapSnapshot.WIDTH / 2.0, 0, point.z - ChunkStabilizerMapSnapshot.HEIGHT / 2.0);
    }

    @Nullable Vec3 point(long nanos) {
        if (target == null) {
            return null;
        }

        if (start == null || duration <= 0) {
            return target.position();
        }

        double progress = Mth.clamp((double) (nanos - received) / duration, 0, 1);
        return start.lerp(target.position(), progress);
    }
}
