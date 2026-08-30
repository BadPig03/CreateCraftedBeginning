package net.ty.createcraftedbeginning.content.particles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class BubbleWithoutWaterParticle extends TextureSheetParticle {
    private static final double VERTICAL_ACCELERATION = 0.002;

    private BubbleWithoutWaterParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z);
        setSize(0.02f, 0.02f);
        quadSize *= random.nextFloat() * 0.6f + 0.2f;
        xd = xSpeed * 0.2 + (random.nextDouble() * 2 - 1) * 0.02;
        yd = ySpeed * 0.2 + (random.nextDouble() * 2 - 1) * 0.02;
        zd = zSpeed * 0.2 + (random.nextDouble() * 2 - 1) * 0.02;
        lifetime = (int) (8 / (random.nextDouble() * 0.8 + 0.2));
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

        yd += VERTICAL_ACCELERATION;
        move(xd, yd, zd);
        xd *= 0.85;
        yd *= 0.85;
        zd *= 0.85;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            BubbleWithoutWaterParticle particle = new BubbleWithoutWaterParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
