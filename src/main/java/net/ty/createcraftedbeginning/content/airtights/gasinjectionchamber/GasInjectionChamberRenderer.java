package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_IDLE_TIME;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_PART_TIME;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_TIME;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.PROCESSING_TIME;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionChamberRenderer extends SmartBlockEntityRenderer<GasInjectionChamberBlockEntity> {
    public GasInjectionChamberRenderer(Context context) {
        super(context);
    }

    public static float getNozzleSqueeze(float ticks) {
        if (ticks < 0) {
            return 0;
        }
        else if (ticks < NOZZLE_TIME) {
            return Mth.lerp((NOZZLE_TIME - ticks) / NOZZLE_TIME, -0.75f, 0);
        }
        else if (ticks < PROCESSING_TIME - NOZZLE_TIME) {
            return -0.75f;
        }
        else if (ticks < PROCESSING_TIME) {
            return Mth.lerp((PROCESSING_TIME - ticks) / NOZZLE_TIME, 0, -0.75f);
        }
        return 0;
    }

    public static float getNozzleSqueezePart(float ticks) {
        int squeezeTime = NOZZLE_PART_TIME - NOZZLE_IDLE_TIME;
        int squeezeEnd = NOZZLE_TIME + squeezeTime;
        int releaseStart = PROCESSING_TIME - NOZZLE_TIME - squeezeTime;
        int releaseEnd = PROCESSING_TIME - NOZZLE_TIME;
        if (ticks < NOZZLE_TIME) {
            return 0;
        }

        if (ticks <= squeezeEnd) {
            return Mth.lerp((squeezeEnd - ticks) / squeezeTime, -0.2f, 0);
        }

        if (ticks <= releaseStart) {
            return -0.2f;
        }

        if (ticks <= releaseEnd) {
            return Mth.lerp((releaseEnd - ticks) / squeezeTime, 0, -0.2f);
        }
        return 0;
    }

    private static void renderPart(PartialModel model, BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light) {
        CachedBuffers.partial(model, state).light(light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    private static void renderInstalledFilter(ItemStack filterStack, BlockState blockState, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.cutoutMipped());
        CachedBuffers.partial(CCBPartialModels.GAS_INJECTION_CHAMBER_FILTER, blockState).light(light).overlay(overlay).renderInto(poseStack, vertexConsumer);

        int filterColor = filterStack.getOrDefault(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_COLOR, 0xFFFFFFFF);
        CachedBuffers.partial(CCBPartialModels.GAS_INJECTION_CHAMBER_FILTER_INNER, blockState).color(filterColor >> 16 & 0xFF, filterColor >> 8 & 0xFF, filterColor & 0xFF, 0xFF).light(light).overlay(overlay).renderInto(poseStack, vertexConsumer);
    }

    @Override
    protected void renderSafe(GasInjectionChamberBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(blockEntity, partialTicks, poseStack, buffer, light, overlay);
        if (VisualizationManager.supportsVisualization(blockEntity.getLevel())) {
            return;
        }

        poseStack.pushPose();

        BlockState blockState = blockEntity.getBlockState();
        float processingTicks = blockEntity.getRenderedProcessingTicks(partialTicks);

        poseStack.translate(0, getNozzleSqueeze(processingTicks), 0);
        renderPart(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE, blockState, poseStack, buffer, light);
        poseStack.translate(0, getNozzleSqueezePart(processingTicks), 0);
        renderPart(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE_TOP, blockState, poseStack, buffer, light);
        renderPart(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE_BOTTOM, blockState, poseStack, buffer, light);

        ItemStack installedFilter = blockEntity.getInstalledFilter();
        if (!installedFilter.isEmpty()) {
            renderInstalledFilter(installedFilter, blockState, poseStack, buffer, light, overlay);
        }

        poseStack.popPose();
    }
}
