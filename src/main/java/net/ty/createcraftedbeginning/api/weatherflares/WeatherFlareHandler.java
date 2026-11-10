package net.ty.createcraftedbeginning.api.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface WeatherFlareHandler {
    /** Read-only check, called at both the muzzle and the effect position. */
    boolean supports(ServerLevel level, Vec3 pos, WeatherFlareEffect effect);

    /**
     * Runs synchronously on the server. Refreshing an existing weather state counts as applied.
     * Rejection must leave weather unchanged. Restore owned state before propagating an exception.
     */
    WeatherFlareResult apply(WeatherFlareContext context);
}
