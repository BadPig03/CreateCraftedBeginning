package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.client.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleStructuralCogRenderer extends KineticBlockEntityRenderer<AirtightReactorKettleStructuralCogBlockEntity> {
    public AirtightReactorKettleStructuralCogRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(AirtightReactorKettleStructuralCogBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = blockEntity.getBlockState();
        AirtightReactorKettleStructuralPosition position = state.getValue(AirtightReactorKettleStructuralCogBlock.STRUCTURAL_POSITION);
        if (position == AirtightReactorKettleStructuralPosition.TOP_CENTER) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        SuperByteBuffer cogsModel = getRotatedModel(blockEntity, state);
        kineticRotationTransform(cogsModel, blockEntity, Axis.Y, getAngleForBe(blockEntity, pos, Axis.Y), light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(AirtightReactorKettleStructuralCogBlockEntity blockEntity, BlockState state) {
        return CachedBuffers.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_COGS, state);
    }
}
