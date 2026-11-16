package net.ty.createcraftedbeginning.gas.flow;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasFlowMotion(Vec3 continuous, Vec3 burst) {
    private static final double MAX_ACCELERATION = 0.12;
    private static final double CONTINUOUS_SPEED_LIMIT = 0.35;
    private static final double MAX_IMPULSE = 0.8;

    public Vec3 impulseFor(Vec3 movement) {
        Vec3 continuousImpulse = continuous;
        double continuousLength = continuousImpulse.length();
        if (continuousLength > 0) {
            Vec3 direction = continuousImpulse.scale(1 / continuousLength);
            double allowance = Math.max(0, CONTINUOUS_SPEED_LIMIT - movement.dot(direction));
            continuousImpulse = direction.scale(Math.min(continuousLength, Math.min(MAX_ACCELERATION, allowance)));
        }

        Vec3 burstImpulse = burst;
        double burstLength = burstImpulse.length();
        if (burstLength > MAX_IMPULSE) {
            burstImpulse = burstImpulse.scale(MAX_IMPULSE / burstLength);
        }

        Vec3 impulse = continuousImpulse.add(burstImpulse);
        double limit = burstLength > 0 ? MAX_IMPULSE : MAX_ACCELERATION;
        double impulseLength = impulse.length();
        if (impulseLength <= 0) {
            return Vec3.ZERO;
        }

        if (impulseLength > limit) {
            return impulse.scale(limit / impulseLength);
        }

        return impulse;
    }

    public boolean isUpdraft(Vec3 impulse) {
        return continuous.y > 0 && impulse.y > 0;
    }
}
