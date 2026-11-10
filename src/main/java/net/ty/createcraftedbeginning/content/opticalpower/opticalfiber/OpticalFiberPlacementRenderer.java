package net.ty.createcraftedbeginning.content.opticalpower.opticalfiber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.lib.model.baked.VirtualBlockGetter;
import net.createmod.catnip.client.render.model.BakedModelBufferer;
import net.createmod.catnip.ghostblock.GhostBlockParams;
import net.createmod.catnip.ghostblock.GhostBlockRenderer;
import net.createmod.catnip.ghostblock.GhostBlocks;
import net.createmod.catnip.impl.client.render.ColoringVertexConsumer;
import net.createmod.catnip.placement.PlacementClient;
import net.createmod.catnip.placement.PlacementOffset;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
final class OpticalFiberPlacementRenderer extends GhostBlockRenderer {
    private static final float PREVIEW_SCALE = 0.85F;
    private static final float PREVIEW_OPACITY = 0.75F;

    private final WeakReference<ClientLevel> sourceLevel;
    private final BlockPos pos;
    private final BlockState state;

    private OpticalFiberPlacementRenderer(ClientLevel level, BlockPos pos, BlockState state) {
        sourceLevel = new WeakReference<>(level);
        this.pos = pos;
        this.state = state;
    }

    static void show(Object slot, PlacementOffset offset) {
        ClientLevel level = Minecraft.getInstance().level;
        BlockState ghostState = offset.getGhostState();
        if (level == null || ghostState == null) {
            return;
        }

        BlockPos pos = offset.getBlockPos().immutable();
        BlockState state = offset.getTransform().apply(ghostState);
        GhostBlocks.getInstance().showGhost(slot, new OpticalFiberPlacementRenderer(level, pos, state), GhostBlockParams.of(state).at(pos), 1);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void render(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, GhostBlockParams params) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || sourceLevel.get() != level) {
            return;
        }

        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        PreviewBlockGetter preview = new PreviewBlockGetter(level, pos, state);
        float alpha = (float) GhostBlocks.getBreathingAlpha() * PREVIEW_OPACITY * PlacementClient.getCurrentAlpha();
        VertexConsumer vertices = new ColoringVertexConsumer(buffer.getEarlyBuffer(RenderType.translucent()), 1, 1, 1, alpha);
        poseStack.pushPose();
        try {
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(PREVIEW_SCALE, PREVIEW_SCALE, PREVIEW_SCALE);
            poseStack.translate(-0.5, -0.5, -0.5);
            BakedModelBufferer.bufferModel(model, pos, preview, state, poseStack, (layer, shade) -> vertices);
        }
        finally {
            poseStack.popPose();
        }
    }

    private static final class PreviewBlockGetter extends VirtualBlockGetter {
        private static final int FULL_LIGHT_LEVEL = 15;

        private final ClientLevel level;
        private final BlockPos pos;
        private final BlockState state;

        private PreviewBlockGetter(ClientLevel level, BlockPos pos, BlockState state) {
            super(ignored -> FULL_LIGHT_LEVEL, ignored -> FULL_LIGHT_LEVEL);
            this.level = level;
            this.pos = pos;
            this.state = state;
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos queryPos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos queryPos) {
            if (queryPos.equals(pos)) {
                return state;
            }

            if (!level.isLoaded(queryPos)) {
                return Blocks.AIR.defaultBlockState();
            }

            return level.getBlockState(queryPos);
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return level.getMinBuildHeight();
        }
    }
}
