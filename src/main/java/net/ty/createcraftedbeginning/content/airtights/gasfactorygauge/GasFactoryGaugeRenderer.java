package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.RenderTypes;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFactoryGaugeRenderer extends SmartBlockEntityRenderer<GasFactoryGaugeBlockEntity> {
    public GasFactoryGaugeRenderer(Context context) {
        super(context);
    }

    private static void renderBulb(FactoryPanelBehaviour behaviour, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        BlockState blockState = behaviour.blockEntity.getBlockState();
        float xRot = FactoryPanelBlock.getXRot(blockState) + Mth.PI / 2;
        float yRot = FactoryPanelBlock.getYRot(blockState);
        float glow = behaviour.bulb.getValue(partialTicks);
        boolean missingAddress = behaviour.isMissingAddress();
        PartialModel bulb = behaviour.redstonePowered || missingAddress ? CCBPartialModels.GAS_FACTORY_GAUGE_BULB_RED : CCBPartialModels.GAS_FACTORY_GAUGE_BULB_LIGHT;
        CachedBuffers.partial(bulb, blockState).rotateCentered(yRot, Direction.UP).rotateCentered(xRot, Direction.EAST).rotateCentered(Mth.PI, Direction.UP).translate(behaviour.slot.xOffset * 0.5, 0, behaviour.slot.yOffset * 0.5).light(glow > 0.125f ? LightTexture.FULL_BRIGHT : light).overlay(overlay).renderInto(poseStack, buffer.getBuffer(RenderType.translucent()));
        if (glow < 0.125f) {
            return;
        }

        glow = Mth.clamp(1 - 2 * Mth.square(glow - 0.75f), -1, 1);
        int color = (int) (200 * glow);
        CachedBuffers.partial(bulb, blockState).rotateCentered(yRot, Direction.UP).rotateCentered(xRot, Direction.EAST).rotateCentered(Mth.PI, Direction.UP).translate(behaviour.slot.xOffset * 0.5, 0, behaviour.slot.yOffset * 0.5).light(LightTexture.FULL_BRIGHT).color(color, color, color, 255).overlay(overlay).renderInto(poseStack, buffer.getBuffer(RenderTypes.additive()));
    }

    @Override
    protected void renderSafe(GasFactoryGaugeBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(blockEntity, partialTicks, poseStack, buffer, light, overlay);
        for (FactoryPanelBehaviour behaviour : blockEntity.panels.values()) {
            if (!behaviour.isActive()) {
                continue;
            }

            if (behaviour.getAmount() > 0) {
                renderBulb(behaviour, partialTicks, poseStack, buffer, light, overlay);
            }
            for (FactoryPanelConnection connection : behaviour.targetedBy.values()) {
                FactoryPanelRenderer.renderPath(behaviour, connection, partialTicks, poseStack, buffer, light, overlay);
            }

            for (FactoryPanelConnection connection : behaviour.targetedByLinks.values()) {
                FactoryPanelRenderer.renderPath(behaviour, connection, partialTicks, poseStack, buffer, light, overlay);
            }
        }
    }
}
