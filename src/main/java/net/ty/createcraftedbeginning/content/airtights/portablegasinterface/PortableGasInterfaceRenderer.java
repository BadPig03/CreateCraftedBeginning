package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PortableGasInterfaceRenderer extends SafeBlockEntityRenderer<PortableGasInterfaceBlockEntity> {
    public PortableGasInterfaceRenderer(Context ignored) {
    }

    public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource bufferSource) {
        LerpedFloat animation = PortableGasInterfaceMovement.getAnimation(context);
        render(context.state, animation.settled(), animation.getValue(AnimationTickHolder.getPartialTicks()), matrices.getModel(), byteBuffer -> byteBuffer.light(LevelRenderer.getLightColor(renderWorld, context.localPos)).useLevelLight(context.world, matrices.getWorld()).renderInto(matrices.getViewProjection(), bufferSource.getBuffer(RenderType.solid())));
    }

    private static void render(BlockState blockState, boolean lit, float progress, @Nullable PoseStack local, Consumer<SuperByteBuffer> drawCallback) {
        SuperByteBuffer middle = CachedBuffers.partial(getMiddleForState(lit), blockState);
        SuperByteBuffer top = CachedBuffers.partial(getTopForState(), blockState);
        if (local != null) {
            middle.transform(local);
            top.transform(local);
        }
        Direction facing = blockState.getValue(PortableGasInterfaceBlock.FACING);
        rotateToFacing(middle, facing);
        rotateToFacing(top, facing);
        middle.translate(0, progress * 0.5f + 0.375f, 0);
        top.translate(0, progress, 0);

        drawCallback.accept(middle);
        drawCallback.accept(top);
    }

    private static void rotateToFacing(SuperByteBuffer buffer, Direction facing) {
        buffer.center().rotateYDegrees(AngleHelper.horizontalAngle(facing)).rotateXDegrees(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90).uncenter();
    }

    public static PartialModel getTopForState() {
        return CCBPartialModels.PORTABLE_GAS_INTERFACE_TOP;
    }

    public static PartialModel getMiddleForState(boolean lit) {
        return lit ? CCBPartialModels.PORTABLE_GAS_INTERFACE_MIDDLE_POWERED : CCBPartialModels.PORTABLE_GAS_INTERFACE_MIDDLE;
    }

    @Override
    protected void renderSafe(PortableGasInterfaceBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        render(be.getBlockState(), be.isConnected(), be.getExtensionDistance(partialTicks), null, byteBuffer -> byteBuffer.light(light).renderInto(ms, bufferSource.getBuffer(RenderType.solid())));
    }
}
