package net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PhotothermalReceiverRenderer extends SafeBlockEntityRenderer<PhotothermalReceiverBlockEntity> {
    private static final float SURFACE_OFFSET = 0.0005F;

    public PhotothermalReceiverRenderer(Context ignoredContext) {
    }

    @Override
    protected void renderSafe(PhotothermalReceiverBlockEntity receiver, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = receiver.getBlockState();
        VertexConsumer consumer = buffers.getBuffer(RenderType.cutoutMipped());
        for (PhotothermalReceiverPort port : PhotothermalReceiverPort.values()) {
            Direction direction = port.getDirection();
            boolean illuminated = receiver.isPortIlluminated(port);
            PartialModel model = CCBPartialModels.PHOTOTHERMAL_RECEIVER_HORIZONTAL_PORT;
            int portLight = light;
            if (illuminated) {
                model = CCBPartialModels.PHOTOTHERMAL_RECEIVER_HORIZONTAL_GLOW_PORT;
                portLight = LightTexture.FULL_BRIGHT;
            }

            poseStack.pushPose();
            if (illuminated) {
                poseStack.translate(direction.getStepX() * SURFACE_OFFSET, direction.getStepY() * SURFACE_OFFSET, direction.getStepZ() * SURFACE_OFFSET);
            }
            if (port == PhotothermalReceiverPort.DOWN) {
                model = CCBPartialModels.PHOTOTHERMAL_RECEIVER_VERTICAL_PORT;
                if (illuminated) {
                    model = CCBPartialModels.PHOTOTHERMAL_RECEIVER_VERTICAL_GLOW_PORT;
                }

                poseStack.translate(0, -SURFACE_OFFSET, 0);
            }
            else {
                float rotation = switch (direction) {
                    case WEST -> 90;
                    case SOUTH -> 180;
                    case EAST -> 270;
                    default -> 0;
                };
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
                poseStack.translate(-0.5, -0.5, -0.5);
            }

            CachedBuffers.partial(model, state).light(portLight).overlay(overlay).renderInto(poseStack, consumer);
            poseStack.popPose();
        }
    }
}
