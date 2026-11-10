package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeChamberAnimation {
    private BreezeChamberAnimation() {
    }

    static void tick(BreezeChamberBlockEntity chamber, float targetAngle) {
        boolean isControllerActive = chamber.isControllerActive();
        LerpedFloat headAngle = chamber.getHeadAngle();
        LerpedFloat headAnimation = chamber.getHeadAnimation();
        if (isControllerActive) {
            float facingAngle = (AngleHelper.horizontalAngle(chamber.getBlockState().getOptionalValue(BreezeChamberBlock.FACING).orElse(Direction.NORTH)) + 180) % 360;
            headAngle.chase(facingAngle, 0.125F, Chaser.EXP);
        }
        else {
            headAngle.chase(targetAngle, 0.25F, Chaser.exp(5));
        }
        headAngle.tickChaser();
        headAnimation.chase(isControllerActive ? 1 : 0, 0.25F, Chaser.exp(0.25F));
        headAnimation.tickChaser();
    }
}
