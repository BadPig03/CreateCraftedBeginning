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
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEngineRenderer extends KineticBlockEntityRenderer<AirtightEngineBlockEntity> {
    public AirtightEngineRenderer(Context context) {
        super(context);
    }

    private static SuperByteBuffer getPistonModel(BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.AIRTIGHT_ENGINE_PISTON, blockState);
    }

    @Override
    protected void renderSafe(AirtightEngineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState engineState = be.getBlockState();
        Axis axis = engineState.getValue(AirtightEngineBlock.AXIS);
        Direction facing = AirtightEngineBlock.getFacing(engineState);
        BlockPos enginePos = be.getBlockPos();
        SuperByteBuffer cogs = getRotatedModel(be, engineState);
        SuperByteBuffer piston = getPistonModel(engineState);
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

        float pistonPhase = be.getPistonPhase(partialTicks);
        piston.translate(0, -0.2 * Mth.sin(pistonPhase) - 0.2, 0).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
        kineticRotationTransform(cogs, be, Axis.Y, getAngleForBe(be, enginePos, axis) * rotationSign, light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(AirtightEngineBlockEntity be, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.AIRTIGHT_ENGINE_COGS, blockState);
    }
}
