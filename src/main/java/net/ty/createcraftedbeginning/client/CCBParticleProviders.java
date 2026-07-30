package net.ty.createcraftedbeginning.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.ty.createcraftedbeginning.content.particles.AirtightJetpackParticle;
import net.ty.createcraftedbeginning.content.particles.BreezeCloudParticle;
import net.ty.createcraftedbeginning.content.particles.BreezeCloudParticle.ColoredProvider;
import net.ty.createcraftedbeginning.content.particles.BubbleWithoutWaterParticle;
import net.ty.createcraftedbeginning.content.particles.ColoredBreezeCloudParticleType.ColoredBreezeCloudParticleOptions;
import net.ty.createcraftedbeginning.content.particles.EndIncinerationParticle.Provider;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class CCBParticleProviders {
    private CCBParticleProviders() {
    }

    @SuppressWarnings("unchecked")
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet((SimpleParticleType) CCBParticleTypes.AIRTIGHT_JETPACK.get(), AirtightJetpackParticle.Provider::new);
        event.registerSpriteSet((SimpleParticleType) CCBParticleTypes.BREEZE_CLOUD.get(), BreezeCloudParticle.Provider::new);
        event.registerSpriteSet((ParticleType<ColoredBreezeCloudParticleOptions>) CCBParticleTypes.COLORED_BREEZE_CLOUD.get(), ColoredProvider::new);
        event.registerSpriteSet((SimpleParticleType) CCBParticleTypes.BUBBLE_WITHOUT_WATER.get(), BubbleWithoutWaterParticle.Provider::new);
        event.registerSpriteSet((SimpleParticleType) CCBParticleTypes.END_INCINERATION.get(), Provider::new);
    }
}
