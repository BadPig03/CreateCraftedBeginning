package net.ty.createcraftedbeginning.compat.jei;

import net.createmod.catnip.gui.TextureSheetSegment;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum CCBJEITextures implements ScreenElement, TextureSheetSegment {
    JEI_COOLING("jei/widgets", 12, 178, 16, 16),
    JEI_COOLING_BACKGROUND("jei/widgets", 28, 178, 16, 16),
    JEI_DOWN_ARROW("jei/widgets", 0, 21, 18, 14),
    JEI_HEAT_BAR("jei/widgets", 0, 201, 169, 19),
    JEI_LONG_ARROW("jei/widgets", 19, 0, 71, 10),
    JEI_NO_HEAT_BAR("jei/widgets", 0, 221, 169, 19),
    JEI_PRESS_HEAD_TOOL("jei/widgets", 0, 72, 19, 19),
    JEI_QUESTION_MARK("jei/widgets", 0, 178, 12, 16),
    JEI_SHADOW("jei/widgets", 0, 56, 52, 11),
    JEI_WIND_CHARGING("jei/widgets", 44, 178, 16, 16),
    JEI_WIND_CHARGING_BACKGROUND("jei/widgets", 60, 178, 16, 16);

    public final ResourceLocation location;
    private final int width;
    private final int height;
    private final int startX;
    private final int startY;

    CCBJEITextures(String texturePath, int startX, int startY, int width, int height) {
        location = CCBAPI.asResource("textures/gui/" + texturePath + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }

    @Override
    public ResourceLocation getLocation() {
        return location;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, startX, startY, width, height);
    }

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y, Color color) {
        bind();
        UIRenderHelper.drawColoredTexture(graphics, color, x, y, startX, startY, width, height);
    }

    @Override
    public int getStartX() {
        return startX;
    }

    @Override
    public int getStartY() {
        return startY;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
