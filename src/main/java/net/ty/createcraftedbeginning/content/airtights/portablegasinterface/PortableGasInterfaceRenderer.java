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
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PortableGasInterfaceRenderer extends SafeBlockEntityRenderer<PortableGasInterfaceBlockEntity> {
    public PortableGasInterfaceRenderer(Context ignored) {
    }

    public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource bufferSource) {
        LerpedFloat connectionAnimation = PortableGasInterfaceMovement.getAnimation(context);
        boolean lit = connectionAnimation.settled();
        float extensionProgress = connectionAnimation.getValue(AnimationTickHolder.getPartialTicks());
        PoseStack model = matrices.getModel();
        Consumer<SuperByteBuffer> draw = buffer -> buffer.light(LevelRenderer.getLightColor(renderWorld, context.localPos)).useLevelLight(context.world, matrices.getWorld()).renderInto(matrices.getViewProjection(), bufferSource.getBuffer(RenderType.solid()));

        render(context.state, lit, extensionProgress, model, draw);
    }

    private static void render(BlockState blockState, boolean lit, float extensionProgress, @Nullable PoseStack poseStack, Consumer<SuperByteBuffer> draw) {
        SuperByteBuffer middle = CachedBuffers.partial(getMiddleForState(lit), blockState);
        SuperByteBuffer top = CachedBuffers.partial(getTopForState(), blockState);
        if (poseStack != null) {
            middle.transform(poseStack);
            top.transform(poseStack);
        }

        Direction facing = blockState.getValue(PortableGasInterfaceBlock.FACING);
        rotateToFacing(middle, facing);
        rotateToFacing(top, facing);
        middle.translate(0, extensionProgress * 0.5 + 0.375, 0);
        top.translate(0, extensionProgress, 0);

        draw.accept(middle);
        draw.accept(top);
    }

    private static void rotateToFacing(SuperByteBuffer buffer, Direction facing) {
        float xRotation = switch (facing) {
            case UP -> 0;
            case DOWN -> 180;
            default -> 90;
        };
        buffer.center().rotateYDegrees(AngleHelper.horizontalAngle(facing)).rotateXDegrees(xRotation).uncenter();
    }

    public static PartialModel getTopForState() {
        return CCBPartialModels.PORTABLE_GAS_INTERFACE_TOP;
    }

    public static PartialModel getMiddleForState(boolean lit) {
        return lit ? CCBPartialModels.PORTABLE_GAS_INTERFACE_MIDDLE_POWERED : CCBPartialModels.PORTABLE_GAS_INTERFACE_MIDDLE;
    }

    @Override
    protected void renderSafe(PortableGasInterfaceBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        render(blockEntity.getBlockState(), blockEntity.isConnected(), blockEntity.getExtensionDistance(partialTicks), null, buffer -> buffer.light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.solid())));
    }
}
