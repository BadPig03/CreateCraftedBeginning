package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.util.SmartRecycler;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ChainBalloonRenderer {
    private ChainBalloonRenderer() {
    }

    public static boolean render(ChainConveyorBlockEntity conveyor, PoseStack matrices, MultiBufferSource buffer, int overlay, BlockPos pos, ChainConveyorPackage box, float partialTicks) {
        ChainBalloonRenderState state = ChainBalloonRenderState.create(conveyor, box, pos, partialTicks);
        if (state == null) {
            return false;
        }

        ItemStack item = state.item();
        ChainBalloonPose pose = state.pose();
        BlockState blockState = conveyor.getBlockState();
        ResourceLocation model = state.model();
        SuperByteBuffer rigBuffer = CachedBuffers.partial(AllPartialModels.PACKAGE_RIGGING.get(model), blockState);
        SuperByteBuffer boxBuffer = CachedBuffers.partial(AllPartialModels.PACKAGES.get(model), blockState);
        for (SuperByteBuffer part : new SuperByteBuffer[]{rigBuffer, boxBuffer}) {
            part.translate(pose.offset()).translate(0, 0.625, 0).rotateYDegrees(pose.yaw()).rotateZDegrees(pose.zRotation()).rotateXDegrees(pose.xRotation());
            if (state.flipped() && part == rigBuffer) {
                part.rotateYDegrees(180);
            }
            BalloonRenderHelper.applyChainConveyorAnchoredScale(part, item, state.scale());
            part.uncenter().translate(0, BalloonItem.getHookDistance(item), 0);
            if (part == boxBuffer) {
                part.translate(0, BalloonItem.getBoxDistance(item), 0);
            }
            part.light(state.light()).overlay(overlay).renderInto(matrices, buffer.getBuffer(RenderType.cutoutMipped()));
        }

        return true;
    }

    public static boolean renderVisual(ChainConveyorBlockEntity conveyor, ChainConveyorPackage box, float partialTicks, BlockPos pos, BlockPos visualPos, SmartRecycler<ResourceLocation, TransformedInstance> rigging, SmartRecycler<ResourceLocation, TransformedInstance> boxes) {
        ChainBalloonRenderState state = ChainBalloonRenderState.create(conveyor, box, pos, partialTicks);
        if (state == null) {
            return false;
        }

        ItemStack item = state.item();
        ChainBalloonPose pose = state.pose();
        ResourceLocation model = state.model();
        TransformedInstance rigBuffer = rigging.get(model);
        TransformedInstance boxBuffer = boxes.get(model);
        for (TransformedInstance part : new TransformedInstance[]{rigBuffer, boxBuffer}) {
            part.setIdentityTransform().translate(visualPos).translate(pose.offset()).translate(0, 0.625, 0).rotateYDegrees(pose.yaw()).rotateZDegrees(pose.zRotation()).rotateXDegrees(pose.xRotation());
            if (state.flipped() && part == rigBuffer) {
                part.rotateYDegrees(180);
            }
            BalloonRenderHelper.applyChainConveyorAnchoredScale(part, item, state.scale());
            part.uncenter().translate(0, BalloonItem.getHookDistance(item), 0);
            if (part == boxBuffer) {
                part.translate(0, BalloonItem.getBoxDistance(item), 0);
            }
            part.light(state.light()).setChanged();
        }

        return true;
    }
}
