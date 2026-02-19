package net.ty.createcraftedbeginning.content.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class AirtightJetpackParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected AirtightJetpackParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z);
        friction = 1.0f;
        this.sprites = sprites;
        xd = xSpeed;
        yd = ySpeed;
        zd = zSpeed;
        quadSize = 0.12f * (random.nextFloat() * random.nextFloat() + 1);
        lifetime = (int) (8 / ((double) random.nextFloat() * 0.8 + 0.2)) + 2;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        xd *= 0.95f;
        yd *= 0.95f;
        zd *= 0.95f;
        if (lifetime-- <= 0) {
            remove();
        }
        if (!level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER)) {
            return;
        }

        level.addParticle(ParticleTypes.BUBBLE, x, y, z, xd, yd, zd);
        remove();
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            if (level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER)) {
                level.addParticle(ParticleTypes.BUBBLE, x, y, z, xSpeed, ySpeed, zSpeed);
                return null;
            }

            AirtightJetpackParticle particle = new AirtightJetpackParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
            particle.setColor(0.964f, 0.964f, 0.964f);
            return particle;
        }
    }
}
