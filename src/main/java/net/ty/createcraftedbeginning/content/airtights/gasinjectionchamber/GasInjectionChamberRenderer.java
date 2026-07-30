package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

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

    private static float getNozzleSqueeze(float ticks) {
        if (ticks < 0) {
            return 0;
        }
        if (ticks < NOZZLE_TIME) {
            return Mth.lerp((NOZZLE_TIME - ticks) / NOZZLE_TIME, -0.75f, 0);
        }
        if (ticks < PROCESSING_TIME - NOZZLE_TIME) {
            return -0.75f;
        }
        if (ticks < PROCESSING_TIME) {
            return Mth.lerp((PROCESSING_TIME - ticks) / NOZZLE_TIME, 0, -0.75f);
        }
        return 0;
    }

    private static float getNozzleSqueezePart(float ticks) {
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

    private static void renderInstalledFilter(ItemStack filter, BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.cutoutMipped());
        CachedBuffers.partial(CCBPartialModels.GAS_INJECTION_CHAMBER_FILTER, state).light(light).overlay(overlay).renderInto(poseStack, vertexConsumer);

        int color = filter.getOrDefault(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_COLOR, 0xFFFFFFFF);
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        CachedBuffers.partial(CCBPartialModels.GAS_INJECTION_CHAMBER_FILTER_INNER, state).color(red, green, blue, 0xFF).light(light).overlay(overlay).renderInto(poseStack, vertexConsumer);
    }

    @Override
    protected void renderSafe(GasInjectionChamberBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(blockEntity, partialTicks, poseStack, buffer, light, overlay);
        poseStack.pushPose();

        BlockState state = blockEntity.getBlockState();
        float ticks = blockEntity.getRenderedProcessingTicks(partialTicks);

        poseStack.translate(0, getNozzleSqueeze(ticks), 0);
        renderPart(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE, state, poseStack, buffer, light);
        poseStack.translate(0, getNozzleSqueezePart(ticks), 0);
        renderPart(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE_TOP, state, poseStack, buffer, light);
        renderPart(CCBPartialModels.GAS_INJECTION_CHAMBER_NOZZLE_BOTTOM, state, poseStack, buffer, light);

        ItemStack filter = blockEntity.getInstalledFilter();
        if (!filter.isEmpty()) {
            renderInstalledFilter(filter, state, poseStack, buffer, light, overlay);
        }

        poseStack.popPose();
    }
}
