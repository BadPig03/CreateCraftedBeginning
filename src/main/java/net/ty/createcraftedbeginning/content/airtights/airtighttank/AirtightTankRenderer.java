package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightTankRenderer<T extends AirtightTankBlockEntity> extends SmartBlockEntityRenderer<T> {

    public AirtightTankRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(T tank, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        if (!tank.isController() || !tank.hasTankGauge()) {
            return;
        }

        Direction.Axis mainAxis = tank.getMainConnectionAxis();
        int xSize = mainAxis == Direction.Axis.X ? tank.getHeight() : tank.getWidth();
        int zSize = mainAxis == Direction.Axis.Z ? tank.getHeight() : tank.getWidth();
        float needleAngle = tank.getTankGaugeNeedleAngle(partialTicks);
        BlockState state = tank.getBlockState();
        for (Direction direction : Iterate.horizontalDirections) {
            if (tank.isTankGaugeOccluded(direction)) {
                continue;
            }

            renderGauge(direction, xSize, zSize, needleAngle, state, poseStack, buffer, light, overlay);
        }
    }

    @SuppressWarnings("ConstantExpression")
    private static void renderGauge(Direction direction, int xSize, int zSize, float needleAngle, BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        double x = switch (direction) {
            case EAST -> xSize - 0.9375;
            case WEST -> -0.0625;
            default -> xSize / 2.0 - 0.5;
        };
        double z = switch (direction) {
            case SOUTH -> zSize - 0.9375;
            case NORTH -> -0.0625;
            default -> zSize / 2.0 - 0.5;
        };

        poseStack.pushPose();

        poseStack.translate(x, 0, z);
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(faceRotation(direction)));
        poseStack.translate(-0.5, -0.5, -0.5);

        CachedBuffers.partial(CCBPartialModels.AIRTIGHT_TANK_GAUGE, state).light(light).overlay(overlay).renderInto(poseStack, buffer.getBuffer(RenderType.cutout()));

        poseStack.pushPose();

        poseStack.translate(0, 0.375F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(needleAngle));
        poseStack.translate(0, -0.375F, -0.5F);
        CachedBuffers.partial(CCBPartialModels.AIRTIGHT_TANK_GAUGE_NEEDLE, state).light(light).overlay(overlay).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));

        poseStack.popPose();

        poseStack.popPose();
    }

    private static float faceRotation(Direction direction) {
        if (direction.getAxisDirection() == AxisDirection.POSITIVE) {
            if (direction.getAxis() != Direction.Axis.X) {
                return -90;
            }

            return 0;
        }

        if (direction.getAxis() != Direction.Axis.X) {
            return 90;
        }

        return 180;
    }
}
