package net.ty.createcraftedbeginning.compat.kubejs;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Function;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.weatherflares.VanillaWeatherFlareHandler;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareContext;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareEffect;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareHandler;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareResult;
import net.ty.createcraftedbeginning.compat.kubejs.events.WeatherFlareHandlerEvent.Apply;
import net.ty.createcraftedbeginning.compat.kubejs.events.WeatherFlareHandlerEvent.CanApply;
import net.ty.createcraftedbeginning.compat.kubejs.events.WeatherFlareHandlerEvent.Capture;
import net.ty.createcraftedbeginning.compat.kubejs.events.WeatherFlareHandlerEvent.Restore;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class KubeJSWeatherFlareHandler implements WeatherFlareHandler {
    private final ResourceLocation dimension;
    private final EnumSet<WeatherFlareEffect> effects;
    @Nullable
    private final CanApply canApply;
    @Nullable
    private final Operation operation;

    public KubeJSWeatherFlareHandler(Context context, ResourceLocation dimension, @Nullable Object options) {
        if (!(options instanceof Map<?, ?> fields)) {
            throw new IllegalArgumentException("Weather flare handler options must be an object.");
        }

        for (Object key : fields.keySet()) {
            switch (String.valueOf(key)) {
                case "effects", "canApply", "capture", "apply", "restore" -> {}
                default -> throw new IllegalArgumentException("Unknown weather flare handler field '" + key + "'.");
            }
        }

        this.dimension = dimension;
        effects = readEffects(fields.get("effects"));
        canApply = readCallback(context, fields, "canApply", CanApply.class);
        Capture capture = readCallback(context, fields, "capture", Capture.class);
        Apply apply = readCallback(context, fields, "apply", Apply.class);
        Restore restore = readCallback(context, fields, "restore", Restore.class);
        if (capture == null && apply == null && restore == null) {
            if (!dimension.equals(Level.OVERWORLD.location())) {
                throw new IllegalArgumentException("Weather flare handlers outside minecraft:overworld require capture, apply and restore callbacks.");
            }

            operation = null;
            return;
        }

        if (capture == null || apply == null || restore == null) {
            throw new IllegalArgumentException("Weather flare handler fields 'capture', 'apply' and 'restore' must be supplied together.");
        }

        operation = new Operation(capture, apply, restore);
    }

    private static EnumSet<WeatherFlareEffect> readEffects(@Nullable Object value) {
        if (!(value instanceof List<?> choices) || choices.isEmpty()) {
            throw new IllegalArgumentException("Weather flare handler field 'effects' must be a non-empty array of effect names.");
        }

        EnumSet<WeatherFlareEffect> effects = EnumSet.noneOf(WeatherFlareEffect.class);
        for (Object choice : choices) {
            if (!(choice instanceof CharSequence name)) {
                throw new IllegalArgumentException("Weather flare effect must be a name string; got " + choice + '.');
            }

            WeatherFlareEffect effect;
            try {
                effect = WeatherFlareEffect.valueOf(name.toString());
            }
            catch (IllegalArgumentException failure) {
                throw new IllegalArgumentException("Unknown weather flare effect '" + name + "'; expected CLEAR, RAIN, THUNDER or TOGGLE_CYCLE.", failure);
            }
            if (!effects.add(effect)) {
                throw new IllegalArgumentException("Duplicate weather flare effect '" + name + "'.");
            }
        }
        return effects;
    }

    @Nullable
    private static <T> T readCallback(Context context, Map<?, ?> fields, String name, Class<T> type) {
        if (!fields.containsKey(name)) {
            return null;
        }

        Object callback = fields.get(name);
        if (!(callback instanceof Function)) {
            throw new IllegalArgumentException("Weather flare handler field '" + name + "' must be a function.");
        }

        return type.cast(context.jsToJava(callback, TypeInfo.of(type)));
    }

    private static WeatherFlareResult readResult(@Nullable Object value) {
        if (!(value instanceof Map<?, ?> fields)) {
            throw new IllegalArgumentException("Weather flare apply callback must return an outcome object; got " + value + '.');
        }

        for (Object key : fields.keySet()) {
            switch (String.valueOf(key)) {
                case "applied", "clearedStorm", "anchoredStorm" -> {}
                default -> throw new IllegalArgumentException("Unknown weather flare result field '" + key + "'.");
            }
        }

        boolean applied = readBoolean(fields.get("applied"), "applied");
        boolean clearedStorm = fields.containsKey("clearedStorm") && readBoolean(fields.get("clearedStorm"), "clearedStorm");
        boolean anchoredStorm = fields.containsKey("anchoredStorm") && readBoolean(fields.get("anchoredStorm"), "anchoredStorm");
        return new WeatherFlareResult(applied, clearedStorm, anchoredStorm);
    }

    private static boolean readBoolean(@Nullable Object value, String name) {
        if (!(value instanceof Boolean flag)) {
            throw new IllegalArgumentException("Weather flare '" + name + "' must return a boolean; got " + value + '.');
        }

        return flag;
    }

    @Override
    public boolean supports(ServerLevel level, Vec3 pos, WeatherFlareEffect effect) {
        if (!dimension.equals(level.dimension().location()) || !effects.contains(effect)) {
            return false;
        }

        if (canApply == null) {
            return true;
        }

        return readBoolean(canApply.test(level, pos, effect), "canApply");
    }

    @Override
    public WeatherFlareResult apply(WeatherFlareContext context) {
        if (operation == null) {
            return VanillaWeatherFlareHandler.INSTANCE.apply(context);
        }

        Object snapshot = operation.capture().capture(context);
        WeatherFlareResult result;
        try {
            result = readResult(operation.apply().apply(context, snapshot));
        }
        catch (RuntimeException | Error failure) {
            try {
                operation.restore().restore(context, snapshot);
            }
            catch (RuntimeException | Error restoreFailure) {
                if (restoreFailure != failure) {
                    failure.addSuppressed(restoreFailure);
                }
            }
            throw failure;
        }
        if (!result.applied()) {
            operation.restore().restore(context, snapshot);
        }

        return result;
    }

    private record Operation(Capture capture, Apply apply, Restore restore) {
    }
}
