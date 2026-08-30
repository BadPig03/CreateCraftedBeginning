package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.sound.SoundScapes.AmbienceGroup;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightReactorKettleAnimationState {
    private final AirtightReactorKettleBlockEntity kettle;
    private final LerpedFloat ingredientRotation = LerpedFloat.angular().startWithValue(0);
    private final LerpedFloat ingredientRotationSpeed = LerpedFloat.linear().startWithValue(0);
    private final LerpedFloat mixerRotation = LerpedFloat.angular().startWithValue(0);
    private final LerpedFloat mixerRotationSpeed = LerpedFloat.linear().startWithValue(0);
    private final LerpedFloat windowDistance = LerpedFloat.linear().startWithValue(0.5);

    AirtightReactorKettleAnimationState(AirtightReactorKettleBlockEntity kettle) {
        this.kettle = kettle;
    }

    void tickClient() {
        CatnipServices.PLATFORM.executeOnClientOnly(() -> this::tickAudio);
        ingredientRotationSpeed.tickChaser();
        ingredientRotation.setValue(ingredientRotation.getValue() + ingredientRotationSpeed.getValue());
        mixerRotationSpeed.tickChaser();
        mixerRotation.setValue(mixerRotation.getValue() + mixerRotationSpeed.getValue());
    }

    void updateTargets(boolean moving, int operatingTicks, boolean windowsOpen) {
        float rotationSpeed = CCBMathUtils.clampMagnitude(kettle.getCore().getStructureManager().getSpeed() * 0.5f, 64);
        if (kettle.getLevel() instanceof PonderLevel) {
            rotationSpeed = SpeedLevel.FAST.getSpeedValue() * 0.5f;
        }

        boolean isProcessing = operatingTicks > 15 && operatingTicks <= AirtightReactorKettleController.PROCESSING_STARTED;
        float targetIngredientSpeed = 0;
        float targetMixerSpeed = 0;
        if (moving) {
            targetMixerSpeed = isProcessing ? rotationSpeed * 2 : rotationSpeed / 2;
            if (isProcessing) {
                targetIngredientSpeed = rotationSpeed * 0.5f;
            }
        }

        ingredientRotationSpeed.chase(targetIngredientSpeed, 0.15, Chaser.EXP);
        mixerRotationSpeed.chase(targetMixerSpeed, 0.1, Chaser.EXP);

        double targetWindowDistance = windowsOpen ? 0.5 : 0;
        double windowChaseSpeed = windowsOpen ? 0.2 : 0.3;
        windowDistance.chase(targetWindowDistance, windowChaseSpeed, Chaser.EXP);
        windowDistance.tickChaser();
    }

    LerpedFloat getIngredientRotation() {
        return ingredientRotation;
    }

    LerpedFloat getMixerRotation() {
        return mixerRotation;
    }

    LerpedFloat getWindowDistance() {
        return windowDistance;
    }

    @OnlyIn(Dist.CLIENT)
    private void tickAudio() {
        Level level = kettle.getLevel();
        if (level == null || !level.isClientSide) {
            return;
        }

        float absoluteSpeed = Mth.abs(kettle.getCore().getStructureManager().getSpeed());
        if (absoluteSpeed == 0) {
            return;
        }

        float pitch = Mth.clamp(absoluteSpeed / 256 + 0.45f, 0.85f, 1);
        SoundScapes.play(AmbienceGroup.KINETIC, kettle.getBlockPos(), pitch);
        if (absoluteSpeed <= 64 && AnimationTickHolder.getTicks() % 2 == 0 || kettle.getController().getOperatingTicks() != AirtightReactorKettleController.PROCESSING_STARTED) {
            return;
        }

        CCBSoundEvents.REACTOR_KETTLE_MIXING.playAt(level, kettle.getBlockPos(), 0.75f, 1, true);
    }
}
