package net.ty.createcraftedbeginning.content.gasinjectionchamber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

import static net.ty.createcraftedbeginning.content.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_IDLE_TIME;
import static net.ty.createcraftedbeginning.content.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_PART_TIME;
import static net.ty.createcraftedbeginning.content.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_TIME;
import static net.ty.createcraftedbeginning.content.gasinjectionchamber.GasInjectionChamberBlockEntity.PROCESSING_TIME;

public class GasInjectionChamberRenderer extends SmartBlockEntityRenderer<GasInjectionChamberBlockEntity> {

    public GasInjectionChamberRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(GasInjectionChamberBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        ms.pushPose();

        BlockState state = be.getBlockState();

        int processingTicks = be.processingTicks;
        float pt = processingTicks - partialTicks;

        PartialModel nozzle = CCBPartialModels.NOZZLE;
        PartialModel nozzleTop = CCBPartialModels.NOZZLE_TOP;
        PartialModel nozzleBottom = CCBPartialModels.NOZZLE_BOTTOM;

        ms.translate(0, getNozzleSqueeze(pt), 0);
        CachedBuffers.partial(nozzle, state).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        ms.translate(0, getNozzleSqueezePart(pt), 0);
        CachedBuffers.partial(nozzleTop, state).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        CachedBuffers.partial(nozzleBottom, state).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

        ms.popPose();
    }

    private float getNozzleSqueeze(float partialTicks) {
        if (partialTicks < 0) {
            return 0;
        } else if (partialTicks < NOZZLE_TIME) {
            return Mth.lerp((NOZZLE_TIME - partialTicks) / NOZZLE_TIME, -0.75f, 0);
        } else if (partialTicks < PROCESSING_TIME - NOZZLE_TIME) {
            return -0.75f;
        } else if (partialTicks < PROCESSING_TIME) {
            return Mth.lerp((PROCESSING_TIME - partialTicks) / NOZZLE_TIME, 0, -0.75f);
        }
        return 0;
    }

    private float getNozzleSqueezePart(float partialTicks) {
        if (partialTicks < NOZZLE_TIME) {
            return 0;
        } else if (partialTicks <= NOZZLE_TIME + NOZZLE_PART_TIME - NOZZLE_IDLE_TIME) {
            return Mth.lerp((NOZZLE_TIME + NOZZLE_PART_TIME - NOZZLE_IDLE_TIME - partialTicks) / (NOZZLE_TIME - NOZZLE_IDLE_TIME), -0.2f, 0);
        } else if (partialTicks <= PROCESSING_TIME - NOZZLE_TIME - NOZZLE_PART_TIME + NOZZLE_IDLE_TIME) {
            return -0.2f;
        } else if (partialTicks <= PROCESSING_TIME - NOZZLE_TIME) {
            return Mth.lerp((PROCESSING_TIME - NOZZLE_TIME - partialTicks) / (NOZZLE_TIME - NOZZLE_IDLE_TIME), 0, -0.2f);
        } else if (partialTicks <= PROCESSING_TIME) {
            return 0;
        }
        return 0;
    }
}
