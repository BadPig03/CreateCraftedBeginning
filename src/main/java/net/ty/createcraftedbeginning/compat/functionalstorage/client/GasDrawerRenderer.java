package net.ty.createcraftedbeginning.compat.functionalstorage.client;

import com.buuz135.functionalstorage.block.Drawer;
import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile.DrawerOptions;
import com.buuz135.functionalstorage.client.DrawerRenderer;
import com.buuz135.functionalstorage.client.FunctionalStorageClientConfig;
import com.buuz135.functionalstorage.item.ConfigurationToolItem.ConfigurationAction;
import com.buuz135.functionalstorage.util.MathUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.client.CCBGasClientTextures;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerBlockEntity;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerBlockEntity.RenderGas;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerRenderer implements BlockEntityRenderer<GasDrawerBlockEntity> {
    public GasDrawerRenderer(Context ignoredContext) {
    }

    private static void renderSlots(GasDrawerBlockEntity drawer, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        switch (drawer.getDrawerType()) {
            case X_1 -> renderSlot(drawer, 0, poseStack, buffers, light, overlay, 0, 0, 0.875, 0.78125, false);
            case X_2 -> {
                renderSlot(drawer, 0, poseStack, buffers, light, overlay, 0, 0, 0.875, 0.34375, false);
                renderSlot(drawer, 1, poseStack, buffers, light, overlay, 0, 0.5, 0.875, 0.34375, false);
            }
            case X_4 -> {
                renderSlot(drawer, 0, poseStack, buffers, light, overlay, 0.5, 0, 0.4375, 0.34375, true);
                renderSlot(drawer, 1, poseStack, buffers, light, overlay, 0, 0, 0.4375, 0.34375, true);
                renderSlot(drawer, 2, poseStack, buffers, light, overlay, 0.5, 0.5, 0.4375, 0.34375, true);
                renderSlot(drawer, 3, poseStack, buffers, light, overlay, 0, 0.5, 0.4375, 0.34375, true);
            }
        }
    }

    private static void renderSlot(GasDrawerBlockEntity drawer, int slot, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, double offsetX, double offsetY, double width, double height, boolean compact) {
        RenderGas renderGas = drawer.getRenderGas(slot);
        if (renderGas.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, 0);
        DrawerOptions drawerOptions = drawer.getDrawerOptions();
        if (drawerOptions.isActive(ConfigurationAction.TOGGLE_RENDER)) {
            AABB gasBounds = new AABB(0.0625, 0.078125, 0.0625, 0.0625 + width, 0.078125 + height, 0.9375);
            renderGasSurface(poseStack, buffers, light, overlay, renderGas.stack(), renderGas.filterOnly(), gasBounds);
        }
        if (drawerOptions.isActive(ConfigurationAction.TOGGLE_NUMBERS)) {
            renderAmount(drawer, renderGas, poseStack, buffers, overlay, compact);
        }
        renderIndicator(drawer, renderGas, poseStack, buffers, light, overlay, drawerOptions, compact);
        poseStack.popPose();
    }

    private static void renderAmount(GasDrawerBlockEntity drawer, RenderGas renderGas, PoseStack poseStack, MultiBufferSource buffers, int overlay, boolean compact) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.84, 0.97);
        if (compact) {
            poseStack.translate(-0.25, 0, 0);
        }

        String amountText = renderGas.filterOnly() ? "0" : drawer.isCreative() ? "∞" : GasAmounts.formatCompact(renderGas.stack().getAmount());
        DrawerRenderer.renderText(poseStack, buffers, overlay, Component.literal(amountText).withStyle(ChatFormatting.WHITE), Direction.NORTH, 0.007f);
        poseStack.popPose();
    }

    private static void renderIndicator(GasDrawerBlockEntity drawer, RenderGas renderGas, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, DrawerOptions options, boolean compact) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.453, 0.97);
        if (compact) {
            poseStack.scale(0.5f, 0.65f, 0.5f);
            poseStack.translate(-0.5, -0.18, 0);
        }

        long tankCapacity = Math.max(1, drawer.getPhysicalTankCapacity());
        float fillRatio = renderGas.filterOnly() ? 0 : drawer.isCreative() ? 1 : (float) Math.min(1, renderGas.stack().getAmount() / (double) tankCapacity);
        DrawerRenderer.renderIndicator(poseStack, buffers, light, overlay, fillRatio, options);
        poseStack.popPose();
    }

    public static void renderItemGas(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, GasStack stack, DrawerOptions options, AABB bounds, boolean compact, boolean creative) {
        if (stack.isEmpty()) {
            return;
        }

        if (options.isActive(ConfigurationAction.TOGGLE_RENDER)) {
            renderGasSurface(poseStack, buffers, light, overlay, stack, false, bounds);
        }
        if (options.isActive(ConfigurationAction.TOGGLE_NUMBERS)) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.84, 0.97);
            if (compact) {
                poseStack.translate(-0.25, 0, 0);
            }
            String amountText = creative ? "∞" : GasAmounts.formatCompact(stack.getAmount());
            DrawerRenderer.renderText(poseStack, buffers, overlay, Component.literal(amountText).withStyle(ChatFormatting.WHITE), Direction.NORTH, 0.007f);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.453, 0.97);
        if (compact) {
            poseStack.scale(0.5f, 0.65f, 0.5f);
            poseStack.translate(-0.5, -0.18, 0);
        }
        DrawerRenderer.renderIndicator(poseStack, buffers, light, overlay, 1, options);
        poseStack.popPose();
    }

    private static void renderGasSurface(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, GasStack stack, boolean filterOnly, AABB bounds) {
        TextureAtlasSprite sprite = CCBGasClientTextures.getGasTexture(stack.getGasHolder());
        int tint = stack.getHint();
        float red = ARGB32.red(tint) / 255.0f;
        float green = ARGB32.green(tint) / 255.0f;
        float blue = ARGB32.blue(tint) / 255.0f;
        float sourceAlpha = ARGB32.alpha(tint) / 255.0f;
        float alpha = filterOnly ? 0.3f : sourceAlpha <= 0 ? 1 : sourceAlpha;
        VertexConsumer buffer = buffers.getBuffer(RenderType.translucent());
        Matrix4f poseMatrix = poseStack.last().pose();

        float minX = (float) bounds.minX;
        float maxX = (float) bounds.maxX;
        float minY = (float) bounds.minY;
        float maxY = (float) bounds.maxY;
        float maxZ = (float) bounds.maxZ;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float frontMinX = minX;
        float frontMaxX = maxX;
        float frontHeight = maxY - minY;
        if (maxX - minX > frontHeight * 1.5f) {
            float centerX = (minX + maxX) * 0.5f;
            float halfSize = frontHeight * 0.5f;
            frontMinX = centerX - halfSize;
            frontMaxX = centerX + halfSize;
        }
        buffer.addVertex(poseMatrix, frontMaxX, minY, maxZ).setColor(red, green, blue, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        buffer.addVertex(poseMatrix, frontMaxX, maxY, maxZ).setColor(red, green, blue, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        buffer.addVertex(poseMatrix, frontMinX, maxY, maxZ).setColor(red, green, blue, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        buffer.addVertex(poseMatrix, frontMinX, minY, maxZ).setColor(red, green, blue, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
    }

    private static void renderUpgrades(GasDrawerBlockEntity drawer, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0, 0, 0.9688);
        DrawerRenderer.renderUpgrades(poseStack, buffers, light, overlay, drawer);
        poseStack.popPose();
    }

    private static void applyDrawerOrientation(GasDrawerBlockEntity drawer, PoseStack poseStack) {
        Direction drawerFacing = drawer.getFacingDirection();
        if (drawer.getBlockState().hasProperty(Drawer.FACING_ALL)) {
            applyAllFacingOrientation(drawerFacing, drawer.getBlockState().getValue(Drawer.FACING_ALL), poseStack);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(-180));
        switch (drawer.getFacingDirection()) {
            case NORTH -> poseStack.translate(-1, 0, -1);
            case EAST -> {
                poseStack.translate(0, 0, -1);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(-180));
            case WEST -> {
                poseStack.translate(-1, 0, 0);
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
            }
            default -> {
            }
        }
    }

    private static void applyAllFacingOrientation(Direction drawerFacing, Direction blockFacing, PoseStack poseStack) {
        if (drawerFacing == Direction.UP) {
            poseStack.mulPose(MathUtils.createTransformMatrix(new Vector3f(1, 0, 0), new Vector3f(90, 0, 0), 1));
            if (blockFacing == Direction.EAST) {
                poseStack.mulPose(MathUtils.createTransformMatrix(new Vector3f(-1, 0, 0), new Vector3f(0, 0, -90), 1));
            }
            else if (blockFacing == Direction.WEST) {
                poseStack.mulPose(MathUtils.createTransformMatrix(new Vector3f(0, 1, 0), new Vector3f(0, 0, 90), 1));
            }
        }
        else if (drawerFacing == Direction.DOWN) {
            poseStack.mulPose(MathUtils.createTransformMatrix(new Vector3f(0, 1, 0), new Vector3f(-90, 0, -180), 1));
            if (blockFacing == Direction.WEST) {
                poseStack.mulPose(MathUtils.createTransformMatrix(new Vector3f(-1, 0, 0), new Vector3f(0, 0, -90), 1));
            }
            else if (blockFacing == Direction.EAST) {
                poseStack.mulPose(MathUtils.createTransformMatrix(new Vector3f(0, 1, 0), new Vector3f(0, 0, 90), 1));
            }
        }
        if (blockFacing == Direction.NORTH) {
            poseStack.mulPose(MathUtils.createTransformMatrix(new Vector3f(-1, 1, 0), new Vector3f(0, 0, 180), 1));
        }
    }

    @Override
    public void render(GasDrawerBlockEntity drawer, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = drawer.getLevel();
        if (level == null) {
            return;
        }

        poseStack.pushPose();
        applyDrawerOrientation(drawer, poseStack);
        Direction drawerFacing = drawer.getFacingDirection();
        packedLight = LevelRenderer.getLightColor(level, drawer.getBlockPos().relative(drawerFacing));
        renderSlots(drawer, poseStack, buffers, packedLight, packedOverlay);
        renderUpgrades(drawer, poseStack, buffers, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return FunctionalStorageClientConfig.DRAWER_RENDER_RANGE;
    }
}
