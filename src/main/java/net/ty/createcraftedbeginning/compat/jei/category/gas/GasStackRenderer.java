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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.TooltipFlag;
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
@SuppressWarnings("unused")
public class GasStackRenderer implements IIngredientRenderer<GasStack> {
    private static final int TEXTURE_SIZE = 16;

    private static void drawTiledSprite(GuiGraphics graphics, TextureAtlasSprite sprite) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());

        float uMin = sprite.getU0();
        float uMax = sprite.getU1();
        float vMin = sprite.getV0();
        float vMax = sprite.getV1();

        RenderSystem.enableBlend();
        BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f pose = graphics.pose().last().pose();
        buffer.addVertex(pose, 0, TEXTURE_SIZE, 100).setUv(uMin, vMax);
        buffer.addVertex(pose, TEXTURE_SIZE, TEXTURE_SIZE, 100).setUv(uMax, vMax);
        buffer.addVertex(pose, TEXTURE_SIZE, 0, 100).setUv(uMax, vMin);
        buffer.addVertex(pose, 0, 0, 100).setUv(uMin, vMin);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static void collectTooltips(GasStack stack, List<Component> tooltips) {
        if (stack.isEmpty()) {
            return;
        }

        tooltips.add(Component.translatable(stack.getTranslationKey()));
        tooltips.add(CCBLang.text(stack.getGasType().getResourceLocation().toString()).style(ChatFormatting.DARK_GRAY).component());
    }

    @Override
    public void render(GuiGraphics guiGraphics, GasStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        int color = stack.getHint();
        guiGraphics.setColor(ARGB32.red(color) / 255.0f, ARGB32.green(color) / 255.0f, ARGB32.blue(color) / 255.0f, ARGB32.alpha(color) / 255.0f);
        drawTiledSprite(guiGraphics, Gas.getGasTexture(stack.getGasHolder()));
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public List<Component> getTooltip(GasStack stack, TooltipFlag tooltipFlag) {
        if (stack.isEmpty()) {
            return Collections.emptyList();
        }

        List<Component> tooltips = new ArrayList<>();
        collectTooltips(stack, tooltips);
        return tooltips;
    }

    @Override
    public void getTooltip(ITooltipBuilder builder, GasStack stack, TooltipFlag tooltipFlag) {
        List<Component> tooltips = new ArrayList<>();
        collectTooltips(stack, tooltips);
        builder.addAll(tooltips);
    }

    @Override
    public int getWidth() {
        return TEXTURE_SIZE;
    }

    @Override
    public int getHeight() {
        return TEXTURE_SIZE;
    }
}
