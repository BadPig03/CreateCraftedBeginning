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

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AtmosphereProviderEvent implements KubeEvent {
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

    @FunctionalInterface
    public interface CompositionHandler {
        @Nullable ResourceLocation apply(Level level, BlockPos pos);
    }
}
