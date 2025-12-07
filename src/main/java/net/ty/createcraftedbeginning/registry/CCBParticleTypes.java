package net.ty.createcraftedbeginning.registry;

import net.createmod.catnip.lang.Lang;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.particles.AirtightJetpackParticle;
import net.ty.createcraftedbeginning.content.particles.BreezeCloudParticle;
import net.ty.createcraftedbeginning.content.particles.BubbleWithoutWaterParticle.Provider;
import net.ty.createcraftedbeginning.content.particles.CompressedAirIntakeParticle;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public enum CCBParticleTypes {
    COMPRESSED_AIR_INTAKE(() -> new SimpleParticleType(false)),
    AIRTIGHT_JETPACK(() -> new SimpleParticleType(false)),
    BREEZE_CLOUD(() -> new SimpleParticleType(false)),
    BUBBLE_WITHOUT_WATER(() -> new SimpleParticleType(false));

    private final ParticleEntry<?> entry;

    CCBParticleTypes(Supplier<SimpleParticleType> typeSupplier) {
        entry = new ParticleEntry<>(Lang.asId(name()), typeSupplier);
    }

    @Internal
    public static void register(IEventBus modEventBus) {
        ParticleEntry.REGISTER.register(modEventBus);
    }

    public static void registerFactories(@NotNull RegisterParticleProvidersEvent event) {
        event.registerSpriteSet((SimpleParticleType) COMPRESSED_AIR_INTAKE.get(), CompressedAirIntakeParticle.Provider::new);
        event.registerSpriteSet((SimpleParticleType) AIRTIGHT_JETPACK.get(), AirtightJetpackParticle.Provider::new);
        event.registerSpriteSet((SimpleParticleType) BREEZE_CLOUD.get(), BreezeCloudParticle.Provider::new);
        event.registerSpriteSet((SimpleParticleType) BUBBLE_WITHOUT_WATER.get(), Provider::new);
    }

    public @NotNull ParticleType<?> get() {
        return entry.object.get();
    }

    public @NotNull ParticleOptions getParticleOptions() {
        return (ParticleOptions) entry.object.get();
    }

    public String parameter() {
        return entry.name;
    }

    private static class ParticleEntry<T extends ParticleType<?>> {
        private static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, CreateCraftedBeginning.MOD_ID);

        private final String name;
        private final DeferredHolder<ParticleType<?>, T> object;

        public ParticleEntry(String name, Supplier<T> typeSupplier) {
            this.name = name;
            object = REGISTER.register(name, typeSupplier);
        }
    }
}
