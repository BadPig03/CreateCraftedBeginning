package net.ty.createcraftedbeginning.gas.flow;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.gas.release.GasReleasePushPacket;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import net.ty.createcraftedbeginning.platform.access.PlayerFlightCheckAccess;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasFlowTarget {
    private Vec3 continuous = Vec3.ZERO;
    private Vec3 burst = Vec3.ZERO;

    void add(Vec3 impulse, boolean sustained) {
        if (sustained) {
            continuous = continuous.add(impulse);
            return;
        }

        burst = burst.add(impulse);
    }

    void apply(Entity entity) {
        if (!entity.isAlive() || entity.isSpectator()) {
            return;
        }

        CoordinateTransform transform = SubLevelBridge.createEntityTransform(entity);
        GasFlowMotion motion = new GasFlowMotion(continuous, burst);
        Vec3 nativeMovement = entity.getDeltaMovement();
        if (entity instanceof ServerPlayer player) {
            nativeMovement = player.getKnownMovement();
        }

        Vec3 movement = transform.transformNormal(nativeMovement);
        Vec3 impulse = motion.impulseFor(movement);
        if (impulse.lengthSqr() <= 0 && !(entity instanceof ServerPlayer)) {
            return;
        }

        Vec3 nextMovement = movement.add(impulse);
        if (entity instanceof ServerPlayer player) {
            CatnipServices.NETWORK.sendToClient(player, new GasReleasePushPacket(player.level().dimension().location(), continuous, burst));
            player.serverLevel().getChunkSource().broadcast(player, new ClientboundSetEntityMotionPacket(player.getId(), transform.inverseTransformNormal(nextMovement)));
        }
        else {
            entity.setDeltaMovement(transform.inverseTransformNormal(nextMovement));
            entity.hasImpulse = true;
            entity.hurtMarked = true;
            if (entity instanceof ItemEntity item) {
                GasFlowForces.synchronizePosition(item);
            }
        }
        if (impulse.y > 0 && nextMovement.y > 0) {
            entity.setOnGround(false);
        }

        if (!motion.isUpdraft(impulse)) {
            return;
        }

        if (nextMovement.y >= 0) {
            entity.fallDistance = 0;
        }

        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        ((PlayerFlightCheckAccess) player.connection).ccb$setAboveGroundTickCount(0);
    }
}
