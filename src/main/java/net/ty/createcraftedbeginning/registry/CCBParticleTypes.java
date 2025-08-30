package net.ty.createcraftedbeginning.registry;

import net.createmod.catnip.lang.Lang;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.compressedair.CompressedAirIntakeParticle;
import net.ty.createcraftedbeginning.content.compressedair.CompressedAirLeakParticle;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public enum CCBParticleTypes {
    COMPRESSED_AIR_INTAKE(() -> new SimpleParticleType(false)),
    COMPRESSED_AIR_LEAK(() -> new SimpleParticleType(false));

    private final ParticleEntry<?> entry;

    CCBParticleTypes(Supplier<SimpleParticleType> typeSupplier) {
        String name = Lang.asId(name());
        entry = new ParticleEntry<>(name, typeSupplier);
    }

    @ApiStatus.Internal
    public static void register(IEventBus modEventBus) {
        ParticleEntry.REGISTER.register(modEventBus);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet((SimpleParticleType) COMPRESSED_AIR_INTAKE.get(), CompressedAirIntakeParticle.Provider::new);
        event.registerSpriteSet((SimpleParticleType) COMPRESSED_AIR_LEAK.get(), CompressedAirLeakParticle.Provider::new);
    }

    public ParticleType<?> get() {
        return entry.object.get();
    }

    public String parameter() {
        return entry.name;
    }

    private static class ParticleEntry<T extends ParticleType<?>> {
        private static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, CreateCraftedBeginning.MOD_ID);

        private final String name;
        private final Supplier<T> typeSupplier;
        private final DeferredHolder<ParticleType<?>, T> object;

        public ParticleEntry(String name, Supplier<T> typeSupplier) {
            this.name = name;
            this.typeSupplier = typeSupplier;
            object = REGISTER.register(name, typeSupplier);
        }
    }
}
