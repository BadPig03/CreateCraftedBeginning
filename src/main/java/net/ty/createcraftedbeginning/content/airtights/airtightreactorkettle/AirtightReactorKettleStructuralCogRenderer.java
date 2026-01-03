package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class AirtightReactorKettleStructuralCogRenderer extends KineticBlockEntityRenderer<AirtightReactorKettleStructuralCogBlockEntity> {
    public AirtightReactorKettleStructuralCogRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(@NotNull AirtightReactorKettleStructuralCogBlockEntity be, float partialTicks, PoseStack ms, @NotNull MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (state.getValue(AirtightReactorKettleStructuralCogBlock.STRUCTURAL_POSITION) == AirtightReactorKettleStructuralPosition.TOP_CENTER) {
            return;
        }

        BlockPos pos = be.getBlockPos();
        SuperByteBuffer cogsModel = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_REACTOR_KETTLE_COGS, state);
        kineticRotationTransform(cogsModel, be, Axis.Y, getAngleForBe(be, pos, Axis.Y), light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }
}
