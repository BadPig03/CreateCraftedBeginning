package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressStructuralShaftRenderer extends KineticBlockEntityRenderer<AirtightForgingPressStructuralShaftBlockEntity> {
    public AirtightForgingPressStructuralShaftRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(AirtightForgingPressStructuralShaftBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = blockEntity.getBlockState();
        BlockPos pos = blockEntity.getBlockPos();
        AirtightForgingPressStructuralPosition position = state.getValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION);
        if (position == AirtightForgingPressStructuralPosition.TOP_CENTER) {
            return;
        }

        Axis axis = position.getAxis();
        AxisDirection axisDirection = position.getAxisDirection();
        SuperByteBuffer shaft = getRotatedModel(blockEntity, state);
        if (axis == Axis.X) {
            shaft.rotateCentered(-Mth.HALF_PI, Direction.fromAxisAndDirection(Axis.Z, axisDirection));
        }
        else {
            shaft.rotateCentered(Mth.HALF_PI, Direction.fromAxisAndDirection(Axis.X, axisDirection));
        }

        float angle = getAngleForBe(blockEntity, pos, axis) * axisDirection.getStep();
        kineticRotationTransform(shaft, blockEntity, Axis.Y, angle, light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(AirtightForgingPressStructuralShaftBlockEntity blockEntity, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.SHAFT_HALF_UP, blockState);
    }
}
