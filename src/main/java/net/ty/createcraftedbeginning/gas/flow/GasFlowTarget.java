package net.ty.createcraftedbeginning.gas.flow;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
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
    private Vec3 weightedIntakeMovement = Vec3.ZERO;
    private double intakeStrength;

    static boolean canAffect(Entity entity) {
        return entity.isAlive() && !entity.isSpectator() && !(entity instanceof ArmorStand) && (!(entity instanceof Player player) || !player.isCreative() || !player.getAbilities().flying) && (entity instanceof LivingEntity || entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof Boat || entity instanceof AbstractMinecart);
    }

    void add(Vec3 impulse, boolean sustained) {
        if (sustained) {
            continuous = continuous.add(impulse);
            return;
        }

        burst = burst.add(impulse);
    }

    void addItemIntake(Vec3 desired, double strength) {
        weightedIntakeMovement = weightedIntakeMovement.add(desired.scale(strength));
        intakeStrength += strength;
    }

    void apply(Entity entity) {
        if (!entity.isAlive() || entity.isSpectator()) {
            return;
        }

        CoordinateTransform transform = SubLevelBridge.createEntityTransform(entity);
        Vec3 nativeMovement = entity.getDeltaMovement();
        if (entity instanceof ServerPlayer player) {
            nativeMovement = player.getKnownMovement();
        }

        Vec3 movement = transform.transformNormal(nativeMovement);
        Vec3 combinedContinuous = continuous;
        if (entity instanceof ItemEntity item && intakeStrength > 0) {
            Vec3 desired = weightedIntakeMovement.scale(1 / intakeStrength);
            Vec3 gravity = transform.transformNormal(new Vec3(0, item.getGravity(), 0));
            Vec3 intake = desired.add(gravity).subtract(movement);
            double length = intake.length();
            if (length > intakeStrength) {
                intake = intake.scale(intakeStrength / length);
            }

            combinedContinuous = combinedContinuous.add(intake);
        }

        GasFlowMotion motion = new GasFlowMotion(combinedContinuous, burst);
        Vec3 impulse = motion.impulseFor(movement);
        if (impulse.lengthSqr() <= 0 && !(entity instanceof ServerPlayer)) {
            return;
        }

        Vec3 nextMovement = movement.add(impulse);
        if (entity instanceof ServerPlayer player) {
            CatnipServices.NETWORK.sendToClient(player, new GasReleasePushPacket(player.level().dimension().location(), combinedContinuous, burst));
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
