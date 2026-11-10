package net.ty.createcraftedbeginning.content.opticalpower.amethystloader;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystLoaderAnimation {
    private final LerpedFloat speed = LerpedFloat.linear().startWithValue(0);
    private final LerpedFloat angle = LerpedFloat.angular().startWithValue(0);

    void tick(boolean active) {
        speed.chase(active ? 28 : 0, 0.15F, Chaser.EXP);
        speed.tickChaser();
        angle.setValue((angle.getValue() + speed.getValue()) % 360);
    }

    float angle(float partialTick) {
        return angle.getValue(partialTick);
    }
}
