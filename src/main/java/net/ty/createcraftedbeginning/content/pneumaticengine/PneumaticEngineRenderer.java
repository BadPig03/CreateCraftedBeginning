package net.ty.createcraftedbeginning.content.pneumaticengine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

public class PneumaticEngineRenderer extends KineticBlockEntityRenderer<PneumaticEngineBlockEntity> {
    public PneumaticEngineRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PneumaticEngineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        RenderType type = getRenderType(be, state);
        SuperByteBuffer model = getRotatedModel(be, state);

        if (be.getSpeed() != 0) {
            super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
            renderRotatingBuffer(be, model, ms, buffer.getBuffer(type), light);
        }
        else {
            model.light(light).renderInto(ms, buffer.getBuffer(type));
        }
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PneumaticEngineBlockEntity be, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.PNEUMATIC_ENGINE_COGS, blockState);
    }
}
