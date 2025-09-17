package net.ty.createcraftedbeginning.content.airtights.airtightengine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

import static net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlockEntity.DELTA_TIME;

public class AirtightEngineRenderer extends KineticBlockEntityRenderer<AirtightEngineBlockEntity> {
    public AirtightEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(@NotNull AirtightEngineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        SuperByteBuffer cogsModel = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_ENGINE_COGS, state);
        SuperByteBuffer pistonModel = CachedBuffers.partial(CCBPartialModels.AIRTIGHT_ENGINE_PISTON, state);

        Direction.Axis axis = state.getValue(AirtightEngineBlock.AXIS);
        Direction direction = AirtightEngineBlock.getFacing(state);
        final BlockPos pos = be.getBlockPos();
        float speed = be.getSpeed();
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float offset = getRotationOffsetForPosition(be, pos, axis);
        float angle = (time * speed * 3 / 10f) % 360;
        float degree = (float) (Math.PI / 2) * (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1);

        if (axis == Direction.Axis.X) {
            pistonModel.rotateCentered(degree, Direction.Axis.Z);
            cogsModel.rotateCentered(degree, Direction.Axis.Z);
        } else if (axis == Direction.Axis.Z) {
            pistonModel.rotateCentered(-degree, Direction.Axis.X);
            cogsModel.rotateCentered(-degree, Direction.Axis.X);
        } else if (direction == Direction.UP) {
            pistonModel.rotateCentered((float) Math.PI, Direction.Axis.X);
            cogsModel.rotateCentered((float) Math.PI, Direction.Axis.X);
        }

        if (speed != 0) {
            angle += offset;
            angle = angle / 180f * (float) Math.PI;
        }
        kineticRotationTransform(cogsModel, be, Direction.Axis.Y, angle, light);
        cogsModel.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

        if (speed != 0) {
            float currentPhase = be.getPistonPhase() + (Mth.abs(speed) * partialTicks * DELTA_TIME);
            float distance = (float) (-0.2 * Math.sin(currentPhase) - 0.2);
            pistonModel.translate(0, distance, 0);
        }
        pistonModel.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }
}
