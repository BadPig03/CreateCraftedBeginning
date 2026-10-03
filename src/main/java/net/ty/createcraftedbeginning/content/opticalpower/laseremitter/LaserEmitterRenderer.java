package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.LaserBeamGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.LaserBehaviour;
import net.ty.createcraftedbeginning.content.opticalpower.laser.LaserRenderTypes;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LaserEmitterRenderer extends SafeBlockEntityRenderer<LaserEmitterBlockEntity> {
    private static final double BEAM_HALF_WIDTH = 0.09375;
    private static final double RENDER_EPSILON = 9.765625E-4;
    private static final int BEAM_RED = 212;
    private static final int BEAM_GREEN = 104;
    private static final int BEAM_BLUE = 232;
    private static final int BEAM_ALPHA = 64;

    public LaserEmitterRenderer(Context ignored) {
    }

    private static void renderQuad(VertexConsumer buffer, Matrix4f pose, Vec3 firstCorner, Vec3 secondCorner, Vec3 thirdCorner, Vec3 fourthCorner) {
        buffer.addVertex(pose, (float) firstCorner.x, (float) firstCorner.y, (float) firstCorner.z).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA);
        buffer.addVertex(pose, (float) secondCorner.x, (float) secondCorner.y, (float) secondCorner.z).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA);
        buffer.addVertex(pose, (float) thirdCorner.x, (float) thirdCorner.y, (float) thirdCorner.z).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA);
        buffer.addVertex(pose, (float) fourthCorner.x, (float) fourthCorner.y, (float) fourthCorner.z).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA);
    }

    @Override
    protected void renderSafe(LaserEmitterBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        double beamLength = blockEntity.getBeamLength();
        if (level == null || !blockEntity.isLaserActive() || beamLength <= 0) {
            return;
        }

        Direction direction = blockEntity.getLaserDirection();
        Vec3 directionVector = Vec3.atLowerCornerOf(direction.getNormal());
        Vec3 start = new Vec3(0.5, 0.5, 0.5).add(directionVector.scale(0.5 + RENDER_EPSILON));
        Vec3[] perpendicular = switch (direction.getAxis()) {
            case X -> new Vec3[]{new Vec3(0, 1, 0), new Vec3(0, 0, 1)};
            case Y -> new Vec3[]{new Vec3(1, 0, 0), new Vec3(0, 0, 1)};
            case Z -> new Vec3[]{new Vec3(1, 0, 0), new Vec3(0, 1, 0)};
        };
        Vec3 firstOffset = perpendicular[0].scale(BEAM_HALF_WIDTH);
        Vec3 secondOffset = perpendicular[1].scale(BEAM_HALF_WIDTH);
        Vec3[] offsets = {firstOffset.scale(-1).subtract(secondOffset), firstOffset.subtract(secondOffset), firstOffset.add(secondOffset), secondOffset.subtract(firstOffset)};
        Vec3[] ends = new Vec3[offsets.length];
        double maxDistance = blockEntity.getLaserRange() - RENDER_EPSILON;
        double fallbackDistance = Math.max(0, Math.min(beamLength - RENDER_EPSILON, maxDistance));
        BlockPos blockPos = blockEntity.getBlockPos();
        Vec3 blockOrigin = Vec3.atLowerCornerOf(blockPos);
        CoordinateTransform emitterTransform = SubLevelBridge.createRenderTransform(level, Vec3.atCenterOf(blockPos), partialTick);
        Vec3 worldStart = emitterTransform.transformPosition(blockOrigin.add(start));
        Vec3 worldDirection = emitterTransform.transformPosition(blockOrigin.add(start).add(directionVector)).subtract(worldStart);
        LaserBehaviour behaviour = blockEntity.getBehaviour(LaserBehaviour.TYPE);
        BlockHitResult hit = null;
        if (behaviour != null) {
            hit = behaviour.getHitResult();
        }

        Vec3 planePoint = worldStart.add(worldDirection.scale(fallbackDistance));
        Vec3 planeNormal = worldDirection;
        if (hit != null) {
            CoordinateTransform targetTransform = SubLevelBridge.createRenderTransform(level, Vec3.atCenterOf(hit.getBlockPos()), partialTick);
            planePoint = targetTransform.transformPosition(hit.getLocation());
            planeNormal = targetTransform.transformNormal(Vec3.atLowerCornerOf(hit.getDirection().getNormal()));
        }

        for (int i = 0; i < offsets.length; i++) {
            Vec3 cornerStart = start.add(offsets[i]);
            double distance = fallbackDistance;
            if (hit != null) {
                Vec3 worldCornerStart = emitterTransform.transformPosition(blockOrigin.add(cornerStart));
                distance = LaserBeamGeometry.intersectPlaneDistance(worldCornerStart, worldDirection, planePoint, planeNormal, fallbackDistance, maxDistance);
            }

            ends[i] = cornerStart.add(directionVector.scale(distance));
        }

        VertexConsumer buffer = bufferSource.getBuffer(LaserRenderTypes.LASER);
        Matrix4f pose = poseStack.last().pose();
        for (int i = 0; i < offsets.length; i++) {
            int next = (i + 1) % offsets.length;
            renderQuad(buffer, pose, start.add(offsets[i]), ends[i], ends[next], start.add(offsets[next]));
        }
    }

    @Override
    public boolean shouldRenderOffScreen(LaserEmitterBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
