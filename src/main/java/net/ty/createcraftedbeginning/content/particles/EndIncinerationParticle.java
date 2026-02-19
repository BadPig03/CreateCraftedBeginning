package net.ty.createcraftedbeginning.content.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class EndIncinerationParticle extends TextureSheetParticle {
    protected EndIncinerationParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        gravity = 0.75f;
        friction = 0.999f;
        xd *= 0.8f;
        yd *= 0.8f;
        zd *= 0.8f;
        yd = random.nextFloat() * 0.4f + 0.05f;
        quadSize = quadSize * (random.nextFloat() * 2 + 0.2f);
        lifetime = (int) (16.0 / (Math.random() * 0.8 + 0.2));
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public int getLightColor(float partialTick) {
        int i = super.getLightColor(partialTick);
        return 240 | (i >> 16 & 0xFF) << 16;
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        float f = (age + scaleFactor) / lifetime;
        return quadSize * (1.0f - f * f);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }

        float f = (float) age / lifetime;
        if (!(random.nextFloat() > f)) {
            return;
        }

        level.addParticle(ParticleTypes.SMOKE, x, y, z, xd, yd, zd);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            EndIncinerationParticle particle = new EndIncinerationParticle(level, x, y, z);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
