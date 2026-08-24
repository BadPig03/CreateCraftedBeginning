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
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
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
        if (chamber.getLevel() == null) {
            return false;
        }

        WindLevel windLevel = chamber.getWindLevel();
        CCBLang.translate("gui.breeze_chamber").forGoggles(tooltip);
        CCBLang.translate("gui.breeze_chamber.current_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(windLevel.getTranslatable()).style(windLevel.getChatFormatting()).forGoggles(tooltip, 1);

        BreezeChamberGasProcessor gasProcessor = chamber.getGasProcessorInternal();
        Gas tankGasType = gasProcessor.getTankGasType();
        boolean isControllerActive = gasProcessor.isControllerActive();
        boolean hasInvalidInput = gasProcessor.isInputInvalid();
        boolean hasOutputFailure = (gasProcessor.isOutputFull() || gasProcessor.isOutputMismatched()) && !isControllerActive;
        int remainingTime = chamber.getWindRemainingTime();
        if (windLevel != WindLevel.CALM) {
            CCBLang.translate("gui.breeze_chamber.remaining_time").style(ChatFormatting.GRAY).forGoggles(tooltip);
            ChatFormatting timeColor = remainingTime > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
            if (chamber.isCreative()) {
                CCBLang.translate("gui.gas_container.infinity").style(timeColor).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.seconds(remainingTime, chamber.getLevel().tickRateManager().tickrate()).style(timeColor).forGoggles(tooltip, 1);
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
        IGasHandler outputHandler = chamber.getTankBehaviourInternal().getPrimaryHandler();
        GasStack outputGas = outputHandler.getGasInTank(0);
        long outputCapacity = outputHandler.getTankCapacity(0);
        CCBLang.translate("gui.gas_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (outputGas.isEmpty()) {
            GasAmounts.precise(outputCapacity).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.gasName(outputGas).style(ChatFormatting.WHITE).forGoggles(tooltip, 1);
            GasAmounts.precise(outputGas.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmounts.precise(outputCapacity).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }
        if (hasInvalidInput || hasOutputFailure) {
            tooltip.add(CommonComponents.EMPTY);
            CCBLang.translate("gui.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        }
        if (hasInvalidInput) {
            CCBLang.addToGoggles(tooltip, "gui.breeze_chamber.invalid_gas", Component.translatable(tankGasType.getTranslationKey()));
        }
        if (hasOutputFailure) {
            CCBLang.addToGoggles(tooltip, "gui.breeze_chamber.output_failed");
        }
        return true;
    }

    void playSound(boolean isIllCharge) {
        if (chamber.getLevel() == null) {
            return;
        }

        if (isIllCharge) {
            chamber.getLevel().playSound(null, chamber.getBlockPos(), SoundEvents.BREEZE_HURT, SoundSource.BLOCKS, 0.125f + chamber.getLevel().random.nextFloat() * 0.125f, 0.75f - chamber.getLevel().random.nextFloat() * 0.25f);
        }
        else {
            chamber.getLevel().playSound(null, chamber.getBlockPos(), SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.125f + chamber.getLevel().random.nextFloat() * 0.125f, 0.75f - chamber.getLevel().random.nextFloat() * 0.25f);
        }
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
            Vec3 particleDirection = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25, 1).normalize();
            Vec3 particlePos = center.add(particleDirection.scale(0.5 + random.nextDouble() * 0.125)).add(0, 0.125, 0);
            Vec3 particleMotion = particleDirection.scale(0.03125);
            level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), particlePos.x, particlePos.y, particlePos.z, particleMotion.x, particleMotion.y, particleMotion.z);
        }
    }

    void tickAnimation(float targetAngle) {
        boolean isControllerActive = chamber.isControllerActive();
        if (isControllerActive) {
            float facingAngle = (AngleHelper.horizontalAngle(chamber.getBlockState().getOptionalValue(BreezeChamberBlock.FACING).orElse(Direction.NORTH)) + 180) % 360;
            chamber.getHeadAngle().chase(facingAngle, 0.125f, Chaser.EXP);
        }
        else {
            chamber.getHeadAngle().chase(targetAngle, 0.25f, Chaser.exp(5));
        }
        chamber.getHeadAngle().tickChaser();
        chamber.getHeadAnimationInternal().chase(isControllerActive ? 1 : 0, 0.25f, Chaser.exp(0.25f));
        chamber.getHeadAnimationInternal().tickChaser();
    }

    void spawnParticles() {
        WindLevel windLevel = chamber.getWindLevelFromBlock();
        if (chamber.getLevel() == null) {
            return;
        }

        RandomSource random = chamber.getLevel().getRandom();
        int particleChanceBound = windLevel == WindLevel.ILL ? 4 : 2;
        if (random.nextInt(particleChanceBound) != 0) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(chamber.getBlockPos());
        Vec3 particlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f).multiply(1, 0, 1));
        if (random.nextInt(particleChanceBound * 2) == 0) {
            chamber.getLevel().addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
        }
        double upwardMotion = random.nextDouble() * 0.0125;
        Vec3 galeParticlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25, 1).normalize().scale(0.5 + random.nextDouble() * 0.125)).add(0, 0.5, 0);
        if (!windLevel.isActive()) {
            return;
        }

        chamber.getLevel().addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), galeParticlePos.x, galeParticlePos.y, galeParticlePos.z, 0, upwardMotion, 0);
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
