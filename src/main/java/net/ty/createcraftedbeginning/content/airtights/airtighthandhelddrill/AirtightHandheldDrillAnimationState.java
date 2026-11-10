package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class AirtightHandheldDrillAnimationState {

    private final LerpedFloat speedProgress = LerpedFloat.linear().startWithValue(0);
    private final LerpedFloat rotationAngle = LerpedFloat.angular().startWithValue(0);

    public void tick(boolean rotating) {
        float targetProgress = 0;
        if (rotating) {
            targetProgress = 1;
        }
        speedProgress.chase(targetProgress, 0.1, Chaser.LINEAR);
        speedProgress.tickChaser();
        rotationAngle.setValue((rotationAngle.getValue() + getRotationSpeed()) % 360);
    }

    public void freeze(float partialTicks) {
        rotationAngle.startWithValue(getRotationAngle(partialTicks));
    }

    public void reset() {
        speedProgress.startWithValue(0);
        rotationAngle.startWithValue(0);
    }

    public float getRotationAngle(float partialTicks) {
        return Mth.positiveModulo(rotationAngle.getValue(partialTicks), 360);
    }

    public double getRotationSpeed() {
        float progress = speedProgress.getValue();
        if (progress <= 0) {
            return 0;
        }

        return 60 * Math.pow(1200, progress - 1);
    }
}
