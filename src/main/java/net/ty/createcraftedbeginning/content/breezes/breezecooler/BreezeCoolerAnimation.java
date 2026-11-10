package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeCoolerAnimation {
    private BreezeCoolerAnimation() {
    }

    static void tick(BreezeCoolerBlockEntity cooler, float targetAngle) {
        LerpedFloat headAngle = cooler.getHeadAngle();
        LerpedFloat headAnimation = cooler.getHeadAnimation();
        headAngle.chase(targetAngle, 0.25F, Chaser.exp(5));
        headAngle.tickChaser();
        headAnimation.chase(0, 0.25F, Chaser.exp(0.25F));
        headAnimation.tickChaser();
    }
}
