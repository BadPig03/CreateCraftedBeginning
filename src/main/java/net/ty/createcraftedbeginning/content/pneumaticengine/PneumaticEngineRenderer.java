package net.ty.createcraftedbeginning.content.pneumaticengine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PneumaticEngineRenderer extends KineticBlockEntityRenderer<PneumaticEngineBlockEntity> {
    public PneumaticEngineRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PneumaticEngineBlockEntity engine, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = engine.getBlockState();
        SuperByteBuffer model = getRotatedModel(engine, state);

        BlockPos pos = engine.getBlockPos();
        float speed = engine.getSpeed();
        float time = AnimationTickHolder.getRenderTime(engine.getLevel());
        float offset = getRotationOffsetForPosition(engine, pos, Axis.Y);
        float angle = time * speed * 3 / 10 % 360;

        if (speed != 0) {
            angle += offset;
            angle *= Mth.DEG_TO_RAD;
        }
        kineticRotationTransform(model, engine, Axis.Y, angle, light);
        model.renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PneumaticEngineBlockEntity engine, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.PNEUMATIC_ENGINE_COGS, blockState);
    }
}
