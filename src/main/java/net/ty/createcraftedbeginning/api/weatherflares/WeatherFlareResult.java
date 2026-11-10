package net.ty.createcraftedbeginning.api.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

/** Outcomes must describe the handler's actual weather system, not unrelated vanilla weather data. */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record WeatherFlareResult(boolean applied, boolean clearedStorm, boolean anchoredStorm) {
    public static final WeatherFlareResult REJECTED = new WeatherFlareResult(false, false, false);
    public static final WeatherFlareResult APPLIED = new WeatherFlareResult(true, false, false);

    public WeatherFlareResult {
        if (!applied && (clearedStorm || anchoredStorm)) {
            throw new IllegalArgumentException("Rejected weather flare results cannot grant weather outcomes.");
        }
    }
}
