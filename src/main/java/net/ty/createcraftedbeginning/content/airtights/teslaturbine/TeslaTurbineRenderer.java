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
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineRenderer extends KineticBlockEntityRenderer<TeslaTurbineBlockEntity> {
    public TeslaTurbineRenderer(Context context) {
        super(context);
    }

    private static SuperByteBuffer rotateToAxis(SuperByteBuffer buffer, Axis axis) {
        if (axis == Axis.Z) {
            buffer.rotateCentered(Mth.HALF_PI, Axis.X);
        }
        else if (axis == Axis.X) {
            buffer.rotateCentered(-Mth.HALF_PI, Axis.Z);
        }
        return buffer;
    }

    @Override
    protected void renderSafe(TeslaTurbineBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(blockEntity, partialTicks, poseStack, buffer, light, overlay);
        BlockState state = blockEntity.getBlockState();
        Axis axis = state.getValue(TeslaTurbineBlock.AXIS);
        SuperByteBuffer shaft = rotateToAxis(getRotatedModel(blockEntity, state), axis);
        float angle = getAngleForBe(blockEntity, blockEntity.getBlockPos(), axis);
        kineticRotationTransform(shaft, blockEntity, Axis.Y, angle, light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));

        int rotorCount = state.getValue(TeslaTurbineBlock.ROTOR);
        if (rotorCount == 0) {
            return;
        }

        float spacing = 14.0f / (rotorCount + 1);
        for (int i = 0; i < rotorCount; i++) {
            SuperByteBuffer rotor = getRotorModel(state, axis);
            rotor.translate(0, (spacing * (i + 1) - 7.0f) / 16.0f, 0);
            kineticRotationTransform(rotor, blockEntity, Axis.Y, angle, light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
        }
    }

    @Override
    protected SuperByteBuffer getRotatedModel(TeslaTurbineBlockEntity blockEntity, BlockState blockState) {
        return CachedBuffers.partial(AllPartialModels.SHAFT, blockState);
    }

    protected SuperByteBuffer getRotorModel(BlockState state, Axis axis) {
        SuperByteBuffer rotor = CachedBuffers.partial(CCBPartialModels.TESLA_TURBINE_ROTOR, state);
        return rotateToAxis(rotor, axis);
    }
}
