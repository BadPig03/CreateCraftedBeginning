package net.ty.createcraftedbeginning.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class CCBParticles {
    private static final int REDUCED_DESTROY_PARTICLE_COUNT = 16;
    private static final double PARTICLE_POSITION_MARGIN = 0.2;
    private static final double PARTICLE_POSITION_RANGE = 0.6;
    private static final double PARTICLE_OUTWARD_SPEED = 0.12;
    private static final double PARTICLE_UPWARD_SPEED = 0.04;

    private CCBParticles() {
    }

    public static void addReducedDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
        if (state.isAir()) {
            return;
        }

        RandomSource random = level.getRandom();
        BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, state);
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        for (int i = 0; i < REDUCED_DESTROY_PARTICLE_COUNT; i++) {
            double x = pos.getX() + PARTICLE_POSITION_MARGIN + random.nextDouble() * PARTICLE_POSITION_RANGE;
            double y = pos.getY() + PARTICLE_POSITION_MARGIN + random.nextDouble() * PARTICLE_POSITION_RANGE;
            double z = pos.getZ() + PARTICLE_POSITION_MARGIN + random.nextDouble() * PARTICLE_POSITION_RANGE;
            double velocityX = (x - centerX) * PARTICLE_OUTWARD_SPEED;
            double velocityY = (y - centerY) * PARTICLE_OUTWARD_SPEED + PARTICLE_UPWARD_SPEED;
            double velocityZ = (z - centerZ) * PARTICLE_OUTWARD_SPEED;
            manager.createParticle(option, x, y, z, velocityX, velocityY, velocityZ);
        }
    }
}
