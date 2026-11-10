package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeChamberEffects {
    private final BreezeChamberBlockEntity chamber;

    BreezeChamberEffects(BreezeChamberBlockEntity chamber) {
        this.chamber = chamber;
    }

    void playSound(boolean isIllCharge) {
        Level level = chamber.getLevel();
        if (level == null) {
            return;
        }

        if (isIllCharge) {
            level.playSound(null, chamber.getBlockPos(), SoundEvents.BREEZE_HURT, SoundSource.BLOCKS, 0.125F + level.random.nextFloat() * 0.125F, 0.75F - level.random.nextFloat() * 0.25F);
            return;
        }

        level.playSound(null, chamber.getBlockPos(), SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.125F + level.random.nextFloat() * 0.125F, 0.75F - level.random.nextFloat() * 0.25F);
    }

    void spawnParticleBurst(boolean isIllCharge) {
        Level level = chamber.getLevel();
        if (level == null) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(chamber.getBlockPos());
        RandomSource random = level.random;
        int particleCount = isIllCharge ? 5 : 20;
        ParticleOptions particleOptions = CCBParticleTypes.BREEZE_CLOUD.getParticleOptions();
        for (int particleIndex = 0; particleIndex < particleCount; particleIndex++) {
            Vec3 particleDirection = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5F).multiply(1, 0.25, 1).normalize();
            Vec3 particlePos = center.add(particleDirection.scale(0.5 + random.nextDouble() * 0.125)).add(0, 0.125, 0);
            Vec3 particleMotion = particleDirection.scale(0.03125);
            level.addParticle(particleOptions, particlePos.x, particlePos.y, particlePos.z, particleMotion.x, particleMotion.y, particleMotion.z);
        }
    }

    void spawnParticles() {
        WindLevel windLevel = chamber.getWindLevelFromBlock();
        Level level = chamber.getLevel();
        if (level == null) {
            return;
        }

        RandomSource random = level.getRandom();
        int particleChanceBound = windLevel == WindLevel.ILL ? 4 : 2;
        if (random.nextInt(particleChanceBound) != 0) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(chamber.getBlockPos());
        Vec3 particlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125F).multiply(1, 0, 1));
        ParticleOptions particleOptions = CCBParticleTypes.BREEZE_CLOUD.getParticleOptions();
        if (random.nextInt(particleChanceBound * 2) == 0) {
            level.addParticle(particleOptions, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
        }
        double upwardMotion = random.nextDouble() * 0.0125;
        Vec3 galeParticlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5F).multiply(1, 0.25, 1).normalize().scale(0.5 + random.nextDouble() * 0.125)).add(0, 0.5, 0);
        if (!windLevel.isActive()) {
            return;
        }

        level.addParticle(particleOptions, galeParticlePos.x, galeParticlePos.y, galeParticlePos.z, 0, upwardMotion, 0);
    }
}
