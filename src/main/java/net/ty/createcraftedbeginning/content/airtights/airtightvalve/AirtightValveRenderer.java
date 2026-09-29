package net.ty.createcraftedbeginning.content.airtights.airtightvalve;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
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
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightValveRenderer extends KineticBlockEntityRenderer<AirtightValveBlockEntity> {
    public AirtightValveRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(AirtightValveBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = blockEntity.getBlockState();
        BlockPos pos = blockEntity.getBlockPos();
        Axis shaftAxis = AirtightValveBlock.getShaftAxis(state);
        float angle = getAngleForBe(blockEntity, pos, shaftAxis);
        for (AxisDirection axisDirection : AxisDirection.values()) {
            Direction shaftDirection = Direction.fromAxisAndDirection(shaftAxis, axisDirection);
            SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, shaftDirection);
            kineticRotationTransform(shaft, blockEntity, shaftAxis, angle, light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
        }
    }

    @Override
    protected SuperByteBuffer getRotatedModel(AirtightValveBlockEntity blockEntity, BlockState state) {
        Direction shaftDirection = Direction.fromAxisAndDirection(AirtightValveBlock.getShaftAxis(state), AxisDirection.POSITIVE);
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, shaftDirection);
    }
}
