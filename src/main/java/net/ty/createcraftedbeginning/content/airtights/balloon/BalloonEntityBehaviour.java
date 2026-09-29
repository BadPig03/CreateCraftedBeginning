package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BalloonEntityBehaviour {
    private static final double WATER_HORIZONTAL_DRAG = 0.85;

    private BalloonEntityBehaviour() {
    }

    public static boolean isBalloon(PackageEntity entity) {
        return BalloonItem.isBalloon(entity.getBox());
    }

    public static void tick(PackageEntity entity, BalloonWorldPhysics physics) {
        Level level = entity.level();
        boolean passenger = entity.isPassenger();
        if (!level.isClientSide && entity.isAlive() && !passenger && BalloonItem.containsGas(entity.getBox()) && entity.getBoundingBox().maxY >= level.getMaxBuildHeight()) {
            entity.hurt(entity.damageSources().generic(), Float.MAX_VALUE);
            return;
        }

        boolean submerged = entity.getFluidTypeHeight(Fluids.WATER.getFluidType()) > 0;
        double acceleration = passenger ? 0 : physics.buoyancyAcceleration(submerged);
        if (acceleration > 0) {
            Vec3 movement = entity.getDeltaMovement();
            double drag = submerged ? WATER_HORIZONTAL_DRAG : 1;
            double speed = physics.nextBuoyantVerticalSpeed(movement.y, submerged);
            entity.setDeltaMovement(movement.x * drag, speed, movement.z * drag);
            entity.setOnGround(false);
            entity.hasImpulse = true;
        }
        BalloonGasEffects.renderTrail(entity);
    }

    public static void destroy(PackageEntity entity) {
        if (!isBalloon(entity) || entity.level().isClientSide) {
            return;
        }

        entity.setInvulnerable(true);
        BalloonGasEffects.release(entity);
    }
}
