package net.ty.createcraftedbeginning.content.airtights.airtightengine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
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
import org.jetbrains.annotations.NotNull;

import static net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlockEntity.DELTA_TIME;

public class AirtightEngineRenderer extends KineticBlockEntityRenderer<AirtightEngineBlockEntity> {
    public AirtightEngineRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(@NotNull AirtightEngineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Axis axis = state.getValue(AirtightEngineBlock.AXIS);
        Direction direction = AirtightEngineBlock.getFacing(state);
        BlockPos pos = be.getBlockPos();
        SuperByteBuffer cogsModel = getRotatedModel(be, state);
        SuperByteBuffer pistonModel = getPistonModel(state);
        int directionModifier = direction.getAxisDirection() == AxisDirection.NEGATIVE ? 1 : -1;
        if (axis == Axis.X) {
            pistonModel.rotateCentered(-Mth.HALF_PI * directionModifier, Axis.Z);
            cogsModel.rotateCentered(-Mth.HALF_PI * directionModifier, Axis.Z);
        }
        else if (axis == Axis.Z) {
            pistonModel.rotateCentered(Mth.HALF_PI * directionModifier, Axis.X);
            cogsModel.rotateCentered(Mth.HALF_PI * directionModifier, Axis.X);
        }
        else if (direction == Direction.UP) {
            pistonModel.rotateCentered(Mth.PI, Axis.X);
            cogsModel.rotateCentered(Mth.PI, Axis.X);
        }

        float newPhase = Mth.lerp(0.1f, be.getPistonPhase() + Mth.abs(be.getSpeed()) * partialTicks * DELTA_TIME, be.getPreviousPhase());
        be.setPreviousPhase(newPhase);
        pistonModel.translate(0, -0.2f * Mth.sin(newPhase) - 0.2f, 0).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
        kineticRotationTransform(cogsModel, be, Axis.Y, getAngleForBe(be, pos, axis) * directionModifier, light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(AirtightEngineBlockEntity be, BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.AIRTIGHT_ENGINE_COGS, blockState);
    }

    protected SuperByteBuffer getPistonModel(BlockState blockState) {
        return CachedBuffers.partial(CCBPartialModels.AIRTIGHT_ENGINE_PISTON, blockState);
    }
}
