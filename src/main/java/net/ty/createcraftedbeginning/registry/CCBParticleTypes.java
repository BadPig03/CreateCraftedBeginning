package net.ty.createcraftedbeginning.registry;

import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.particles.AirtightJetpackParticle.Provider;
import net.ty.createcraftedbeginning.content.particles.BreezeCloudParticle;
import net.ty.createcraftedbeginning.content.particles.BreezeCloudParticle.ColoredProvider;
import net.ty.createcraftedbeginning.content.particles.BubbleWithoutWaterParticle;
import net.ty.createcraftedbeginning.content.particles.ColoredBreezeCloudParticleType;
import net.ty.createcraftedbeginning.content.particles.ColoredBreezeCloudParticleType.ColoredBreezeCloudParticleOptions;
import net.ty.createcraftedbeginning.content.particles.EndIncinerationParticle;
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

    @SuppressWarnings("unchecked")
    public static void registerFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet((SimpleParticleType) AIRTIGHT_JETPACK.get(), Provider::new);
        event.registerSpriteSet((SimpleParticleType) BREEZE_CLOUD.get(), BreezeCloudParticle.Provider::new);
        event.registerSpriteSet((ParticleType<ColoredBreezeCloudParticleOptions>) COLORED_BREEZE_CLOUD.get(), ColoredProvider::new);
        event.registerSpriteSet((SimpleParticleType) BUBBLE_WITHOUT_WATER.get(), BubbleWithoutWaterParticle.Provider::new);
        event.registerSpriteSet((SimpleParticleType) END_INCINERATION.get(), EndIncinerationParticle.Provider::new);
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
        private static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, CreateCraftedBeginning.MOD_ID);

        private final String name;
        private final DeferredHolder<ParticleType<?>, T> object;

        public ParticleEntry(String name, Supplier<T> typeSupplier) {
            this.name = name;
            object = REGISTER.register(name, typeSupplier);
        }
    }
}
