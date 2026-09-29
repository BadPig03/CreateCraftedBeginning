package net.ty.createcraftedbeginning.gas.release;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandler;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandlers;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasReleaseService {
    public static final long EFFECT_INTERVAL = 1000;

    private GasReleaseService() {
    }

    public static GasReleaseResult release(Level level, GasReleaseRequest request) {
        GasStack gas = request.gas();
        GasReleaseContext context = createContext(level, request, gas);
        if (gas.isEmpty()) {
            return new GasReleaseResult(context, false, false);
        }

        GasReleaseHandler releaseHandler = GasReleaseHandlers.resolve(gas, context.sourcePressurePa().orElse(GasPressure.REFERENCE_PRESSURE_PA));
        GasReleaseFeedback feedback = GasReleaseFeedback.of(context);
        boolean effectDue = gas.getAmount() >= EFFECT_INTERVAL;
        applyFeedback(context, releaseHandler, feedback, false);
        if (effectDue) {
            releaseHandler.apply(context);
        }

        return new GasReleaseResult(context, true, effectDue);
    }

    public static GasReleaseResult release(Level level, GasReleaseRequest request, GasReleaseState state) {
        GasStack gas = request.gas();
        GasReleaseContext context = createContext(level, request, gas);
        if (gas.isEmpty()) {
            return new GasReleaseResult(context, false, false);
        }

        long gameTime = level.getGameTime();
        long sourcePressurePa = context.sourcePressurePa().orElse(GasPressure.REFERENCE_PRESSURE_PA);
        boolean feedbackDue = state.recordFeedback(gameTime);
        boolean effectDue = state.recordEffect(gas, sourcePressurePa, gas.getAmount(), gameTime);
        GasReleaseHandler releaseHandler = GasReleaseHandlers.resolve(gas, sourcePressurePa);
        GasReleaseFeedback feedback = GasReleaseFeedback.of(context);
        if (feedbackDue) {
            applyFeedback(context, releaseHandler, feedback, true);
        }

        if (effectDue) {
            releaseHandler.apply(context);
        }

        return new GasReleaseResult(context, feedbackDue, effectDue);
    }

    @Internal
    public static GasReleaseContext createContext(Level level, GasReleaseRequest request) {
        return createContext(level, request, request.gas());
    }

    private static GasReleaseContext createContext(Level level, GasReleaseRequest request, GasStack gas) {
        return new GasReleaseContext(level, gas, request.sourcePos(), request.effectPos(), request.direction(), request.cause(), request.mode(), request.sourcePressurePa(), AtmosphereStateResolver.resolve(level, request.effectPos()).pressurePa());
    }

    private static void applyFeedback(GasReleaseContext context, GasReleaseHandler releaseHandler, GasReleaseFeedback feedback, boolean continuous) {
        Level level = context.level();
        if (releaseHandler.shouldShowOutline()) {
            GasReleaseHandlers.showOutline(level, context.effectPos(), feedback.outlineInflation(), context.gas().getGasType().getTint());
        }

        if (continuous && level.getGameTime() % 20 != 10) {
            return;
        }

        CCBSoundEvents.GAS_DRAINAGE.playOnServer(level, context.sourcePos(), feedback.soundVolume(), feedback.soundPitch());
    }
}
