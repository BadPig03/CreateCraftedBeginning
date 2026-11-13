package net.ty.createcraftedbeginning.content.opticalpower.photothermalchamber;

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
public final class PhotothermalChamberRenderer extends SafeBlockEntityRenderer<PhotothermalChamberBlockEntity> {
    private static final float SURFACE_OFFSET = 0.0005F;

    public PhotothermalChamberRenderer(Context ignoredContext) {
    }

    @Override
    protected void renderSafe(PhotothermalChamberBlockEntity chamber, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = chamber.getBlockState();
        VertexConsumer consumer = buffers.getBuffer(RenderType.cutoutMipped());
        for (PhotothermalChamberPort port : PhotothermalChamberPort.getPorts()) {
            Direction direction = port.getDirection();
            boolean illuminated = chamber.isPortIlluminated(port);
            PartialModel model = CCBPartialModels.PHOTOTHERMAL_CHAMBER_HORIZONTAL_PORT;
            int portLight = light;
            if (illuminated) {
                model = CCBPartialModels.PHOTOTHERMAL_CHAMBER_HORIZONTAL_GLOW_PORT;
                portLight = LightTexture.FULL_BRIGHT;
            }

            poseStack.pushPose();
            try {
                if (illuminated) {
                    poseStack.translate(direction.getStepX() * SURFACE_OFFSET, direction.getStepY() * SURFACE_OFFSET, direction.getStepZ() * SURFACE_OFFSET);
                }
                if (port == PhotothermalChamberPort.DOWN) {
                    model = CCBPartialModels.PHOTOTHERMAL_CHAMBER_VERTICAL_PORT;
                    if (illuminated) {
                        model = CCBPartialModels.PHOTOTHERMAL_CHAMBER_VERTICAL_GLOW_PORT;
                    }

                    poseStack.translate(0, -SURFACE_OFFSET, 0);
                    poseStack.translate(0.5, 0.5, 0.5);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180 - state.getValue(PhotothermalChamberBlock.FACING).toYRot()));
                    poseStack.translate(-0.5, -0.5, -0.5);
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
            }
            finally {
                poseStack.popPose();
            }
        }
    }
}
