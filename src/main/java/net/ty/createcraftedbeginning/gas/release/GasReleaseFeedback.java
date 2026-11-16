package net.ty.createcraftedbeginning.gas.release;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasReleaseFeedback(float pressureScale, float soundVolume, float soundPitch) {
    private static final double LOG_2 = Math.log(2);
    private static final double MAX_PRESSURE_STEPS = 5;
    private static final float PRESSURE_SCALE_PER_STEP = 0.25F;
    private static final float MIN_PRESSURE_SCALE = 0.75F;
    private static final float BASE_SOUND_PITCH = 1.05F;
    private static final float SOUND_PITCH_PER_SCALE = 0.2F;
    private static final float MIN_SOUND_PITCH = 0.8F;

    @Internal
    public GasReleaseFeedback {
        if (!Float.isFinite(pressureScale) || pressureScale < 0) {
            throw new IllegalArgumentException("Gas release pressure scale must be finite and non-negative; got " + pressureScale + '.');
        }

        if (!Float.isFinite(soundVolume) || soundVolume < 0) {
            throw new IllegalArgumentException("Gas release sound volume must be finite and non-negative; got " + soundVolume + '.');
        }

        if (!Float.isFinite(soundPitch) || soundPitch <= 0) {
            throw new IllegalArgumentException("Gas release sound pitch must be finite and positive; got " + soundPitch + '.');
        }
    }

    @Internal
    public static GasReleaseFeedback of(GasReleaseContext context) {
        if (context.sourcePressurePa().isEmpty()) {
            return new GasReleaseFeedback(1, 1, 1);
        }

        double pressureDeltaAtm = (double) context.pressureDeltaPa().orElse(0) / GasPressure.REFERENCE_PRESSURE_PA;
        double pressureSteps = Math.min(MAX_PRESSURE_STEPS, Math.log1p(pressureDeltaAtm) / LOG_2);
        float pressureScale = (float) (MIN_PRESSURE_SCALE + PRESSURE_SCALE_PER_STEP * pressureSteps);
        float soundPitch = Math.max(MIN_SOUND_PITCH, BASE_SOUND_PITCH - SOUND_PITCH_PER_SCALE * (pressureScale - MIN_PRESSURE_SCALE));
        return new GasReleaseFeedback(pressureScale, pressureScale, soundPitch);
    }
}
