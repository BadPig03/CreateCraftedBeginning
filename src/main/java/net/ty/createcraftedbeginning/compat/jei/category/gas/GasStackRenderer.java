package net.ty.createcraftedbeginning.compat.jei.category.gas;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasStackRenderer implements IIngredientRenderer<GasStack> {
    private static final int TEXTURE_SIZE = 16;
    private static final int MIN_GAS_HEIGHT = 1;

    private final long capacityMb;
    private final TooltipMode tooltipMode;
    private final int width;
    private final int height;

    public GasStackRenderer() {
        this(FluidType.BUCKET_VOLUME, TooltipMode.ITEM_LIST, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private GasStackRenderer(long capacityMb, TooltipMode tooltipMode, int width, int height) {
        if (capacityMb <= 0) {
            throw new IllegalArgumentException("Capacity must be bigger than 0");
        }

        this.capacityMb = capacityMb;
        this.tooltipMode = tooltipMode;
        this.width = width;
        this.height = height;
    }

    public GasStackRenderer(long capacityMb, int width, int height) {
        this(capacityMb, TooltipMode.SHOW_AMOUNT, width, height);
    }

    private static void drawTiledSprite(GuiGraphics guiGraphics, int yOffset, int desiredWidth, int desiredHeight, TextureAtlasSprite sprite) {
        if (desiredWidth == 0 || desiredHeight == 0 || TEXTURE_SIZE == 0) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        int xTileCount = desiredWidth / TEXTURE_SIZE;
        int xRemainder = desiredWidth - xTileCount * TEXTURE_SIZE;
        int yTileCount = desiredHeight / TEXTURE_SIZE;
        int yRemainder = desiredHeight - yTileCount * TEXTURE_SIZE;
        float uMin = sprite.getU0();
        float uMax = sprite.getU1();
        float vMin = sprite.getV0();
        float vMax = sprite.getV1();
        float uDif = uMax - uMin;
        float vDif = vMax - vMin;
        RenderSystem.enableBlend();
        BufferBuilder vertexBuffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            int width = xTile == xTileCount ? xRemainder : TEXTURE_SIZE;
            if (width == 0) {
                break;
            }

            int x = xTile * TEXTURE_SIZE;
            int maskRight = TEXTURE_SIZE - width;
            int shiftedX = x + TEXTURE_SIZE - maskRight;
            float uLocalDif = uDif * maskRight / TEXTURE_SIZE;
            float uLocalMin;
            float uLocalMax;
            if (TilingDirection.UP_RIGHT.right) {
                uLocalMin = uMin;
                uLocalMax = uMax - uLocalDif;
            }
            else {
                uLocalMin = uMin + uLocalDif;
                uLocalMax = uMax;
            }
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                int height = yTile == yTileCount ? yRemainder : TEXTURE_SIZE;
                if (height == 0) {
                    break;
                }

                int y = yOffset - (yTile + 1) * TEXTURE_SIZE;
                int maskTop = TEXTURE_SIZE - height;
                float vLocalDif = vDif * maskTop / TEXTURE_SIZE;
                float vLocalMin;
                float vLocalMax;
                if (TilingDirection.UP_RIGHT.down) {
                    vLocalMin = vMin;
                    vLocalMax = vMax - vLocalDif;
                }
                else {
                    vLocalMin = vMin + vLocalDif;
                    vLocalMax = vMax;
                }
                vertexBuffer.addVertex(matrix4f, x, y + TEXTURE_SIZE, 100).setUv(uLocalMin, vLocalMax);
                vertexBuffer.addVertex(matrix4f, shiftedX, y + TEXTURE_SIZE, 100).setUv(uLocalMax, vLocalMax);
                vertexBuffer.addVertex(matrix4f, shiftedX, y + maskTop, 100).setUv(uLocalMax, vLocalMin);
                vertexBuffer.addVertex(matrix4f, x, y + maskTop, 100).setUv(uLocalMin, vLocalMin);
            }
        }
        BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    @Override
    public void render(GuiGraphics guiGraphics, GasStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        int desiredHeight = Math.clamp(Math.round(height * (double) stack.getAmount() / capacityMb), 0, Integer.MAX_VALUE);
        if (desiredHeight < MIN_GAS_HEIGHT) {
            desiredHeight = MIN_GAS_HEIGHT;
        }
        if (desiredHeight > height) {
            desiredHeight = height;
        }
        int color = stack.getHint();
        guiGraphics.setColor(ARGB32.red(color) / 255.0f, ARGB32.green(color) / 255.0f, ARGB32.blue(color) / 255.0f, ARGB32.alpha(color) / 255.0f);
        drawTiledSprite(guiGraphics, height, width, desiredHeight, Gas.getGasTexture(stack.getGasHolder()));
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    @SuppressWarnings("removal")
    public List<Component> getTooltip(GasStack stack, TooltipFlag tooltipFlag) {
        Holder<Gas> gasHolder = stack.getGasHolder();
        if (gasHolder.value().isEmpty()) {
            return Collections.emptyList();
        }

        List<Component> tooltips = new ArrayList<>();
        collectTooltips(stack, tooltips, tooltipFlag);
        return tooltips;
    }

    @Override
    public void getTooltip(ITooltipBuilder builder, GasStack stack, TooltipFlag tooltipFlag) {
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

    private void collectTooltips(GasStack stack, List<Component> tooltips, TooltipFlag tooltipFlag) {
        Holder<Gas> gasHolder = stack.getGasHolder();
        if (gasHolder.value().isEmpty()) {
            return;
        }

        tooltips.add(Component.translatable(gasHolder.value().getTranslationKey()));
        if (tooltipMode == TooltipMode.SHOW_AMOUNT_AND_CAPACITY) {
            MutableComponent text = CCBLang.number(stack.getAmount()).style(ChatFormatting.AQUA).add(CCBLang.number(capacityMb).style(ChatFormatting.BLUE)).component();
            tooltips.add(text);
        }
        else if (tooltipMode == TooltipMode.SHOW_AMOUNT) {
            MutableComponent text = CCBLang.number(stack.getAmount()).style(ChatFormatting.AQUA).component();
            tooltips.add(text);
        }
        tooltips.add(CCBLang.text(gasHolder.getRegisteredName()).style(ChatFormatting.DARK_GRAY).component());
    }

    private static TooltipContext getTooltipContext() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return TooltipContext.EMPTY;
        }
        else if (Minecraft.getInstance().isSameThread()) {
            return TooltipContext.of(level);
        }

        return TooltipContext.of(level.registryAccess());
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
