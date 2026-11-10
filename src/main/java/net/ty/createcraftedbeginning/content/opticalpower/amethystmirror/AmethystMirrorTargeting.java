package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorTargeting {
    static final int TARGET_RANGE = 32;
    private static final double TARGET_TOLERANCE = 0.02;
    private static final double LOADED_BOUNDARY_BACKOFF = 1.0E-5;

    private AmethystMirrorTargeting() {
    }

    static boolean isWithinAimRange(Player player, BlockPos mirror) {
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(mirror)) <= TARGET_RANGE * TARGET_RANGE;
    }

    static AmethystMirrorAimStatus getStatus(BlockPos mirror, @Nullable BlockHitResult hit) {
        if (hit == null || hit.getType() != Type.BLOCK) {
            return AmethystMirrorAimStatus.NO_TARGET;
        }

        if (mirror.equals(hit.getBlockPos())) {
            return AmethystMirrorAimStatus.SELF;
        }

        if (Vec3.atCenterOf(mirror).distanceToSqr(hit.getLocation()) > TARGET_RANGE * TARGET_RANGE) {
            return AmethystMirrorAimStatus.TOO_FAR;
        }

        return AmethystMirrorAimStatus.READY;
    }

    static @Nullable BlockHitResult findTarget(Level level, Player player, BlockPos mirror) {
        Vec3 start = player.getEyePosition();
        double reach = TARGET_RANGE + start.distanceTo(Vec3.atCenterOf(mirror)) + 1;
        Vec3 end = start.add(player.getViewVector(1).scale(reach));
        return clipLoaded(level, player, start, end);
    }

    @SuppressWarnings("ConstantValue")
    static @Nullable BlockHitResult clipLoaded(Level level, Player player, Vec3 start, Vec3 end) {
        if (!level.isLoaded(BlockPos.containing(start))) {
            return null;
        }

        BlockPos unloaded = BlockGetter.traverseBlocks(start, end, level, (world, pos) -> {
            if (world.isLoaded(pos)) {
                return null;
            }

            return pos.immutable();
        }, world -> null);
        Vec3 clipEnd = end;
        if (unloaded != null) {
            Vec3 boundaryEnd = end.add(end.subtract(start).scale(LOADED_BOUNDARY_BACKOFF));
            Vec3 boundary = new AABB(unloaded).clip(start, boundaryEnd).orElse(start);
            clipEnd = boundary.lerp(start, LOADED_BOUNDARY_BACKOFF);
            if (clipEnd.equals(start)) {
                return null;
            }
        }
        BlockHitResult hit = level.clip(new ClipContext(start, clipEnd, Block.OUTLINE, Fluid.NONE, player));
        if (unloaded != null && hit.getType() != Type.BLOCK) {
            return null;
        }

        return hit;
    }

    static boolean isValidTarget(Level level, Player player, BlockPos mirror, BlockPos targetPos, Vec3 target) {
        if (!Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z) || mirror.equals(targetPos) || !level.isLoaded(targetPos) || !new AABB(targetPos).inflate(TARGET_TOLERANCE).contains(target)) {
            return false;
        }

        double distance = Vec3.atCenterOf(mirror).distanceToSqr(target);
        if (distance < AmethystMirrorGeometry.HIT_TOLERANCE || distance > TARGET_RANGE * TARGET_RANGE) {
            return false;
        }

        Vec3 start = player.getEyePosition();
        Vec3 end = target.add(target.subtract(start).normalize().scale(TARGET_TOLERANCE));
        BlockHitResult hit = clipLoaded(level, player, start, end);
        return hit != null && hit.getType() == Type.BLOCK && hit.getBlockPos().equals(targetPos) && hit.getLocation().distanceToSqr(target) <= TARGET_TOLERANCE * TARGET_TOLERANCE;
    }
}
