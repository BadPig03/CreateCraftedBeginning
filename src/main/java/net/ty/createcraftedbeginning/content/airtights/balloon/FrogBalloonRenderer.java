package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FrogBalloonRenderer {
    private FrogBalloonRenderer() {
    }

    public static boolean render(FrogportBlockEntity port, PoseStack matrices, MultiBufferSource buffers, int light, int overlay, Vec3 travel, float scale, float distance) {
        FrogBalloonRenderState state = FrogBalloonRenderState.create(port, travel, scale, distance);
        if (state == null) {
            return false;
        }

        if (!state.visible()) {
            return true;
        }

        BlockState block = port.getBlockState();
        ResourceLocation model = state.model();
        FrogBalloonPose pose = state.pose();
        float renderedScale = state.scale();
        VertexConsumer buffer = buffers.getBuffer(RenderType.cutout());
        SuperByteBuffer box = CachedBuffers.partial(AllPartialModels.PACKAGES.get(model), block);
        box.translate(0, pose.baseY(), 0).translate(pose.offset()).center().scale(renderedScale).uncenter().translate(0, pose.hookDistance() + pose.boxDistance(), 0).light(light).overlay(overlay).renderInto(matrices, buffer);
        if (!state.depositing()) {
            return true;
        }

        SuperByteBuffer rig = CachedBuffers.partial(AllPartialModels.PACKAGE_RIGGING.get(model), block);
        rig.translate(0, pose.baseY(), 0).translate(pose.offset()).center().scale(renderedScale).uncenter().translate(0, pose.hookDistance(), 0).light(light).overlay(overlay).renderInto(matrices, buffer);
        return true;
    }

    public static boolean renderVisual(FrogportBlockEntity port, Vec3 travel, float scale, float distance, BlockPos visualPos, InstancerProvider instances, TransformedInstance rig, TransformedInstance box) {
        FrogBalloonRenderState state = FrogBalloonRenderState.create(port, travel, scale, distance);
        if (state == null) {
            return false;
        }

        if (!state.visible()) {
            rig.handle().setVisible(false);
            box.handle().setVisible(false);
            return true;
        }

        ResourceLocation model = state.model();
        FrogBalloonPose pose = state.pose();
        float renderedScale = state.scale();
        instances.instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.PACKAGES.get(model))).stealInstance(box);
        box.handle().setVisible(true);
        box.setIdentityTransform().translate(visualPos).translate(0, pose.baseY(), 0).translate(pose.offset()).center().scale(renderedScale).uncenter().translate(0, pose.hookDistance() + pose.boxDistance(), 0).setChanged();
        if (!state.depositing()) {
            rig.handle().setVisible(false);
            return true;
        }

        instances.instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.PACKAGE_RIGGING.get(model))).stealInstance(rig);
        rig.handle().setVisible(true);
        rig.setIdentityTransform().translate(visualPos).translate(0, pose.baseY(), 0).translate(pose.offset()).center().scale(renderedScale).uncenter().translate(0, pose.hookDistance(), 0).setChanged();
        return true;
    }
}
