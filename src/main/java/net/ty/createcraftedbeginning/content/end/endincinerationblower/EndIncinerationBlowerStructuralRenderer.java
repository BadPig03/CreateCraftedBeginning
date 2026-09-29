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
public class EndIncinerationBlowerStructuralRenderer extends KineticBlockEntityRenderer<EndIncinerationBlowerStructuralBlockEntity> {
    public EndIncinerationBlowerStructuralRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(EndIncinerationBlowerStructuralBlockEntity structure, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        SuperByteBuffer core = getRotatedModel(structure, structure.getBlockState());
        kineticRotationTransform(core, structure, Axis.Y, getAngleForBe(structure, structure.getBlockPos(), Axis.Y), light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(EndIncinerationBlowerStructuralBlockEntity structure, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.SHAFT_HALF_DOWN, blockState);
    }
}
