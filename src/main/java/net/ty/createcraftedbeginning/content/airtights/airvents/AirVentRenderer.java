package net.ty.createcraftedbeginning.content.airtights.airvents;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirVentRenderer extends SafeBlockEntityRenderer<AirVentBlockEntity> {
    private static final float LOUVER_SURFACE_OFFSET = 0.001953125f;

    public AirVentRenderer(Context ignored) {
    }

    private static void renderLouver(PartialModel model, BlockState state, Direction direction, float surfaceOffset, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(direction.getStepX() * surfaceOffset, direction.getStepY() * surfaceOffset, direction.getStepZ() * surfaceOffset);
        CachedBuffers.partialFacing(model, state, direction.getOpposite()).light(light).overlay(overlay).renderInto(poseStack, consumer);
        poseStack.popPose();
    }

    @Override
    protected void renderSafe(AirVentBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        int visibleLouverMask = blockEntity.getVisibleLouverMask();
        if (visibleLouverMask == 0) {
            return;
        }

        int openedLouverMask = blockEntity.getOpenedLouverMask();
        BlockState blockState = blockEntity.getBlockState();
        VertexConsumer solidBuffer = buffers.getBuffer(RenderType.solid());
        for (Direction direction : Iterate.directions) {
            int directionMask = 1 << direction.get3DDataValue();
            if ((visibleLouverMask & directionMask) == 0) {
                continue;
            }

            PartialModel louverModel = (openedLouverMask & directionMask) == 0 ? CCBPartialModels.AIR_VENT_CLOSED : CCBPartialModels.AIR_VENT_OPENED;
            renderLouver(louverModel, blockState, direction, LOUVER_SURFACE_OFFSET, poseStack, solidBuffer, light, overlay);
            renderLouver(louverModel, blockState, direction, -LOUVER_SURFACE_OFFSET, poseStack, solidBuffer, light, overlay);
        }
    }
}
