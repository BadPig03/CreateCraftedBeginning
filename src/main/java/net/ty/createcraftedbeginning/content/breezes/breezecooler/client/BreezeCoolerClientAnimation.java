package net.ty.createcraftedbeginning.content.breezes.breezecooler.client;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerMovementBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BreezeCoolerClientAnimation {
    private BreezeCoolerClientAnimation() {
    }

    public static void initialize() {
        BreezeCoolerBlockEntity.setClientTicker(BreezeCoolerClientAnimation::tick);
        BreezeCoolerMovementBehaviour.setCameraEntityProvider(context -> Minecraft.getInstance().cameraEntity);
    }

    public static void tickAnimation(BreezeCoolerBlockEntity cooler) {
        cooler.tickAnimation(getTargetAngle(cooler));
    }

    private static void tick(BreezeCoolerBlockEntity cooler) {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }

        cooler.spawnParticles();
        if (VisualizationManager.supportsVisualization(level)) {
            return;
        }

        tickAnimation(cooler);
    }

    private static float getTargetAngle(BreezeCoolerBlockEntity cooler) {
        float targetAngle = 0;
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null && !localPlayer.isInvisible()) {
            boolean virtual = cooler.isVirtual();
            BlockPos pos = cooler.getBlockPos();
            double playerX = virtual ? -4 : localPlayer.getX();
            double playerZ = virtual ? -10 : localPlayer.getZ();
            double deltaX = playerX - (pos.getX() + 0.5);
            double deltaZ = playerZ - (pos.getZ() + 0.5);
            targetAngle = AngleHelper.deg(-Mth.atan2(deltaZ, deltaX)) - 90;
        }
        float currentAngle = cooler.getHeadAngle().getValue();
        return currentAngle + AngleHelper.getShortestAngleDiff(currentAngle, targetAngle);
    }
}
