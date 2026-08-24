package net.ty.createcraftedbeginning.content.breezes.breezechamber.client;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberMovementBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BreezeChamberClientAnimation {
    private BreezeChamberClientAnimation() {
    }

    public static void initialize() {
        BreezeChamberBlockEntity.setClientTicker(BreezeChamberClientAnimation::tick);
        BreezeChamberMovementBehaviour.setCameraEntityProvider(context -> Minecraft.getInstance().cameraEntity);
    }

    public static void tickAnimation(BreezeChamberBlockEntity chamber) {
        chamber.tickAnimation(getTargetAngle(chamber));
    }

    private static void tick(BreezeChamberBlockEntity chamber) {
        Level level = chamber.getLevel();
        if (level == null) {
            return;
        }

        chamber.spawnParticles();
        if (VisualizationManager.supportsVisualization(level)) {
            return;
        }

        tickAnimation(chamber);
    }

    private static float getTargetAngle(BreezeChamberBlockEntity chamber) {
        float targetAngle = 0;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !player.isInvisible()) {
            double playerX = chamber.isVirtual() ? -4 : player.getX();
            double playerZ = chamber.isVirtual() ? -10 : player.getZ();
            double deltaX = playerX - (chamber.getBlockPos().getX() + 0.5);
            double deltaZ = playerZ - (chamber.getBlockPos().getZ() + 0.5);
            targetAngle = AngleHelper.deg(-Mth.atan2(deltaZ, deltaX)) - 90;
        }
        float currentAngle = chamber.getHeadAngle().getValue();
        return currentAngle + AngleHelper.getShortestAngleDiff(currentAngle, targetAngle);
    }
}
