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
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
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
    private boolean goggles;
    private boolean trainHat;

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
        boolean active = gasProcessor.isControllerActive();
        boolean inputInvalid = gasProcessor.isInputInvalid();
        boolean outputFailed = (gasProcessor.isOutputFull() || gasProcessor.isOutputMismatched()) && !active;
        int time = chamber.getWindRemainingTime();
        if (windLevel != WindLevel.CALM) {
            CCBLang.translate("gui.breeze_chamber.remaining_time").style(ChatFormatting.GRAY).forGoggles(tooltip);
            ChatFormatting timeColor = time > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
            if (chamber.isCreative()) {
                CCBLang.translate("gui.gas_container.infinity").style(timeColor).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.seconds(time, chamber.getLevel().tickRateManager().tickrate()).style(timeColor).forGoggles(tooltip, 1);
            }
            if (active) {
                CCBLang.translate("gui.breeze_chamber.energization_level").style(ChatFormatting.GRAY).forGoggles(tooltip);
                CCBLang.translate("gui.breeze_chamber.current_level", CCBLang.number(chamber.getWindRemainingLevel())).style(ChatFormatting.BLUE).forGoggles(tooltip, 1);
            }
        }
        if (active) {
            return true;
        }

        tooltip.add(CommonComponents.EMPTY);
        IGasHandler handler = chamber.getTankBehaviourInternal().getPrimaryHandler();
        GasStack gasStack = handler.getGasInTank(0);
        long capacity = handler.getTankCapacity(0);
        CCBLang.translate("gui.gas_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (gasStack.isEmpty()) {
            GasAmountUtils.precise(capacity).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.gasName(gasStack).style(ChatFormatting.WHITE).forGoggles(tooltip, 1);
            GasAmountUtils.precise(gasStack.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmountUtils.precise(capacity).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }
        if (inputInvalid || outputFailed) {
            tooltip.add(CommonComponents.EMPTY);
            CCBLang.translate("gui.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        }
        if (inputInvalid) {
            CCBLang.addToGoggles(tooltip, "gui.breeze_chamber.invalid_gas", Component.translatable(tankGasType.getTranslationKey()));
        }
        if (outputFailed) {
            CCBLang.addToGoggles(tooltip, "gui.breeze_chamber.output_failed");
        }
        return true;
    }

    void playSound(boolean bad) {
        if (chamber.getLevel() == null) {
            return;
        }
        if (bad) {
            chamber.getLevel().playSound(null, chamber.getBlockPos(), SoundEvents.BREEZE_HURT, SoundSource.BLOCKS, 0.125f + chamber.getLevel().random.nextFloat() * 0.125f, 0.75f - chamber.getLevel().random.nextFloat() * 0.25f);
        }
        else {
            chamber.getLevel().playSound(null, chamber.getBlockPos(), SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.125f + chamber.getLevel().random.nextFloat() * 0.125f, 0.75f - chamber.getLevel().random.nextFloat() * 0.25f);
        }
    }

    void spawnParticleBurst(boolean bad) {
        Level level = chamber.getLevel();
        if (level == null) {
            return;
        }
        Vec3 center = VecHelper.getCenterOf(chamber.getBlockPos());
        RandomSource random = level.random;
        int count = bad ? 5 : 20;
        for (int i = 0; i < count; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25, 1).normalize();
            Vec3 particlePos = center.add(offset.scale(0.5 + random.nextDouble() * 0.125)).add(0, 0.125, 0);
            Vec3 motion = offset.scale(0.03125);
            level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), particlePos.x, particlePos.y, particlePos.z, motion.x, motion.y, motion.z);
        }
    }

    void tickAnimation(float targetAngle) {
        boolean active = chamber.isControllerActive();
        if (active) {
            float facingAngle = (AngleHelper.horizontalAngle(chamber.getBlockState().getOptionalValue(BreezeChamberBlock.FACING).orElse(Direction.NORTH)) + 180) % 360;
            chamber.headAngle.chase(facingAngle, 0.125f, Chaser.EXP);
        }
        else {
            chamber.headAngle.chase(targetAngle, 0.25f, Chaser.exp(5));
        }
        chamber.headAngle.tickChaser();
        chamber.getHeadAnimationInternal().chase(active ? 1 : 0, 0.25f, Chaser.exp(0.25f));
        chamber.getHeadAnimationInternal().tickChaser();
    }

    void spawnParticles() {
        WindLevel windLevel = chamber.getWindLevelFromBlock();
        if (chamber.getLevel() == null) {
            return;
        }
        RandomSource random = chamber.getLevel().getRandom();
        int possibility = windLevel == WindLevel.ILL ? 4 : 2;
        if (random.nextInt(possibility) != 0) {
            return;
        }
        Vec3 center = VecHelper.getCenterOf(chamber.getBlockPos());
        Vec3 particlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f).multiply(1, 0, 1));
        if (random.nextInt(possibility * 2) == 0) {
            chamber.getLevel().addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
        }
        double yMotion = random.nextDouble() * 0.0125;
        Vec3 galeParticlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25, 1).normalize().scale(0.5 + random.nextDouble() * 0.125)).add(0, 0.5, 0);
        if (windLevel.isAtLeast(WindLevel.GALE)) {
            chamber.getLevel().addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), galeParticlePos.x, galeParticlePos.y, galeParticlePos.z, 0, yMotion, 0);
        }
    }

    boolean hasGoggles() {
        return goggles;
    }

    boolean hasTrainHat() {
        return trainHat;
    }

    void setGoggles(boolean goggles) {
        this.goggles = goggles;
    }

    void setTrainHat(boolean trainHat) {
        this.trainHat = trainHat;
    }
}
