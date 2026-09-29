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

    private BreezeCloudParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        this(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, 0xFFFFFFFF);
    }

    private BreezeCloudParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites, int color) {
        super(level, x, y, z, 0, 0, 0);
        friction = 0.96F;
        this.sprites = sprites;
        xd *= 0.1;
        yd *= 0.1;
        zd *= 0.1;
        xd += xSpeed;
        yd += ySpeed;
        zd += zSpeed;

        float brightnessReduction = (float) (random.nextDouble() * 0.3);
        float brightness = 1 - brightnessReduction;
        rCol = ARGB32.red(color) / 255.0F * brightness;
        gCol = ARGB32.green(color) / 255.0F * brightness;
        bCol = ARGB32.blue(color) / 255.0F * brightness;
        alpha = ARGB32.alpha(color) / 255.0F;

        quadSize *= 1.875F;
        int baseLifetime = (int) (8 / (random.nextDouble() * 0.8 + 0.3));
        lifetime = (int) Math.max((float) baseLifetime * 2.5F, 1);
        hasPhysics = false;
        setSpriteFromAge(sprites);
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        float progress = (age + scaleFactor) / lifetime * 32;
        return quadSize * Mth.clamp(progress, 0.0F, 1.0F);
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
