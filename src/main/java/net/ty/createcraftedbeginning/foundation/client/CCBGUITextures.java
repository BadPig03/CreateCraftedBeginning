package net.ty.createcraftedbeginning.foundation.client;

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
public enum CCBGUITextures implements ScreenElement, TextureSheetSegment {
    ARMORS("armors", 0, 0, 190, 138),
    HANDHELD_DRILL("handheld_drill", 0, 0, 215, 138),
    GAS_CANISTER_PACK("gas_canister_pack", 0, 0, 196, 144),
    GAS_CANISTER_PACK_CANISTER("gas_canister_pack", 0, 144, 16, 38),
    GAS_CANISTER_PACK_CREATIVE_CANISTER("gas_canister_pack", 16, 144, 16, 38),
    GAS_FILTER("gas_filter", 0, 0, 214, 104),

    GAS_FACTORY_GAUGE_RECIPE("gas_factory_gauge", 0, 0, 200, 160),
    GAS_FACTORY_GAUGE_RESTOCK("gas_factory_gauge_restocker", 0, 0, 200, 125),
    GAS_FACTORY_GAUGE_SET_GAS("gas_factory_gauge_set_gas", 0, 0, 184, 88),

    INDICATOR_RIGHT("widgets", 0, 0, 6, 18),
    INDICATOR_WHITE_RIGHT("widgets", 6, 0, 6, 18),
    INDICATOR_GREEN_RIGHT("widgets", 12, 0, 6, 18),
    INDICATOR_YELLOW_RIGHT("widgets", 18, 0, 6, 18),
    INDICATOR_RED_RIGHT("widgets", 24, 0, 6, 18),
    INDICATOR_LEFT("widgets", 0, 18, 6, 18),
    INDICATOR_WHITE_LEFT("widgets", 6, 18, 6, 18),
    INDICATOR_GREEN_LEFT("widgets", 12, 18, 6, 18),
    INDICATOR_YELLOW_LEFT("widgets", 18, 18, 6, 18),
    INDICATOR_RED_LEFT("widgets", 24, 18, 6, 18);

    public final ResourceLocation location;
    private final int width;
    private final int height;
    private final int startX;
    private final int startY;

    CCBGUITextures(String texturePath, int startX, int startY, int width, int height) {
        this(CCBAPI.MOD_ID, texturePath, startX, startY, width, height);
    }

    CCBGUITextures(String namespace, String texturePath, int startX, int startY, int width, int height) {
        location = ResourceLocation.fromNamespaceAndPath(namespace, "textures/gui/" + texturePath + ".png");
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
