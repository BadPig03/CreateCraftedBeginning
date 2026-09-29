package net.ty.createcraftedbeginning.api.events;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Registers gameplay pressure profiles during common setup, after game registries are available.
 * Subscribe on your mod event bus using {@code modBus.addListener(...)} or
 * {@code @EventBusSubscriber(modid = "your_mod", bus = EventBusSubscriber.Bus.MOD)}.
 * Listeners run synchronously; profile registration closes after all listeners return,
 * before built-in handlers and the other registration events run.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class RegisterGameplayPressureProfilesEvent extends Event implements IModBusEvent {
    public GameplayPressureProfile register(ResourceLocation id, long minimumPressurePa) {
        return GameplayPressureProfiles.register(id, minimumPressurePa);
    }
}
