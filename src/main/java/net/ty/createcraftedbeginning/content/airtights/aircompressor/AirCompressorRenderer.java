package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class AirCompressorRenderer extends KineticBlockEntityRenderer<AirCompressorBlockEntity> {
    public AirCompressorRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(@NotNull AirCompressorBlockEntity be, float partialTicks, PoseStack ms, @NotNull MultiBufferSource buffer, int light, int overlay) {
        SuperByteBuffer shaft = getRotatedModel(be, be.getBlockState());
        kineticRotationTransform(shaft, be, Axis.Y, getAngleForBe(be, be.getBlockPos(), Axis.Y), light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(AirCompressorBlockEntity be, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.SHAFT_HALF_UP, blockState);
    }
}
