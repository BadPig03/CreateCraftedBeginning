package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

public class TeslaTurbineRenderer extends KineticBlockEntityRenderer<TeslaTurbineBlockEntity> {
    public TeslaTurbineRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(TeslaTurbineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        BlockState state = be.getBlockState();
        Axis axis = state.getValue(TeslaTurbineBlock.AXIS);
        SuperByteBuffer shaft = getRotatedModel(be, state);
        float angle = getAngleForBe(be, be.getBlockPos(), axis);
        if (axis == Axis.Z) {
            shaft.rotateCentered(Mth.HALF_PI, Axis.X);
        }
        else if (axis == Axis.X) {
            shaft.rotateCentered(-Mth.HALF_PI, Axis.Z);
        }
        kineticRotationTransform(shaft, be, Axis.Y, angle, light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

        int rotorCount = state.getValue(TeslaTurbineBlock.ROTOR);
        if (rotorCount == 0) {
            return;
        }

        float spacing = 14.0f / (rotorCount + 1);
        for (int i = 0; i < rotorCount; i++) {
            SuperByteBuffer rotor = getRotorModel(state, axis);
            rotor.translate(0, (spacing * (i + 1) - 7.0f) / 16.0f, 0);
            kineticRotationTransform(rotor, be, Axis.Y, angle, light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        }
    }

    @Override
    protected SuperByteBuffer getRotatedModel(TeslaTurbineBlockEntity be, @NotNull BlockState blockState) {
        return CachedBuffers.partial(AllPartialModels.SHAFT, blockState);
    }

    protected SuperByteBuffer getRotorModel(BlockState state, Axis axis) {
        SuperByteBuffer rotor = CachedBuffers.partial(CCBPartialModels.TESLA_TURBINE_ROTOR, state);
        if (axis == Axis.Z) {
            rotor.rotateCentered(Mth.HALF_PI, Axis.X);
        }
        else if (axis == Axis.X) {
            rotor.rotateCentered(-Mth.HALF_PI, Axis.Z);
        }
        return rotor;
    }
}
