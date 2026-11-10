package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorRenderer extends SmartBlockEntityRenderer<AmethystMirrorBlockEntity> {
    public AmethystMirrorRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(AmethystMirrorBlockEntity mirror, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = mirror.getBlockState();
        poseStack.pushPose();
        try {
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(mirror.getRotation());
            poseStack.translate(-0.5, -0.5, -0.5);
            CachedBuffers.partial(CCBPartialModels.AMETHYST_MIRROR_PANEL, state).light(light).renderInto(poseStack, buffers.getBuffer(RenderType.cutoutMipped()));
        }
        finally {
            poseStack.popPose();
        }
    }
}
