package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndIncinerationBlowerRenderer extends KineticBlockEntityRenderer<EndIncinerationBlowerBlockEntity> {
    public EndIncinerationBlowerRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(EndIncinerationBlowerBlockEntity blower, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        SuperByteBuffer core = getRotatedModel(blower, blower.getBlockState());
        kineticRotationTransform(core, blower, Axis.Y, getAngleForBe(blower, blower.getBlockPos(), Axis.Y), light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(EndIncinerationBlowerBlockEntity blower, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.END_INCINERATION_BLOWER_CORE, blockState);
    }
}
