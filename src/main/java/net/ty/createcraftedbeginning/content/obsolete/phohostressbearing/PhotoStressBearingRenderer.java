package net.ty.createcraftedbeginning.content.obsolete.phohostressbearing;

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
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class PhotoStressBearingRenderer extends KineticBlockEntityRenderer<PhotoStressBearingBlockEntity> {
    public PhotoStressBearingRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(@NotNull PhotoStressBearingBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        SuperByteBuffer model = getRotatedModel(be, state);

        BlockPos pos = be.getBlockPos();
        float speed = be.getSpeed();
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float offset = getRotationOffsetForPosition(be, pos, Axis.Y);
        float angle = time * speed * 3.0f / 10 % 360;

        if (speed != 0) {
            angle += offset;
            angle = angle / 180.0f * (float) Math.PI;
        }
        kineticRotationTransform(model, be, Axis.Y, angle, light);
        model.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PhotoStressBearingBlockEntity be, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.PHOTO_STRESS_BEARING_SHAFT, blockState);
    }
}
