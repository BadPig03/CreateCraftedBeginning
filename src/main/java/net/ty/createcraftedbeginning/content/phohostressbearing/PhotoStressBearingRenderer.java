package net.ty.createcraftedbeginning.content.phohostressbearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

public class PhotoStressBearingRenderer extends KineticBlockEntityRenderer<PhotoStressBearingBlockEntity> {
    public PhotoStressBearingRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PhotoStressBearingBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
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
    protected SuperByteBuffer getRotatedModel(PhotoStressBearingBlockEntity be, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.PHOTO_STRESS_BEARING_SHAFT, blockState);
    }
}
