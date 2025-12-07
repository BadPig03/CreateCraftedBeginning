package net.ty.createcraftedbeginning.content.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class BreezeCloudParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected BreezeCloudParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        friction = 0.96f;
        this.sprites = sprites;
        xd *= 0.1f;
        yd *= 0.1f;
        zd *= 0.1f;
        xd += xSpeed;
        yd += ySpeed;
        zd += zSpeed;
        float f1 = 1.0f - (float) (Math.random() * 0.3f);
        rCol = f1;
        gCol = f1;
        bCol = f1;
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
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new BreezeCloudParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
