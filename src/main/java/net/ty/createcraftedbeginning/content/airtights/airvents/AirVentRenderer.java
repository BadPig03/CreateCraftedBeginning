package net.ty.createcraftedbeginning.content.airtights.airvents;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirVentRenderer extends SmartBlockEntityRenderer<AirVentBlockEntity> {
    public AirVentRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(AirVentBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        int visibleMask = blockEntity.getVisibleLouverMask();
        if (visibleMask == 0) {
            return;
        }

        int openedMask = blockEntity.getOpenedLouverMask();
        BlockState state = blockEntity.getBlockState();
        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        for (Direction direction : Iterate.directions) {
            int mask = 1 << direction.get3DDataValue();
            if ((visibleMask & mask) == 0) {
                continue;
            }

            PartialModel model = (openedMask & mask) == 0 ? CCBPartialModels.AIR_VENT_CLOSED : CCBPartialModels.AIR_VENT_OPENED;
            CachedBuffers.partialFacing(model, state, direction.getOpposite()).light(light).overlay(overlay).renderInto(poseStack, consumer);
        }
    }
}
