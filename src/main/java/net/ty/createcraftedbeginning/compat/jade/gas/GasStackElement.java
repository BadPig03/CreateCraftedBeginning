package net.ty.createcraftedbeginning.compat.jade.gas;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.registry.CCBMaterials;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.ui.Element;
import snownee.jade.overlay.OverlayRenderer;

import java.util.Objects;
import java.util.function.BiConsumer;

public class GasStackElement extends Element {
    private final JadeGasObject gas;

    public GasStackElement(JadeGasObject gas) {
        this.gas = Objects.requireNonNull(gas);
    }

    private static void getGasSpriteAndColor(@NotNull JadeGasObject gas, BiConsumer<@Nullable TextureAtlasSprite, Integer> consumer) {
        TextureAtlasSprite sprite = Gas.getGasTexture(gas.type().getHolder());
        int tint = gas.type().getTint();
        if (OverlayRenderer.alpha != 1f) {
            tint = IWailaConfig.IConfigOverlay.applyAlpha(tint, OverlayRenderer.alpha);
        }

        consumer.accept(sprite, tint);
    }

    private static void fill(@NotNull GuiGraphics guiGraphics, float minX, float minY, float maxX, float maxY, int color) {
        Matrix4f matrix = guiGraphics.pose().last().pose();
        if (minX < maxX) {
            float i = minX;
            minX = maxX;
            maxX = i;
        }

        if (minY < maxY) {
            float j = minY;
            minY = maxY;
            maxY = j;
        }

        color = IWailaConfig.IConfigOverlay.applyAlpha(color, OverlayRenderer.alpha);
        VertexConsumer buffer = guiGraphics.bufferSource().getBuffer(RenderType.gui());
        buffer.addVertex(matrix, minX, maxY, 0f).setColor(color);
        buffer.addVertex(matrix, maxX, maxY, 0f).setColor(color);
        buffer.addVertex(matrix, maxX, minY, 0f).setColor(color);
        buffer.addVertex(matrix, minX, minY, 0f).setColor(color);
        guiGraphics.flush();
    }

    private static void setGLColorFromInt(int color) {
        float red = (float) (color >> 16 & 255) / 255f;
        float green = (float) (color >> 8 & 255) / 255f;
        float blue = (float) (color & 255) / 255f;
        float alpha = (float) (color >> 24 & 255) / 255f;
        RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    private static void drawTextureWithMasking(Matrix4f matrix, float xCoordinate, float yCoordinate, @NotNull TextureAtlasSprite textureSprite, float maskTop, float maskRight) {
        float uMin = textureSprite.getU0();
        float uMax = textureSprite.getU1();
        float vMin = textureSprite.getV0();
        float vMax = textureSprite.getV1();
        uMax -= maskRight / 16f * (uMax - uMin);
        vMax -= maskTop / 16f * (vMax - vMin);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, xCoordinate, yCoordinate + 16f, 0f).setUv(uMin, vMax);
        buffer.addVertex(matrix, xCoordinate + 16f - maskRight, yCoordinate + 16f, 0f).setUv(uMax, vMax);
        buffer.addVertex(matrix, xCoordinate + 16f - maskRight, yCoordinate + maskTop, 0f).setUv(uMax, vMin);
        buffer.addVertex(matrix, xCoordinate, yCoordinate + maskTop, 0f).setUv(uMin, vMin);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private void drawTiledSprite(GuiGraphics guiGraphics, float xPosition, float yPosition, float tiledWidth, float tiledHeight, int color, float scaledAmount, TextureAtlasSprite sprite) {
        if (tiledWidth == 0f || tiledHeight == 0f || scaledAmount == 0f) {
            return;
        }
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        setGLColorFromInt(color);
        RenderSystem.enableBlend();
        int xTileCount = (int) (tiledWidth / 16f);
        float xRemainder = tiledWidth - (float) (xTileCount * 16);
        int yTileCount = (int) (scaledAmount / 16f);
        float yRemainder = scaledAmount - (float) (yTileCount * 16);
        float yStart = yPosition + tiledHeight;

        for (int xTile = 0; xTile <= xTileCount; ++xTile) {
            for (int yTile = 0; yTile <= yTileCount; ++yTile) {
                float width = xTile == xTileCount ? xRemainder : 16f;
                float height = yTile == yTileCount ? yRemainder : 16f;
                float x = xPosition + (float) (xTile * 16);
                float y = yStart - (float) ((yTile + 1) * 16);
                if (width > 0f && height > 0f) {
                    float maskTop = 16f - height;
                    float maskRight = 16f - width;
                    drawTextureWithMasking(matrix, x, y, sprite, maskTop, maskRight);
                }
            }
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    @OnlyIn(Dist.CLIENT)
    private void renderGlintEffect(@NotNull GuiGraphics guiGraphics, float x, float y, float width, float height, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        long time = level.getGameTime();
        float r = (float) (color >> 16 & 255) / 255f;
        float g = (float) (color >> 8 & 255) / 255f;
        float b = (float) (color & 255) / 255f;
        renderSingleGlintLayer(guiGraphics, x, y, width, height, time % 6000L, 0.5f, 48f, r, g, b, 0.2f);
        renderSingleGlintLayer(guiGraphics, x, y, width, height, time % 5000L + 1500L, 0.4f, 48f, r, g, b, 0.15f);

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    @OnlyIn(Dist.CLIENT)
    private void renderSingleGlintLayer(@NotNull GuiGraphics guiGraphics, float x, float y, float width, float height, long baseTime, float speedFactor, float textureScale, float r, float g, float b, float a) {
        RenderSystem.setShader(GameRenderer::getRendertypeGlintTranslucentShader);
        RenderSystem.setShaderColor(r, g, b, a);
        RenderSystem.setShaderTexture(0, CCBMaterials.VORTICES_GLINT);

        float time = (float) (baseTime) / (6000f / speedFactor);
        float uOffset = time * 8f;
        float vOffset = time * 8f;

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix, x, y + height, 0).setUv(uOffset, vOffset + height / textureScale);
        bufferBuilder.addVertex(matrix, x + width, y + height, 0).setUv(uOffset + width / textureScale, vOffset + height / textureScale);
        bufferBuilder.addVertex(matrix, x + width, y, 0).setUv(uOffset + width / textureScale, vOffset);
        bufferBuilder.addVertex(matrix, x, y, 0).setUv(uOffset, vOffset);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    @Override
    public Vec2 getSize() {
        return new Vec2(16, 16);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics guiGraphics, float x, float y, float maxX, float maxY) {
        if (gas.isEmpty()) {
            return;
        }

        drawGas(guiGraphics, x, y, gas, size.x, size.y, JadeGasObject.bucketVolume());
    }

    @Override
    public @Nullable String getMessage() {
        return null;
    }

    private void drawGas(GuiGraphics guiGraphics, float xPosition, float yPosition, @NotNull JadeGasObject gas, float width, float height, long capacityMb) {
        long amount = JadeGasObject.bucketVolume();
        MutableFloat scaledAmount = new MutableFloat((float) amount * height / (float) capacityMb);
        if (amount > 0L && scaledAmount.floatValue() < 1f) {
            scaledAmount.setValue(1f);
        }

        if (scaledAmount.floatValue() > height) {
            scaledAmount.setValue(height);
        }

        getGasSpriteAndColor(gas, (sprite, color) -> {
            if (sprite == null) {
                float maxY = yPosition + height;
                if (color == -1) {
                    color = -1431655766;
                }

                fill(guiGraphics, xPosition, maxY - scaledAmount.floatValue(), xPosition + width, maxY, color);
            } else {
                if (OverlayRenderer.alpha != 1f) {
                    color = IWailaConfig.IConfigOverlay.applyAlpha(color, OverlayRenderer.alpha);
                }

                drawTiledSprite(guiGraphics, xPosition, yPosition, width, height, color, scaledAmount.floatValue(), sprite);

                if (gas.type().getEnergy() > 0) {
                    renderGlintEffect(guiGraphics, xPosition, yPosition, width, height, color);
                }
            }
        });
    }
}
