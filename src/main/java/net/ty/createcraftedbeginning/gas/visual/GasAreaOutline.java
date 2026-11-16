package net.ty.createcraftedbeginning.gas.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BooleanSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class GasAreaOutline extends AABBOutline {
    private final Level level;
    private final Vec3 sourceCenter;
    private final boolean anchored;
    private final BooleanSupplier visible;

    public GasAreaOutline(Level level, BlockPos sourcePos, AABB localBounds, BooleanSupplier visible) {
        super(localBounds);
        this.level = level;
        this.visible = visible;
        sourceCenter = sourcePos.getCenter();
        anchored = SubLevelBridge.resolve(level, sourcePos).inSubLevel();
    }

    @Override
    public void render(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != level || minecraft.player == null || !GogglesItem.isWearingGoggles(minecraft.player) || !visible.getAsBoolean()) {
            return;
        }

        CoordinateTransform transform = SubLevelBridge.createRenderTransform(level, sourceCenter, partialTicks);
        if (anchored && !transform.inSubLevel()) {
            return;
        }

        Vec3 x = transform.transformNormal(new Vec3(1, 0, 0));
        Vec3 y = transform.transformNormal(new Vec3(0, 1, 0));
        Vec3 z = transform.transformNormal(new Vec3(0, 0, 1));
        Matrix4f basis = new Matrix4f((float) x.x, (float) x.y, (float) x.z, 0, (float) y.x, (float) y.y, (float) y.z, 0, (float) z.x, (float) z.y, (float) z.z, 0, 0, 0, 0, 1);
        Vec3 localCamera = transform.inverseTransformPosition(camera).subtract(sourceCenter);
        poseStack.pushPose();
        try {
            poseStack.mulPose(basis);
            super.render(poseStack, buffer, localCamera, partialTicks);
        }
        finally {
            poseStack.popPose();
        }
    }
}
