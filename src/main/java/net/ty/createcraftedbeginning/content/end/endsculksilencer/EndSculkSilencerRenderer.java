package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.client.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndSculkSilencerRenderer extends KineticBlockEntityRenderer<EndSculkSilencerBlockEntity> {
    public EndSculkSilencerRenderer(Context context) {
        super(context);
    }

    public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer, float angleDegrees) {
        SuperByteBuffer core = CachedBuffers.partial(CCBPartialModels.END_SCULK_SILENCER_CORE, context.state);
        float angle = angleDegrees * Mth.DEG_TO_RAD;
        core.transform(matrices.getModel()).translate(0, 0.5, 0).rotateCentered(angle, Axis.X).rotateCentered(angle, Axis.Y).rotateCentered(Mth.PI / 4, Axis.Z).light(LevelRenderer.getLightColor(renderWorld, context.localPos)).useLevelLight(context.world, matrices.getWorld()).renderInto(matrices.getViewProjection(), buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected void renderSafe(EndSculkSilencerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        SuperByteBuffer core = getRotatedModel(be, be.getBlockState());
        float angle = be.getAnimation().getValue(partialTicks) * Mth.DEG_TO_RAD;

        ms.pushPose();
        ms.translate(0, 0.5, 0);
        core.rotateCentered(angle, Axis.X).rotateCentered(angle, Axis.Y).rotateCentered(Mth.PI / 4, Axis.Z).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        ms.popPose();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(EndSculkSilencerBlockEntity be, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.END_SCULK_SILENCER_CORE, blockState);
    }
}
