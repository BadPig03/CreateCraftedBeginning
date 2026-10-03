package net.ty.createcraftedbeginning.content.airtights.airvents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.access.AirVentCrawlingAccess;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirVentTraversal {
    private static final double ENTRY_SEARCH_RADIUS = 3;
    private static final double CRAWLING_CORE_INSET_RATIO = 0.25;

    private AirVentTraversal() {
    }

    public static boolean shouldCrawl(Player player) {
        return shouldCrawl(player, false);
    }

    public static boolean shouldCrawl(Player player, boolean wasVentCrawling) {
        if (player.isSpectator() || !player.isAlive() || player.isPassenger() || player.isSleeping() || player.isFallFlying() || player.isAutoSpinAttack() || player.getForcedPose() != null) {
            return false;
        }

        if (isInside(player)) {
            return true;
        }

        if (wasVentCrawling && intersectsInterior(player, player.getDimensions(Pose.SWIMMING).makeBoundingBox(player.position()))) {
            return true;
        }

        if (!player.isShiftKeyDown()) {
            return false;
        }

        Level level = player.level();
        return SubLevelBridge.testLocalFrames(level, player.position(), player.getLookAngle(), ENTRY_SEARCH_RADIUS, (position, direction) -> {
            Direction lookDirection = Direction.getNearest(direction.x, direction.y, direction.z);
            BlockPos playerPos = BlockPos.containing(position);
            return canEnterFrom(level, playerPos.relative(lookDirection), lookDirection) || lookDirection == Direction.UP && canEnterFrom(level, playerPos.above(2), lookDirection);
        });
    }

    public static boolean shouldPreserveLocalCrawling(Player player) {
        if (!player.isLocalPlayer()) {
            return false;
        }

        Pose receivedPose = player.getPose();
        if (receivedPose != Pose.STANDING && receivedPose != Pose.CROUCHING) {
            return false;
        }

        boolean wasVentCrawling = ((AirVentCrawlingAccess) player).ccb$isVentCrawling();
        return wasVentCrawling && shouldCrawl(player, true);
    }

    static boolean isInside(Player player) {
        EntityDimensions dimensions = player.getDimensions(Pose.SWIMMING);
        AABB bounds = dimensions.makeBoundingBox(player.position()).deflate(0, dimensions.height() * CRAWLING_CORE_INSET_RATIO, 0);
        return intersectsInterior(player, bounds);
    }

    private static boolean intersectsInterior(Player player, AABB bounds) {
        Level level = player.level();
        return SubLevelBridge.testLocalBounds(level, bounds, localBounds -> {
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(localBounds.minX, localBounds.minY, localBounds.minZ), BlockPos.containing(localBounds.maxX, localBounds.maxY, localBounds.maxZ))) {
                if (!level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof AirVentBlock)) {
                    continue;
                }

                if (!SubLevelBridge.createEntityArea(level, pos, new AABB(pos).deflate(AirVentVoxelShapes.THICKNESS)).intersects(player, bounds)) {
                    continue;
                }

                return true;
            }
            return false;
        });
    }

    private static boolean canEnterFrom(Level level, BlockPos ventPos, Direction entryDirection) {
        if (!level.isLoaded(ventPos)) {
            return false;
        }

        BlockState ventState = level.getBlockState(ventPos);
        return ventState.getBlock() instanceof AirVentBlock && AirVentBlock.canPassThrough(ventState, level, ventPos, entryDirection.getOpposite());
    }
}
