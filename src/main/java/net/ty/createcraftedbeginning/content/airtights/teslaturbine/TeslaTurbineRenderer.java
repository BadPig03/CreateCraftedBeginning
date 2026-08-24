package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineRenderer extends KineticBlockEntityRenderer<TeslaTurbineBlockEntity> {
    public TeslaTurbineRenderer(Context context) {
        super(context);
    }

    private static SuperByteBuffer rotateToAxis(SuperByteBuffer buffer, Axis axis) {
        switch (axis) {
            case Z -> buffer.rotateCentered(Mth.HALF_PI, Axis.X);
            case X -> buffer.rotateCentered(-Mth.HALF_PI, Axis.Z);
        }
        return buffer;
    }

    private static SuperByteBuffer getRotorModel(BlockState blockState, Axis rotationAxis) {
        return rotateToAxis(CachedBuffers.partial(CCBPartialModels.TESLA_TURBINE_ROTOR, blockState), rotationAxis);
    }

    @Override
    protected void renderSafe(TeslaTurbineBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(blockEntity, partialTicks, poseStack, buffer, light, overlay);
        BlockState blockState = blockEntity.getBlockState();
        Axis rotationAxis = blockState.getValue(TeslaTurbineBlock.AXIS);
        SuperByteBuffer shaft = rotateToAxis(getRotatedModel(blockEntity, blockState), rotationAxis);
        float rotationAngle = getAngleForBe(blockEntity, blockEntity.getBlockPos(), rotationAxis);
        kineticRotationTransform(shaft, blockEntity, Axis.Y, rotationAngle, light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));

        int rotorCount = blockState.getValue(TeslaTurbineBlock.ROTOR);
        if (rotorCount == 0) {
            return;
        }

        float rotorSpacing = 14.0f / (rotorCount + 1);
        for (int rotorIndex = 0; rotorIndex < rotorCount; rotorIndex++) {
            SuperByteBuffer rotorBuffer = getRotorModel(blockState, rotationAxis);
            rotorBuffer.translate(0, (rotorSpacing * (rotorIndex + 1) - 7) / 16, 0);
            kineticRotationTransform(rotorBuffer, blockEntity, Axis.Y, rotationAngle, light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
        }
    }

    @Override
    protected SuperByteBuffer getRotatedModel(TeslaTurbineBlockEntity blockEntity, BlockState blockState) {
        return CachedBuffers.partial(AllPartialModels.SHAFT, blockState);
    }
}
