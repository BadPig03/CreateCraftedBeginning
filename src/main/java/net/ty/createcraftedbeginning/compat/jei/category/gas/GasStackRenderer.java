package net.ty.createcraftedbeginning.compat.jei.category.gas;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.data.CCBGasRegistry;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class GasStackRenderer implements IIngredientRenderer<GasStack> {
    private static final int TEXTURE_SIZE = 16;
    private static final int MIN_CHEMICAL_HEIGHT = 1;

    private final long capacityMb;
    private final TooltipMode tooltipMode;
    private final int width;
    private final int height;

    public GasStackRenderer() {
        this(FluidType.BUCKET_VOLUME, TooltipMode.ITEM_LIST, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    public GasStackRenderer(long capacityMb, int width, int height) {
        this(capacityMb, TooltipMode.SHOW_AMOUNT, width, height);
    }

    private GasStackRenderer(long capacityMb, TooltipMode tooltipMode, int width, int height) {
        Preconditions.checkArgument(capacityMb > 0, "Capacity must be bigger than 0");
        this.capacityMb = capacityMb;
        this.tooltipMode = tooltipMode;
        this.width = width;
        this.height = height;
    }

    @SuppressWarnings("SameParameterValue")
    private static void drawTiledSprite(GuiGraphics guiGraphics, int xPosition, int yPosition, int yOffset, int desiredWidth, int desiredHeight, TextureAtlasSprite sprite, int textureWidth, int textureHeight, int zLevel, TilingDirection tilingDirection, boolean blend) {
        if (desiredWidth == 0 || desiredHeight == 0 || textureWidth == 0 || textureHeight == 0) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        int xTileCount = desiredWidth / textureWidth;
        int xRemainder = desiredWidth - (xTileCount * textureWidth);
        int yTileCount = desiredHeight / textureHeight;
        int yRemainder = desiredHeight - (yTileCount * textureHeight);
        int yStart = yPosition + yOffset;
        float uMin = sprite.getU0();
        float uMax = sprite.getU1();
        float vMin = sprite.getV0();
        float vMax = sprite.getV1();
        float uDif = uMax - uMin;
        float vDif = vMax - vMin;
        if (blend) {
            RenderSystem.enableBlend();
        }
        BufferBuilder vertexBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            int width = (xTile == xTileCount) ? xRemainder : textureWidth;
            if (width == 0) {
                break;
            }
            int x = xPosition + (xTile * textureWidth);
            int maskRight = textureWidth - width;
            int shiftedX = x + textureWidth - maskRight;
            float uLocalDif = uDif * maskRight / textureWidth;
            float uLocalMin;
            float uLocalMax;
            if (tilingDirection.right) {
                uLocalMin = uMin;
                uLocalMax = uMax - uLocalDif;
            } else {
                uLocalMin = uMin + uLocalDif;
                uLocalMax = uMax;
            }
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                int height = (yTile == yTileCount) ? yRemainder : textureHeight;
                if (height == 0) {
                    break;
                }
                int y = yStart - ((yTile + 1) * textureHeight);
                int maskTop = textureHeight - height;
                float vLocalDif = vDif * maskTop / textureHeight;
                float vLocalMin;
                float vLocalMax;
                if (tilingDirection.down) {
                    vLocalMin = vMin;
                    vLocalMax = vMax - vLocalDif;
                } else {
                    vLocalMin = vMin + vLocalDif;
                    vLocalMax = vMax;
                }
                vertexBuffer.addVertex(matrix4f, x, y + textureHeight, zLevel).setUv(uLocalMin, vLocalMax);
                vertexBuffer.addVertex(matrix4f, shiftedX, y + textureHeight, zLevel).setUv(uLocalMax, vLocalMax);
                vertexBuffer.addVertex(matrix4f, shiftedX, y + maskTop, zLevel).setUv(uLocalMax, vLocalMin);
                vertexBuffer.addVertex(matrix4f, x, y + maskTop, zLevel).setUv(uLocalMin, vLocalMin);
            }
        }
        BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        if (blend) {
            RenderSystem.disableBlend();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull GasStack stack) {
        if (!stack.isEmpty()) {
            int desiredHeight = Math.toIntExact(Math.round(height * (double) stack.getAmount() / capacityMb));
            if (desiredHeight < MIN_CHEMICAL_HEIGHT) {
                desiredHeight = MIN_CHEMICAL_HEIGHT;
            }
            if (desiredHeight > height) {
                desiredHeight = height;
            }

            int color = stack.getGasTint();
            guiGraphics.setColor(FastColor.ARGB32.red(color) / 255f, FastColor.ARGB32.green(color) / 255f, FastColor.ARGB32.blue(color) / 255f, FastColor.ARGB32.alpha(color) / 255f);
            drawTiledSprite(guiGraphics, 0, 0, height, width, desiredHeight, Gas.getGasTexture(stack.getGasHolder()), TEXTURE_SIZE, TEXTURE_SIZE, 100, TilingDirection.UP_RIGHT, true);
            guiGraphics.setColor(1f, 1f, 1f, 1f);
        }
    }

    @Override
    @SuppressWarnings("removal")
    public @NotNull List<Component> getTooltip(@NotNull GasStack stack, @NotNull TooltipFlag tooltipFlag) {
        Holder<Gas> gasHolder = stack.getGasHolder();
        if (gasHolder.is(CCBGasRegistry.EMPTY_GAS_KEY)) {
            return Collections.emptyList();
        }
        List<Component> tooltips = new ArrayList<>();
        collectTooltips(stack, tooltips, tooltipFlag);
        return tooltips;
    }

    @Override
    public void getTooltip(@NotNull ITooltipBuilder builder, @NotNull GasStack stack, @NotNull TooltipFlag tooltipFlag) {
        List<Component> tooltips = new ArrayList<>();
        collectTooltips(stack, tooltips, tooltipFlag);
        builder.addAll(tooltips);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    private void collectTooltips(@NotNull GasStack stack, List<Component> tooltips, TooltipFlag tooltipFlag) {
        Holder<Gas> gasHolder = stack.getGasHolder();
        if (!gasHolder.is(CCBGasRegistry.EMPTY_GAS_KEY)) {
            tooltips.add(Component.translatable(gasHolder.value().getTranslationKey()));
            if (tooltipMode == TooltipMode.SHOW_AMOUNT_AND_CAPACITY) {
                MutableComponent text = CCBLang.builder().add(CCBLang.number(stack.getAmount()).style(ChatFormatting.AQUA).add(CCBLang.number(capacityMb).style(ChatFormatting.BLUE))).component();
                tooltips.add(text);
            } else if (tooltipMode == TooltipMode.SHOW_AMOUNT) {
                MutableComponent text = CCBLang.builder().add(CCBLang.number(stack.getAmount()).style(ChatFormatting.AQUA)).component();
                tooltips.add(text);
            }
            stack.appendHoverText(getTooltipContext(), tooltips, tooltipFlag);
        }
    }

    private Item.TooltipContext getTooltipContext() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return Item.TooltipContext.EMPTY;
        } else if (Minecraft.getInstance().isSameThread()) {
            return Item.TooltipContext.of(level);
        }
        return Item.TooltipContext.of(level.registryAccess());
    }

    public enum TilingDirection {
        DOWN_RIGHT(true, true),
        DOWN_LEFT(true, false),
        UP_RIGHT(false, true),
        UP_LEFT(false, false);

        private final boolean down;
        private final boolean right;

        TilingDirection(boolean down, boolean right) {
            this.down = down;
            this.right = right;
        }
    }

    enum TooltipMode {
        SHOW_AMOUNT,
        SHOW_AMOUNT_AND_CAPACITY,
        ITEM_LIST
    }
}
