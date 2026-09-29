package net.ty.createcraftedbeginning.api.events;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereProvider;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereProviderRegistry;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Registers atmosphere providers during common setup, after built-in providers and
 * handler registration. Subscribe on your own mod event bus using
 * {@code modBus.addListener(...)} or
 * {@code @EventBusSubscriber(modid = "your_mod", bus = EventBusSubscriber.Bus.MOD)}.
 * Register directly in the synchronous callback; providers use their declared priority.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class RegisterAtmosphereProvidersEvent extends Event implements IModBusEvent {
    public void register(ResourceLocation id, AtmosphereProvider provider) {
        AtmosphereProviderRegistry.register(id, provider);
    }

    public void register(ResourceLocation id, int priority, AtmosphereProvider provider) {
        AtmosphereProviderRegistry.register(id, priority, provider);
    }
}
