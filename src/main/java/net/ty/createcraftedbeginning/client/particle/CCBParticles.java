package net.ty.createcraftedbeginning.client.particle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class CCBParticles {
    private CCBParticles() {
    }

    public static void addReducedDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
        if (state.isAir()) {
            return;
        }

        RandomSource random = level.getRandom();
        BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, state);
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        for (int i = 0; i < 16; i++) {
            double particleX = pos.getX() + 0.2 + random.nextDouble() * 0.6;
            double particleY = pos.getY() + 0.2 + random.nextDouble() * 0.6;
            double particleZ = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
            double velocityX = (particleX - centerX) * 0.12;
            double velocityY = (particleY - centerY) * 0.12 + 0.04;
            double velocityZ = (particleZ - centerZ) * 0.12;
            manager.createParticle(particleOption, particleX, particleY, particleZ, velocityX, velocityY, velocityZ);
        }
    }
}
