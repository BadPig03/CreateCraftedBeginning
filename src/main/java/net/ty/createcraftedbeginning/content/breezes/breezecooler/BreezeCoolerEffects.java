package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeCoolerEffects {
    private final BreezeCoolerBlockEntity cooler;

    BreezeCoolerEffects(BreezeCoolerBlockEntity cooler) {
        this.cooler = cooler;
    }

    void spawnParticles() {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }

        RandomSource random = level.getRandom();
        if (random.nextInt(2) != 0) {
            return;
        }

        BlockPos pos = cooler.getBlockPos();
        BlockPos above = pos.above();
        Vec3 center = VecHelper.getCenterOf(pos);
        Vec3 particlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125F).multiply(1, 0, 1));
        boolean isTopOpen = level.getBlockState(above).getCollisionShape(level, above).isEmpty();
        if (isTopOpen || random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
        }
        Vec3 chilledParticlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5F).multiply(1, 0.25, 1).normalize().scale((isTopOpen ? 0.25 : 0.5) + random.nextDouble() * 0.125)).add(0, 0.5, 0);
        if (!cooler.getFrostLevelFromBlock().isAtLeast(FrostLevel.CHILLED)) {
            return;
        }

        level.addParticle(ParticleTypes.SNOWFLAKE, chilledParticlePos.x, chilledParticlePos.y, chilledParticlePos.z, 0, isTopOpen ? 0.0625 : random.nextDouble() * 0.0125, 0);
    }

    void playSound() {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }

        level.playSound(null, cooler.getBlockPos(), SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.125F + level.random.nextFloat() * 0.125F, 0.75F - level.random.nextFloat() * 0.25F);
    }

    void spawnParticleBurst() {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(cooler.getBlockPos());
        RandomSource random = level.random;
        for (int i = 0; i < 20; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5F).multiply(1, 0.25, 1).normalize();
            Vec3 particlePos = center.add(offset.scale(0.5 + random.nextDouble() * 0.125)).add(0, 0.125, 0);
            Vec3 velocity = offset.scale(0.03125);
            level.addParticle(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, velocity.x, velocity.y, velocity.z);
        }
    }
}
