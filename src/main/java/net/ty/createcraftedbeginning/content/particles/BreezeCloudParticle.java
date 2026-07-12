package net.ty.createcraftedbeginning.content.particles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.particles.ColoredBreezeCloudParticleType.ColoredBreezeCloudParticleOptions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class BreezeCloudParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected BreezeCloudParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        this(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, 0xFFFFFFFF);
    }

    protected BreezeCloudParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites, int color) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        friction = 0.96f;
        this.sprites = sprites;
        xd *= 0.1f;
        yd *= 0.1f;
        zd *= 0.1f;
        xd += xSpeed;
        yd += ySpeed;
        zd += zSpeed;

        float brightness = 1.0f - (float) (Math.random() * 0.3f);
        rCol = ARGB32.red(color) / 255.0f * brightness;
        gCol = ARGB32.green(color) / 255.0f * brightness;
        bCol = ARGB32.blue(color) / 255.0f * brightness;
        alpha = ARGB32.alpha(color) / 255.0f;

        quadSize *= 1.875f;
        int i = (int) (8.0 / (Math.random() * 0.8 + 0.3));
        lifetime = (int) Math.max((float) i * 2.5f, 1.0f);
        hasPhysics = false;
        setSpriteFromAge(sprites);
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        return quadSize * Mth.clamp(((float) age + scaleFactor) / (float) lifetime * 32.0f, 0.0f, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }

        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new BreezeCloudParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ColoredProvider implements ParticleProvider<ColoredBreezeCloudParticleOptions> {
        private final SpriteSet sprites;

        public ColoredProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColoredBreezeCloudParticleOptions options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new BreezeCloudParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, options.color());
        }
    }
}
