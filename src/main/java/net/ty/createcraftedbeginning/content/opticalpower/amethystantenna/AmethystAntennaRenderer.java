package net.ty.createcraftedbeginning.content.opticalpower.amethystantenna;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.render.PonderRenderTypes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystAntennaRenderer extends SafeBlockEntityRenderer<AmethystAntennaBlockEntity> {
    private static final int VIEW_DISTANCE = 64;
    private static final int BLINK_PERIOD_TICKS = 16;
    private static final int BRIGHT_TICKS = 8;
    private static final float OUTLINE_WIDTH = 0.03125F;
    private static final int COLOR_DIM = 0xB58AD9;
    private static final int COLOR_BRIGHT = 0xD1B0F0;
    private final NetworkOutline outline = new NetworkOutline();
    private final Vector3f viewPosition = new Vector3f();

    public AmethystAntennaRenderer(Context ignored) {
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @Override
    protected void renderSafe(AmethystAntennaBlockEntity antenna, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = antenna.getLevel();
        if (player == null || level == null || minecraft.screen != null) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        LoadingNetworkBinding selected = held.get(CCBDataComponents.LOADING_NETWORK);
        if (!(held.getItem() instanceof AmethystAntennaBlockItem) || selected == null) {
            return;
        }

        LoadingNetworkBinding binding = antenna.binding();
        if (binding == null || !binding.id().equals(selected.id())) {
            return;
        }

        poseStack.last().pose().transformPosition(viewPosition.set(0.5F, 0.5F, 0.5F));
        if (viewPosition.lengthSquared() > VIEW_DISTANCE * VIEW_DISTANCE) {
            return;
        }

        int color = COLOR_DIM;
        if (AnimationTickHolder.getTicks() % BLINK_PERIOD_TICKS < BRIGHT_TICKS) {
            color = COLOR_BRIGHT;
        }
        outline.getParams().colored(color).lineWidth(OUTLINE_WIDTH).disableLineNormals();
        BlockState state = antenna.getBlockState();
        BlockPos pos = antenna.getBlockPos();
        for (AABB bounds : state.getShape(level, pos).toAabbs()) {
            outline.setBounds(bounds);
            outline.render(poseStack, buffers);
        }
    }

    private static final class NetworkOutline extends AABBOutline {
        private NetworkOutline() {
            super(new AABB(BlockPos.ZERO));
        }

        private void render(PoseStack poseStack, MultiBufferSource buffers) {
            params.loadColor(colorTemp);
            minPosTemp1.set((float) bb.minX, (float) bb.minY, (float) bb.minZ);
            maxPosTemp1.set((float) bb.maxX, (float) bb.maxY, (float) bb.maxZ);
            renderBoxEdges(poseStack, buffers.getBuffer(PonderRenderTypes.outlineSolid()), minPosTemp1, maxPosTemp1, params.getLineWidth(), colorTemp, LightTexture.FULL_BRIGHT, true);
        }
    }
}
