package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.LaserRenderTypes;
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

    @Override
    protected void renderSafe(LaserEmitterBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!blockEntity.isLaserActive() || blockEntity.getBeamLength() <= 0) {
            return;
        }

        Direction direction = blockEntity.getLaserDirection();
        Vec3 directionVector = Vec3.atLowerCornerOf(direction.getNormal());
        Vec3 start = new Vec3(0.5, 0.5, 0.5).add(directionVector.scale(0.5 + RENDER_EPSILON));
        Vec3[] perpendicular = perpendicularAxes(direction);
        renderBeam(bufferSource.getBuffer(LaserRenderTypes.LASER), poseStack.last().pose(), start, start.add(directionVector.scale(Math.max(0, blockEntity.getBeamLength() - RENDER_EPSILON))), perpendicular[0], perpendicular[1]);
    }

    @Override
    public boolean shouldRenderOffScreen(LaserEmitterBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    private static Vec3[] perpendicularAxes(Direction direction) {
        return switch (direction.getAxis()) {
            case X -> new Vec3[]{new Vec3(0, 1, 0), new Vec3(0, 0, 1)};
            case Y -> new Vec3[]{new Vec3(1, 0, 0), new Vec3(0, 0, 1)};
            case Z -> new Vec3[]{new Vec3(1, 0, 0), new Vec3(0, 1, 0)};
        };
    }

    private static void renderBeam(VertexConsumer buffer, Matrix4f pose, Vec3 start, Vec3 end, Vec3 axisA, Vec3 axisB) {
        Vec3 firstOffset = axisA.scale(BEAM_HALF_WIDTH);
        Vec3 secondOffset = axisB.scale(BEAM_HALF_WIDTH);
        Vec3[] offsets = {firstOffset.scale(-1).subtract(secondOffset), firstOffset.subtract(secondOffset), firstOffset.add(secondOffset), secondOffset.subtract(firstOffset)};
        for (int i = 0; i < offsets.length; i++) {
            Vec3 current = offsets[i];
            Vec3 next = offsets[(i + 1) % offsets.length];
            renderQuad(buffer, pose, start.add(current), end.add(current), end.add(next), start.add(next));
        }
        renderQuad(buffer, pose, end.add(offsets[0]), end.add(offsets[1]), end.add(offsets[2]), end.add(offsets[3]));
    }

    private static void renderQuad(VertexConsumer buffer, Matrix4f pose, Vec3 firstCorner, Vec3 secondCorner, Vec3 thirdCorner, Vec3 fourthCorner) {
        buffer.addVertex(pose, (float) firstCorner.x, (float) firstCorner.y, (float) firstCorner.z).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA);
        buffer.addVertex(pose, (float) secondCorner.x, (float) secondCorner.y, (float) secondCorner.z).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA);
        buffer.addVertex(pose, (float) thirdCorner.x, (float) thirdCorner.y, (float) thirdCorner.z).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA);
        buffer.addVertex(pose, (float) fourthCorner.x, (float) fourthCorner.y, (float) fourthCorner.z).setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA);
    }
}
