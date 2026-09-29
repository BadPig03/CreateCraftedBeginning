package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasUnitsTooltips;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeChamberDisplay {
    private final BreezeChamberBlockEntity chamber;
    private boolean hasGoggles;
    private boolean hasTrainHat;

    BreezeChamberDisplay(BreezeChamberBlockEntity chamber) {
        this.chamber = chamber;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        Level level = chamber.getLevel();
        if (level == null) {
            return false;
        }

        WindLevel windLevel = chamber.getWindLevel();
        CCBLang.translate("gui.breeze_chamber").forGoggles(tooltip);
        CCBLang.translate("gui.breeze_chamber.current_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(windLevel.getTranslatable()).style(windLevel.getChatFormatting()).forGoggles(tooltip, 1);

        BreezeChamberGasProcessor gasProcessor = chamber.getGasProcessorInternal();
        boolean isControllerActive = gasProcessor.isControllerActive();
        int remainingTime = chamber.getWindRemainingTime();
        if (windLevel != WindLevel.CALM) {
            if (GoggleTooltip.isVisible(tooltip, Section.BREEZE_TIME)) {
                CCBLang.translate("gui.breeze_chamber.remaining_time").style(ChatFormatting.GRAY).forGoggles(tooltip);
                ChatFormatting timeColor = remainingTime > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                if (chamber.isCreative()) {
                    CCBLang.translate("gui.gas_container.infinity").style(timeColor).forGoggles(tooltip, 1);
                }
                else {
                    CCBLang.seconds(remainingTime, level.tickRateManager().tickrate()).style(timeColor).forGoggles(tooltip, 1);
                }
            }
            if (isControllerActive) {
                CCBLang.translate("gui.breeze_chamber.energization_level").style(ChatFormatting.GRAY).forGoggles(tooltip);
                CCBLang.translate("gui.breeze_chamber.current_level", CCBLang.number(chamber.getWindRemainingLevel())).style(ChatFormatting.BLUE).forGoggles(tooltip, 1);
            }
        }
        if (isControllerActive) {
            return true;
        }

        tooltip.add(CommonComponents.EMPTY);
        GasStorageHandler outputHandler = chamber.getTankBehaviourInternal().getPrimaryHandler();
        GasUnitsTooltips.addContainer(tooltip, outputHandler);
        return true;
    }

    boolean addToTooltip(List<Component> tooltip) {
        if (chamber.getLevel() == null) {
            return false;
        }

        BreezeChamberGasProcessor gasProcessor = chamber.getGasProcessorInternal();
        if (gasProcessor.isControllerActive()) {
            return false;
        }

        boolean hasInvalidInput = gasProcessor.isInputInvalid();
        boolean hasOutputFailure = gasProcessor.isOutputBlocked();
        if (!hasInvalidInput && !hasOutputFailure) {
            return false;
        }

        CCBLang.translate("gui.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        if (hasInvalidInput) {
            Gas tankGasType = gasProcessor.getTankGasType();
            CCBLang.addToGoggles(tooltip, "gui.breeze_chamber.invalid_gas", Component.translatable(tankGasType.getTranslationKey()));
        }
        if (hasOutputFailure) {
            CCBLang.addToGoggles(tooltip, "gui.breeze_chamber.output_failed");
        }
        return true;
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
        for (int particleIndex = 0; particleIndex < particleCount; particleIndex++) {
            Vec3 particleDirection = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5F).multiply(1, 0.25, 1).normalize();
            Vec3 particlePos = center.add(particleDirection.scale(0.5 + random.nextDouble() * 0.125)).add(0, 0.125, 0);
            Vec3 particleMotion = particleDirection.scale(0.03125);
            level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), particlePos.x, particlePos.y, particlePos.z, particleMotion.x, particleMotion.y, particleMotion.z);
        }
    }

    void tickAnimation(float targetAngle) {
        boolean isControllerActive = chamber.isControllerActive();
        if (isControllerActive) {
            float facingAngle = (AngleHelper.horizontalAngle(chamber.getBlockState().getOptionalValue(BreezeChamberBlock.FACING).orElse(Direction.NORTH)) + 180) % 360;
            chamber.getHeadAngle().chase(facingAngle, 0.125F, Chaser.EXP);
        }
        else {
            chamber.getHeadAngle().chase(targetAngle, 0.25F, Chaser.exp(5));
        }
        chamber.getHeadAngle().tickChaser();
        chamber.getHeadAnimationInternal().chase(isControllerActive ? 1 : 0, 0.25F, Chaser.exp(0.25F));
        chamber.getHeadAnimationInternal().tickChaser();
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
        if (random.nextInt(particleChanceBound * 2) == 0) {
            level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
        }
        double upwardMotion = random.nextDouble() * 0.0125;
        Vec3 galeParticlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5F).multiply(1, 0.25, 1).normalize().scale(0.5 + random.nextDouble() * 0.125)).add(0, 0.5, 0);
        if (!windLevel.isActive()) {
            return;
        }

        level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), galeParticlePos.x, galeParticlePos.y, galeParticlePos.z, 0, upwardMotion, 0);
    }

    boolean hasGoggles() {
        return hasGoggles;
    }

    boolean hasTrainHat() {
        return hasTrainHat;
    }

    void setGoggles(boolean hasGoggles) {
        this.hasGoggles = hasGoggles;
    }

    void setTrainHat(boolean hasTrainHat) {
        this.hasTrainHat = hasTrainHat;
    }
}
