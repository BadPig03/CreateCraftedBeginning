package net.ty.createcraftedbeginning.registry;

import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.particles.ColoredBreezeCloudParticleType;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public enum CCBParticleTypes {
    AIRTIGHT_JETPACK(() -> new SimpleParticleType(false)),
    BREEZE_CLOUD(() -> new SimpleParticleType(false)),
    COLORED_BREEZE_CLOUD(() -> new ColoredBreezeCloudParticleType(false)),
    BUBBLE_WITHOUT_WATER(() -> new SimpleParticleType(false)),
    END_INCINERATION(() -> new SimpleParticleType(false));

    private final ParticleEntry<?> entry;

    CCBParticleTypes(Supplier<? extends ParticleType<?>> typeSupplier) {
        entry = new ParticleEntry<>(Lang.asId(name()), typeSupplier);
    }

    @Internal
    public static void register(IEventBus modEventBus) {
        ParticleEntry.REGISTER.register(modEventBus);
    }

    public ParticleType<?> get() {
        return entry.object.get();
    }

    public ParticleOptions getParticleOptions() {
        return (ParticleOptions) entry.object.get();
    }

    public String parameter() {
        return entry.name;
    }

    private static class ParticleEntry<T extends ParticleType<?>> {
        private static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, CCBAPI.MOD_ID);

        private final String name;
        private final DeferredHolder<ParticleType<?>, T> object;

        public ParticleEntry(String name, Supplier<T> typeSupplier) {
            this.name = name;
            object = REGISTER.register(name, typeSupplier);
        }
    }
}
