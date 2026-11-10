package net.ty.createcraftedbeginning.api.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class VanillaWeatherFlareHandler implements WeatherFlareHandler {
    public static final VanillaWeatherFlareHandler INSTANCE = new VanillaWeatherFlareHandler();

    private VanillaWeatherFlareHandler() {
    }

    /** Preserves the unrestricted legacy callback path and its vanilla weather rollback. */
    public static WeatherFlareResult applyDirect(ServerLevel level, ItemStack flareStack, double ratio) {
        if (!(flareStack.getItem() instanceof WeatherFlare flare)) {
            return WeatherFlareResult.REJECTED;
        }

        ServerLevelData data = (ServerLevelData) level.getLevelData();
        int clearTime = data.getClearWeatherTime();
        int rainTime = data.getRainTime();
        int thunderTime = data.getThunderTime();
        boolean wasRaining = data.isRaining();
        boolean wasThundering = data.isThundering();
        boolean weatherCycle = level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE);
        try {
            flare.setWeather(level, flareStack.copyWithCount(1), ratio);
        }
        catch (RuntimeException | Error failure) {
            try {
                data.setClearWeatherTime(clearTime);
                data.setRainTime(rainTime);
                data.setThunderTime(thunderTime);
                data.setRaining(wasRaining);
                data.setThundering(wasThundering);
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(weatherCycle, level.getServer());
            }
            catch (RuntimeException | Error restoreFailure) {
                if (restoreFailure != failure) {
                    failure.addSuppressed(restoreFailure);
                }
            }
            throw failure;
        }

        boolean clearedStorm = (wasRaining || wasThundering) && !data.isRaining() && !data.isThundering();
        DimensionType dimensionType = level.dimensionType();
        boolean anchoredStorm = dimensionType.hasSkyLight() && !dimensionType.hasCeiling() && data.isRaining() && data.isThundering() && !level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE);
        return new WeatherFlareResult(true, clearedStorm, anchoredStorm);
    }

    @Override
    public boolean supports(ServerLevel level, Vec3 pos, WeatherFlareEffect effect) {
        return level.dimension() == Level.OVERWORLD;
    }

    @Override
    public WeatherFlareResult apply(WeatherFlareContext context) {
        if (!supports(context.level(), context.pos(), context.effect())) {
            return WeatherFlareResult.REJECTED;
        }

        return applyDirect(context.level(), context.flareStack(), context.durationRatio());
    }
}
