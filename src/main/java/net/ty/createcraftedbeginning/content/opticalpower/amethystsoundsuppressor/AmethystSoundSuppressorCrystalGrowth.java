package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystCrystalGrowthCandidates.Target;
import net.ty.createcraftedbeginning.content.opticalpower.experiencecrystal.ExperienceCrystalBlock;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystSoundSuppressorCrystalGrowth {
    private static final int GROWTH_RADIUS = 4;
    private static final double PARTICLE_PACKET_RANGE_SQR = 4096;
    private static final double PARTICLE_TIP_HEIGHT = 1.2;
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private AmethystSoundSuppressorCrystalGrowth() {
    }

    static boolean isActive() {
        return ACTIVE.get();
    }

    static AABB bounds(BlockPos center) {
        return new AABB(center).inflate(GROWTH_RADIUS);
    }

    static boolean isInRange(BlockPos center, BlockPos pos) {
        return center.distSqr(pos) <= GROWTH_RADIUS * GROWTH_RADIUS;
    }

    static void tryGrow(ServerLevel level, AmethystSoundSuppressorBlockEntity suppressor) {
        BlockPos pos = suppressor.getBlockPos();
        long gameTime = level.getGameTime();
        if (isActive() || !level.isLoaded(pos) || level.getBlockEntity(pos) != suppressor || !suppressor.canGrowCrystal(gameTime)) {
            return;
        }

        BlockPos center = SubLevelBridge.resolve(level, pos).blockPos();
        Target target = suppressor.growthCandidates().select(level, center);
        if (target == null) {
            return;
        }

        BlockPos targetPos = target.pos();
        ACTIVE.set(true);
        try {
            if (!ExperienceCrystalBlock.tryGrow(level, targetPos, target.facing())) {
                return;
            }

            suppressor.recordCrystalGrowth(gameTime);
            level.gameEvent(GameEvent.BLOCK_CHANGE, targetPos, Context.of(level.getBlockState(targetPos)));
            Vec3 visualCenter = SubLevelBridge.resolve(level, targetPos).worldPosition();
            Vec3 suppressorTip = SubLevelBridge.resolve(level, Vec3.atLowerCornerOf(pos).add(0.5, PARTICLE_TIP_HEIGHT, 0.5)).worldPosition();
            AmethystCrystalGrowthPacket packet = new AmethystCrystalGrowthPacket(visualCenter, suppressorTip, level.dimension().location());
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(visualCenter) > PARTICLE_PACKET_RANGE_SQR) {
                    continue;
                }

                CatnipServices.NETWORK.sendToClient(player, packet);
            }
        }
        finally {
            ACTIVE.remove();
        }
    }

}
