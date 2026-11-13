package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ChunkStabilizerRenderer extends SmartBlockEntityRenderer<ChunkStabilizerBlockEntity> {
    public ChunkStabilizerRenderer(Context context) {
        super(context);
    }

    static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffers, float angle) {
        CachedBuffers.partial(CCBPartialModels.CHUNK_STABILIZER_CORE, context.state).transform(matrices.getModel()).rotateCentered(angle * Mth.DEG_TO_RAD, Axis.Y).light(LevelRenderer.getLightColor(renderWorld, context.localPos)).useLevelLight(context.world, matrices.getWorld()).renderInto(matrices.getViewProjection(), buffers.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected void renderSafe(ChunkStabilizerBlockEntity loader, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        CachedBuffers.partial(CCBPartialModels.CHUNK_STABILIZER_CORE, loader.getBlockState()).rotateCentered(loader.animationAngle(partialTick) * Mth.DEG_TO_RAD, Axis.Y).light(light).renderInto(poseStack, buffers.getBuffer(RenderType.cutoutMipped()));
        Level level = loader.getLevel();
        if (!loader.isVirtual() || !loader.isLoadingActive() || !loader.beamEnabled() || level == null) {
            return;
        }

        poseStack.pushPose();
        try {
            poseStack.translate(0, 0.5, 0);
            ChunkStabilizerBeamRenderer.renderBeam(poseStack, buffers, AnimationTickHolder.getTicks(level), partialTick, loader.beamColor());
        }
        finally {
            poseStack.popPose();
        }
    }
}
