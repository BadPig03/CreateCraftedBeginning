package net.ty.createcraftedbeginning.content.airtights.balloon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record FrogBalloonPose(Vec3 offset, float baseY, float hookDistance, float boxDistance) {
    public static FrogBalloonPose calculate(Vec3 travel, float distance, ItemStack balloon, boolean depositing, boolean animating) {
        float progress = Mth.clamp(distance / Math.max((float) travel.length(), 1), 0.0F, 1.0F);
        float hook = Mth.lerp(progress, 0, BalloonItem.getHookDistance(balloon));
        float box = Mth.lerp(progress, 0, BalloonItem.getBoxDistance(balloon));
        float baseY = depositing ? Mth.lerp(progress, 0.1875F, 0.625F) : 0.1875F;
        Vec3 direction = travel.lengthSqr() < 1.0E-6 ? Vec3.ZERO : travel.normalize();
        Vec3 offset = direction.scale(distance);
        if (animating && depositing) {
            offset = offset.subtract(0, 0.75, 0);
        }
        return new FrogBalloonPose(offset, baseY, hook, box);
    }
}
