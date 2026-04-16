package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class EndSculkSilencerRenderer extends KineticBlockEntityRenderer<EndSculkSilencerBlockEntity> {
    public EndSculkSilencerRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(@NotNull EndSculkSilencerBlockEntity be, float partialTicks, @NotNull PoseStack ms, @NotNull MultiBufferSource buffer, int light, int overlay) {
        SuperByteBuffer core = getRotatedModel(be, be.getBlockState());
        float angle = be.getAnimation().getValue(partialTicks) * Mth.DEG_TO_RAD;

        ms.pushPose();
        ms.translate(0, 0.5f, 0);
        core.rotateCentered(angle, Axis.X).rotateCentered(angle, Axis.Y).rotateCentered(Mth.sin(Mth.PI / 4), Axis.Z).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        ms.popPose();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(EndSculkSilencerBlockEntity be, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.END_SCULK_SILENCER_CORE, blockState);
    }
}
