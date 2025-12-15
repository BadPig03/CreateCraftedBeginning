package net.ty.createcraftedbeginning.content.airtights.airvents;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock.VentState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class AirVentRenderer extends SmartBlockEntityRenderer<AirVentBlockEntity> {
    public AirVentRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(@NotNull AirVentBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();

        for (Direction direction : Iterate.directions) {
            VentState ventState = state.getValue(AirVentBlock.PROPERTY_BY_DIRECTION.get(direction));
            if (!ventState.canHandInteract()) {
                continue;
            }

            SuperByteBuffer ventModel = CachedBuffers.partialFacing(ventState == VentState.CLOSED ? CCBPartialModels.AIR_VENT_CLOSED : CCBPartialModels.AIR_VENT_OPENED, state, direction.getOpposite());
            ventModel.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }
}
