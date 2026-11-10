package net.ty.createcraftedbeginning.content.particles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ShriekParticle;
import net.minecraft.client.particle.SpriteSet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.particles.AmethystSoundSuppressorRippleParticleType.RippleOptions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class AmethystSoundSuppressorRippleParticle extends ShriekParticle {
    private AmethystSoundSuppressorRippleParticle(ClientLevel level, double x, double y, double z, int delay, SpriteSet sprites) {
        super(level, x, y, z, delay);
        pickSprite(sprites);
        setAlpha(1);
    }

    public static final class Provider implements ParticleProvider<RippleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(RippleOptions options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new AmethystSoundSuppressorRippleParticle(level, x, y, z, options.delay(), sprites);
        }
    }
}
