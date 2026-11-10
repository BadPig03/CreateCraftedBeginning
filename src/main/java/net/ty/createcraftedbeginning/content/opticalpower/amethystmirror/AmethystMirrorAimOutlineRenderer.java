package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import com.simibubi.create.content.logistics.packagePort.PackagePortTargetSelectionHandler;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorAimOutlineRenderer {
    private static final Object SOURCE = new Object();
    private static final Object TARGET = new Object();
    private static final int VALID_COLOR = 0x9EDE73;
    private static final int INVALID_COLOR = 0xFF7171;

    private AmethystMirrorAimOutlineRenderer() {
    }

    static void tick(@Nullable AmethystMirrorAimResult preview) {
        if (preview == null || preview.status() != AmethystMirrorAimStatus.READY) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Color color = new Color(VALID_COLOR);
        Vec3 reflection = preview.reflectionPoint();
        PackagePortTargetSelectionHandler.animateConnection(minecraft, preview.incomingStart(), reflection, color);
        PackagePortTargetSelectionHandler.animateConnection(minecraft, reflection, preview.targetPoint(), color);
    }

    static void render(BlockPos pos, @Nullable BlockHitResult hit, boolean valid) {
        Outliner outliner = Outliner.getInstance();
        int color = INVALID_COLOR;
        if (valid) {
            color = VALID_COLOR;
        }
        outliner.showAABB(SOURCE, new AABB(pos).inflate(0.03125)).colored(color).lineWidth(0.03125F).disableLineNormals();
        if (hit == null || hit.getType() != Type.BLOCK) {
            outliner.remove(TARGET);
            return;
        }

        Vec3 target = hit.getLocation();
        outliner.chaseAABB(TARGET, new AABB(target, target)).colored(color).lineWidth(0.2F).disableLineNormals();
    }

    static void clear() {
        Outliner outliner = Outliner.getInstance();
        outliner.remove(SOURCE);
        outliner.remove(TARGET);
    }
}
