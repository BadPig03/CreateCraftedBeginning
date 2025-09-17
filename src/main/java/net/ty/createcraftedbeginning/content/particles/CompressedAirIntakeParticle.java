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

public class CompressedAirIntakeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private float targetRoll;

    protected CompressedAirIntakeParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.friction = 0.96f;
        this.sprites = sprites;
        this.xd *= 0.1f;
        this.yd *= 0.1f;
        this.zd *= 0.1f;
        this.xd += xSpeed;
        this.yd += ySpeed;
        this.zd += zSpeed;
        float f1 = 1f - (float) (Math.random() * 0.3f);
        this.rCol = f1;
        this.gCol = f1;
        this.bCol = f1;
        this.quadSize *= 1.5f;
        int i = (int) (8f / (Math.random() * 0.8 + 0.3));
        this.lifetime = (int) Math.max((float) i * 2.5f, 1f);
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);

        if (this.xd != 0 || this.zd != 0) {
            this.targetRoll = (float) Math.atan2(this.zd, this.xd);
        }
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        return this.quadSize * Mth.clamp(((float) this.age + scaleFactor) / (float) this.lifetime * 32f, 0f, 1f);
    }

    @Override
    public void tick() {
        super.tick();

        this.oRoll = this.roll;
        this.roll = Mth.lerp(0.03f, this.roll, this.targetRoll);
        this.alpha = Mth.lerp((float) this.age / this.lifetime, 0.95f, 0.15f);
        this.quadSize = this.quadSize * 0.95f;

        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
        }
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

        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new CompressedAirIntakeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
