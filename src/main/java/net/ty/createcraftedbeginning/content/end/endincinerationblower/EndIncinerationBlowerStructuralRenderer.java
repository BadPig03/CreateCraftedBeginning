package net.ty.createcraftedbeginning.content.end.endincinerationblower;

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

public class EndIncinerationBlowerStructuralRenderer extends KineticBlockEntityRenderer<EndIncinerationBlowerStructuralBlockEntity> {
    public EndIncinerationBlowerStructuralRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(@NotNull EndIncinerationBlowerStructuralBlockEntity be, float partialTicks, PoseStack ms, @NotNull MultiBufferSource buffer, int light, int overlay) {
        SuperByteBuffer core = getRotatedModel(be, be.getBlockState());
        kineticRotationTransform(core, be, Axis.Y, getAngleForBe(be, be.getBlockPos(), Axis.Y), light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(EndIncinerationBlowerStructuralBlockEntity be, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.SHAFT_HALF_DOWN, blockState);
    }
}
