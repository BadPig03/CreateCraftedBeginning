package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_IDLE_TIME;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_PART_TIME;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_TIME;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionChamberRenderer extends SmartBlockEntityRenderer<GasInjectionChamberBlockEntity> {
    public GasInjectionChamberRenderer(Context context) {
        super(context);
    }

    private static float getNozzleSqueeze(float ticks) {
        int processingTime = GasInjectionChamberBlockEntity.PROCESSING_TIME;
        if (ticks < 0) {
            return 0;
        }
        if (ticks < NOZZLE_TIME) {
            return Mth.lerp((NOZZLE_TIME - ticks) / NOZZLE_TIME, -0.75f, 0);
        }
        if (ticks < processingTime - NOZZLE_TIME) {
            return -0.75f;
        }
        if (ticks < processingTime) {
            return Mth.lerp((processingTime - ticks) / NOZZLE_TIME, 0, -0.75f);
        }
        return 0;
    }

    private static float getNozzleSqueezePart(float ticks) {
        int processingTime = GasInjectionChamberBlockEntity.PROCESSING_TIME;
        int squeezeTime = NOZZLE_PART_TIME - NOZZLE_IDLE_TIME;
        if (ticks < NOZZLE_TIME) {
            return 0;
        }
        if (ticks <= NOZZLE_TIME + squeezeTime) {
            return Mth.lerp((NOZZLE_TIME + squeezeTime - ticks) / squeezeTime, -0.2f, 0);
        }
        if (ticks <= processingTime - NOZZLE_TIME - squeezeTime) {
            return -0.2f;
        }
        if (ticks <= processingTime - NOZZLE_TIME) {
            return Mth.lerp((processingTime - NOZZLE_TIME - ticks) / squeezeTime, 0, -0.2f);
        }
        return 0;
    }

    private static void renderPart(PartialModel model, BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light) {
        CachedBuffers.partial(model, state).light(light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected void renderSafe(GasInjectionChamberBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(blockEntity, partialTicks, poseStack, buffer, light, overlay);
        poseStack.pushPose();

        BlockState state = blockEntity.getBlockState();
        float ticks = blockEntity.getProcessingTicks() - partialTicks;

        poseStack.translate(0, getNozzleSqueeze(ticks), 0);
        renderPart(CCBPartialModels.NOZZLE, state, poseStack, buffer, light);
        poseStack.translate(0, getNozzleSqueezePart(ticks), 0);
        renderPart(CCBPartialModels.NOZZLE_TOP, state, poseStack, buffer, light);
        renderPart(CCBPartialModels.NOZZLE_BOTTOM, state, poseStack, buffer, light);

        poseStack.popPose();
    }
}
