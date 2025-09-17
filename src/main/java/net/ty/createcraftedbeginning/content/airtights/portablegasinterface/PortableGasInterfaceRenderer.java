package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class PortableGasInterfaceRenderer extends SafeBlockEntityRenderer<PortableGasInterfaceBlockEntity> {
    public PortableGasInterfaceRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    public static void renderInContraption(@NotNull MovementContext context, VirtualRenderWorld renderWorld, @NotNull ContraptionMatrices matrices, @NotNull MultiBufferSource buffer) {
        BlockState blockState = context.state;
        VertexConsumer vb = buffer.getBuffer(RenderType.solid());
        float renderPartialTicks = AnimationTickHolder.getPartialTicks();

        LerpedFloat animation = PortableGasInterfaceMovement.getAnimation(context);
        float progress = animation.getValue(renderPartialTicks);
        boolean lit = animation.settled();
        render(blockState, lit, progress, matrices.getModel(), sbb -> sbb.light(LevelRenderer.getLightColor(renderWorld, context.localPos)).useLevelLight(context.world, matrices.getWorld()).renderInto(matrices.getViewProjection(), vb));
    }

    private static void render(BlockState blockState, boolean lit, float progress, PoseStack local, Consumer<SuperByteBuffer> drawCallback) {
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

    private static void rotateToFacing(@NotNull SuperByteBuffer buffer, Direction facing) {
        buffer.center().rotateYDegrees(AngleHelper.horizontalAngle(facing)).rotateXDegrees(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90).uncenter();
    }

    @SuppressWarnings("unused")
    private static @Nullable PortableGasInterfaceBlockEntity getTargetPSI(@NotNull MovementContext context) {
        String _workingPos_ = PortableGasInterfaceMovement._workingPos_;
        if (!context.data.contains(_workingPos_)) {
            return null;
        }

        BlockPos pos = NBTHelper.readBlockPos(context.data, _workingPos_);
        BlockEntity blockEntity = context.world.getBlockEntity(pos);
        if (!(blockEntity instanceof PortableGasInterfaceBlockEntity psi)) {
            return null;
        }

        if (!psi.isTransferring()) {
            return null;
        }
        return psi;
    }

    static PartialModel getTopForState() {
        return CCBPartialModels.PORTABLE_GAS_INTERFACE_TOP;
    }

    static PartialModel getMiddleForState(boolean lit) {
        return lit ? CCBPartialModels.PORTABLE_GAS_INTERFACE_MIDDLE_POWERED : CCBPartialModels.PORTABLE_GAS_INTERFACE_MIDDLE;
    }

    @Override
    protected void renderSafe(@NotNull PortableGasInterfaceBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            return;
        }

        BlockState blockState = be.getBlockState();
        float progress = be.getExtensionDistance(partialTicks);
        VertexConsumer vb = buffer.getBuffer(RenderType.solid());
        render(blockState, be.isConnected(), progress, null, sbb -> sbb.light(light).renderInto(ms, vb));
    }
}
