package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightMeterRenderer<T extends AbstractAirtightMeterBlockEntity> extends SmartBlockEntityRenderer<T> {
    public AirtightMeterRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(T blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = blockEntity.getBlockState();
        float angle = blockEntity.getNeedleAngle(partialTicks) * Mth.DEG_TO_RAD;
        for (Direction direction : Iterate.horizontalDirections) {
            if (!AbstractAirtightMeterBlock.isDisplayFace(state, direction)) {
                continue;
            }

            renderNeedle(direction, angle, state, poseStack, buffer, light, overlay);
        }
    }

    private static void renderNeedle(Direction face, float angle, BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        SuperByteBuffer needle = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_METER_NEEDLE, state);

        poseStack.pushPose();

        poseStack.translate(0.5, 0.3125, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(getFaceRotation(face)));
        poseStack.mulPose(Axis.ZP.rotation(angle));
        poseStack.translate(-0.5, -0.3125, -0.5);
        needle.light(light).overlay(overlay).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));

        poseStack.popPose();
    }

    private static float getFaceRotation(Direction face) {
        return switch (face) {
            case NORTH -> 0;
            case EAST -> -90;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> throw new IllegalArgumentException("Airtight meter needle face must be horizontal; got " + face + '.');
        };
    }
}
