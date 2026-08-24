package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class EndSculkSilencerAnimationState {
    private static final float MAX_ANIMATION_SPEED = 40;

    private final LerpedFloat animationSpeed = LerpedFloat.linear().startWithValue(0);
    private final LerpedFloat animation = LerpedFloat.angular().startWithValue(0);

    static float calculateTargetSpeed(float kineticSpeed) {
        float absoluteSpeed = Mth.abs(kineticSpeed);
        if (absoluteSpeed == 0) {
            return 0;
        }

        float unclampedTargetSpeed = Math.signum(kineticSpeed) * 2 * Mth.ceil(Math.log10(absoluteSpeed) + Math.sqrt(absoluteSpeed));
        return Mth.clamp(unclampedTargetSpeed, -MAX_ANIMATION_SPEED, MAX_ANIMATION_SPEED);
    }

    LerpedFloat getAnimation() {
        return animation;
    }

    void tick(boolean speedRequirementFulfilled, float kineticSpeed) {
        if (speedRequirementFulfilled) {
            animationSpeed.chase(calculateTargetSpeed(kineticSpeed), 0.1, Chaser.EXP);
        }
        else {
            animationSpeed.chase(0, 0.2, Chaser.EXP);
        }

        animationSpeed.tickChaser();
        animation.setValue(animation.getValue() + animationSpeed.getValue());
    }
}
