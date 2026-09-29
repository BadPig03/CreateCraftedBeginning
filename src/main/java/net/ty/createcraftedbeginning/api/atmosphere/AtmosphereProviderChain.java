package net.ty.createcraftedbeginning.api.atmosphere;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AtmosphereProviderChain {
    private static final Comparator<RegisteredProvider> RESOLUTION_ORDER = Comparator.comparingInt(RegisteredProvider::priority).reversed().thenComparing(registered -> registered.id().toString());
    private static final AtmosphereProviderChain EMPTY = new AtmosphereProviderChain(List.of());

    private final List<RegisteredProvider> providers;

    private AtmosphereProviderChain(List<RegisteredProvider> providers) {
        this.providers = providers;
    }

    static AtmosphereProviderChain empty() {
        return EMPTY;
    }

    AtmosphereProviderChain withProvider(ResourceLocation id, int priority, AtmosphereProvider provider) {
        List<RegisteredProvider> updated = new ArrayList<>(providers.size() + 1);
        for (RegisteredProvider registered : providers) {
            if (registered.id().equals(id)) {
                throw new IllegalStateException("Atmosphere provider id '" + id + "' is already registered.");
            }

            updated.add(registered);
        }
        updated.add(new RegisteredProvider(id, priority, provider));
        updated.sort(RESOLUTION_ORDER);
        return new AtmosphereProviderChain(List.copyOf(updated));
    }

    @Nullable Gas resolveComposition(Level level, BlockPos pos, FailureHandler failureHandler) {
        for (RegisteredProvider registered : providers) {
            try {
                Gas gas = registered.provider().resolveComposition(level, pos);
                if (gas != null) {
                    return gas;
                }
            }
            catch (RuntimeException exception) {
                failureHandler.onFailure(registered.id(), level, "composition", exception);
            }
        }
        return null;
    }

    OptionalLong resolvePressurePa(Level level, BlockPos pos, FailureHandler failureHandler) {
        for (RegisteredProvider registered : providers) {
            try {
                OptionalLong pressurePa = registered.provider().resolvePressurePa(level, pos);
                if (pressurePa.isEmpty()) {
                    continue;
                }

                long value = pressurePa.getAsLong();
                if (value < GasPressure.VACUUM_PA) {
                    failureHandler.onFailure(registered.id(), level, "pressure", new IllegalArgumentException("Atmospheric pressure must be non-negative; got " + value + " Pa."));
                    continue;
                }

                return OptionalLong.of(value);
            }
            catch (RuntimeException exception) {
                failureHandler.onFailure(registered.id(), level, "pressure", exception);
            }
        }
        return OptionalLong.empty();
    }

    @FunctionalInterface
    interface FailureHandler {
        void onFailure(ResourceLocation id, Level level, String queryType, RuntimeException exception);
    }

    private record RegisteredProvider(ResourceLocation id, int priority, AtmosphereProvider provider) {}
}
