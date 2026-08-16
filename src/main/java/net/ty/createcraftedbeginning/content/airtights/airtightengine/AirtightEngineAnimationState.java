package net.ty.createcraftedbeginning.content.airtights.airtightengine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightEngineAnimationState {
    private float pistonPhase;
    private float pistonAnimationSpeed;

    public void tick(float speed, boolean overStressed, AirtightEngineDriveController driveController) {
        pistonAnimationSpeed = Mth.abs(speed);
        if (pistonAnimationSpeed == 0 && !overStressed) {
            pistonAnimationSpeed = Mth.abs(driveController.getGeneratedSpeed());
        }

        pistonPhase += pistonAnimationSpeed * AirtightEngineBlockEntity.DELTA_TIME;
        if (pistonPhase <= Mth.TWO_PI) {
            return;
        }

        pistonPhase %= Mth.TWO_PI;
    }

    public float getPistonPhase(float partialTicks) {
        return pistonPhase + pistonAnimationSpeed * partialTicks * AirtightEngineBlockEntity.DELTA_TIME;
    }
}
