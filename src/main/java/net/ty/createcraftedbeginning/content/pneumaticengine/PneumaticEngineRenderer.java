package net.ty.createcraftedbeginning.content.pneumaticengine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

public class PneumaticEngineRenderer extends KineticBlockEntityRenderer<PneumaticEngineBlockEntity> {
    public PneumaticEngineRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PneumaticEngineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        SuperByteBuffer model = getRotatedModel(be, state);

        final BlockPos pos = be.getBlockPos();
        float speed = be.getSpeed();
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float offset = getRotationOffsetForPosition(be, pos, Direction.Axis.Y);
        float angle = (time * speed * 3f / 10) % 360;

        if (speed != 0) {
            angle += offset;
            angle = angle / 180f * (float) Math.PI;
        }
        kineticRotationTransform(model, be, Direction.Axis.Y, angle, light);
        model.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PneumaticEngineBlockEntity be, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.PNEUMATIC_ENGINE_COGS, blockState);
    }
}
