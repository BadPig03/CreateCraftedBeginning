package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereProvider;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereProviderRegistry;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasRegistries;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AtmosphereProviderEvent implements KubeEvent {
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    public void add(ResourceLocation id, CompositionHandler handler) {
        add(id, AtmosphereProviderRegistry.DEFAULT_PRIORITY, handler);
    }

    public void add(ResourceLocation id, int priority, CompositionHandler handler) {
        AtmosphereProviderRegistry.register(id, priority, new AtmosphereProvider() {
            @Override
            public @Nullable Gas resolveComposition(Level level, BlockPos pos) {
                if (!level.isLoaded(pos)) {
                    return null;
                }

                ResourceLocation gasId = handler.apply(level, pos);
                if (gasId == null) {
                    return null;
                }

                return GasRegistries.GAS_REGISTRY.getOptional(gasId).orElseThrow(() -> new IllegalArgumentException("Atmosphere provider '" + id + "' must return a registered gas ID; got '" + gasId + "'."));
            }
        });
    }

    public void addPressure(ResourceLocation id, PressureHandler handler) {
        addPressure(id, AtmosphereProviderRegistry.DEFAULT_PRIORITY, handler);
    }

    public void addPressure(ResourceLocation id, int priority, PressureHandler handler) {
        AtmosphereProviderRegistry.register(id, priority, new AtmosphereProvider() {
            @Override
            public OptionalLong resolvePressurePa(Level level, BlockPos pos) {
                if (!level.isLoaded(pos)) {
                    return OptionalLong.empty();
                }

                Object result = handler.apply(level, pos);
                if (result == null) {
                    return OptionalLong.empty();
                }

                if (!(result instanceof Number number)) {
                    throw new IllegalArgumentException("Atmosphere provider '" + id + "' must return a non-negative safe integer pressure in Pa or null; got " + result + '.');
                }

                double pressurePa = number.doubleValue();
                if (!Double.isFinite(pressurePa) || pressurePa < 0 || pressurePa > MAX_SAFE_INTEGER || pressurePa != Math.rint(pressurePa)) {
                    throw new IllegalArgumentException("Atmosphere provider '" + id + "' must return a finite integer pressure between 0 and " + MAX_SAFE_INTEGER + " Pa; got " + result + '.');
                }

                return OptionalLong.of((long) pressurePa);
            }
        });
    }

    @FunctionalInterface
    public interface CompositionHandler {
        @Nullable ResourceLocation apply(Level level, BlockPos pos);
    }

    @FunctionalInterface
    public interface PressureHandler {
        @Nullable Object apply(Level level, BlockPos pos);
    }
}
