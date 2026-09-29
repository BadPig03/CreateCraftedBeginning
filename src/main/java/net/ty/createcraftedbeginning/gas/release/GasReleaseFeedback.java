package net.ty.createcraftedbeginning.gas.release;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasReleaseFeedback(float pressureScale, float outlineInflation, float soundVolume, float soundPitch) {
    private static final float MIN_PRESSURE_SCALE = 0.75F;
    private static final float BASE_SOUND_PITCH = 1.05F;
    private static final float SOUND_PITCH_PER_SCALE = 0.2F;
    private static final float MIN_SOUND_PITCH = 0.8F;

    @Internal
    public GasReleaseFeedback {
        if (!Float.isFinite(pressureScale) || pressureScale < 0) {
            throw new IllegalArgumentException("Gas release pressure scale must be finite and non-negative; got " + pressureScale + '.');
        }

        if (!Float.isFinite(outlineInflation) || outlineInflation < 0) {
            throw new IllegalArgumentException("Gas release outline inflation must be finite and non-negative; got " + outlineInflation + '.');
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
        float pressureScale = context.effectInflation();
        if (context.sourcePressurePa().isEmpty()) {
            return new GasReleaseFeedback(pressureScale, pressureScale, 1, 1);
        }

        float soundPitch = Math.max(MIN_SOUND_PITCH, BASE_SOUND_PITCH - SOUND_PITCH_PER_SCALE * (pressureScale - MIN_PRESSURE_SCALE));
        return new GasReleaseFeedback(pressureScale, pressureScale, pressureScale, soundPitch);
    }
}
