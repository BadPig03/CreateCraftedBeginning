package net.ty.createcraftedbeginning.content.airtights.airtightengine;

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
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEngineRenderer extends KineticBlockEntityRenderer<AirtightEngineBlockEntity> {
    public AirtightEngineRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(AirtightEngineBlockEntity engine, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        BlockState engineState = engine.getBlockState();
        Axis axis = engineState.getValue(AirtightEngineBlock.AXIS);
        Direction facing = AirtightEngineBlock.getFacing(engineState);
        BlockPos enginePos = engine.getBlockPos();
        SuperByteBuffer cogs = getRotatedModel(engine, engineState);
        SuperByteBuffer piston = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_ENGINE_PISTON, engineState);
        int rotationSign = facing.getAxisDirection() == AxisDirection.NEGATIVE ? 1 : -1;
        if (axis == Axis.X) {
            piston.rotateCentered(-Mth.HALF_PI * rotationSign, Axis.Z);
            cogs.rotateCentered(-Mth.HALF_PI * rotationSign, Axis.Z);
        }
        else if (axis == Axis.Z) {
            piston.rotateCentered(Mth.HALF_PI * rotationSign, Axis.X);
            cogs.rotateCentered(Mth.HALF_PI * rotationSign, Axis.X);
        }
        else if (facing == Direction.UP) {
            piston.rotateCentered(Mth.PI, Axis.X);
            cogs.rotateCentered(Mth.PI, Axis.X);
        }

        float pistonPhase = engine.getPistonPhase(partialTicks);
        piston.translate(0, -0.2 * Mth.sin(pistonPhase) - 0.2, 0).light(light).renderInto(poseStack, buffer.getBuffer(RenderType.solid()));
        kineticRotationTransform(cogs, engine, Axis.Y, getAngleForBe(engine, enginePos, axis) * rotationSign, light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(AirtightEngineBlockEntity engine, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.AIRTIGHT_ENGINE_COGS, blockState);
    }

}
