package net.ty.createcraftedbeginning.content.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class BubbleWithoutWaterParticle extends TextureSheetParticle {
    protected BubbleWithoutWaterParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z);
        setSize(0.02f, 0.02f);
        quadSize = quadSize * (random.nextFloat() * 0.6f + 0.2f);
        xd = xSpeed * 0.2f + (Math.random() * 2 - 1) * 0.02f;
        yd = ySpeed * 0.2f + (Math.random() * 2 - 1) * 0.02f;
        zd = zSpeed * 0.2f + (Math.random() * 2 - 1) * 0.02f;
        lifetime = (int) (8 / (Math.random() * 0.8 + 0.2));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (lifetime-- <= 0) {
            remove();
            return;
        }

        yd += 0.002;
        move(xd, yd, zd);
        xd *= 0.85f;
        yd *= 0.85f;
        zd *= 0.85f;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprites) {
            sprite = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            BubbleWithoutWaterParticle particle = new BubbleWithoutWaterParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(sprite);
            return particle;
        }
    }
}
