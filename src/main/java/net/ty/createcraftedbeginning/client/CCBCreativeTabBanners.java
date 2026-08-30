package net.ty.createcraftedbeginning.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout.CCBCreativeTabSection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class CCBCreativeTabBanners {
    private static final int DEFAULT_SECONDARY_TITLE_COLOR = 0xFFCCCCCC;
    private static final int DEFAULT_TITLE_BACKGROUND = 0xBB001E3C;

    private CCBCreativeTabBanners() {
    }

    public static void render(GuiGraphics graphics, BannerLayout banner, int visibleRow) {
        int bannerY = 17 + visibleRow * 18;
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        Window window = minecraft.getWindow();
        Component title = banner.text();

        graphics.blitSprite(banner.sprite(), 8, bannerY, 162, 18);
        graphics.fill(10, bannerY + 2, Math.min(168, 16 + font.width(title)), bannerY + 16, banner.background());
        graphics.drawString(font, title, 13, bannerY + 5, banner.secondaryColor(), true);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 1);

        Matrix4f poseMatrix = poseStack.last().pose();
        Vector3f topLeft = poseMatrix.transformPosition(new Vector3f(13, bannerY + 5, 0));
        Vector3f bottomRight = poseMatrix.transformPosition(new Vector3f(13 + font.width(title), bannerY + 5 + font.lineHeight / 1.8f, 0));
        float guiScale = (float) window.getGuiScale();
        topLeft.mul(guiScale);
        bottomRight.mul(guiScale);

        RenderSystem.enableScissor((int) topLeft.x, window.getHeight() - (int) topLeft.y - Math.max(1, (int) (bottomRight.y - topLeft.y)), Math.max(1, (int) (bottomRight.x - topLeft.x)), Math.max(1, (int) (bottomRight.y - topLeft.y)));
        graphics.drawString(font, title, 13, bannerY + 5, banner.color(), false);
        RenderSystem.disableScissor();

        poseStack.popPose();
    }

    public static BannerLayout getBanner(CCBCreativeTabSection section) {
        return switch (section) {
            case AIRTIGHTS -> banner("airtights_banner", "item_groups.airtights_creative_tab", 0xFF878FAA, 0xBB313436);
            case OPTICAL_POWER -> banner("optical_power_banner", "item_groups.optical_power_creative_tab", DEFAULT_SECONDARY_TITLE_COLOR, DEFAULT_TITLE_BACKGROUND);
            case ENDS -> banner("ends_banner", "item_groups.ends_creative_tab", DEFAULT_SECONDARY_TITLE_COLOR, DEFAULT_TITLE_BACKGROUND);
            case DECORATIONS -> banner("decorations_banner", "item_groups.decorations_creative_tab", DEFAULT_SECONDARY_TITLE_COLOR, DEFAULT_TITLE_BACKGROUND);
            case CANISTERS -> banner("canisters_banner", "item_groups.canisters_creative_tab", 0xFFB160AA, 0xBB5B2E64);
        };
    }

    private static BannerLayout banner(String spritePath, String titleKey, int secondaryColor, int background) {
        return new BannerLayout(CCBAPI.asResource(spritePath), CCBLang.translateDirect(titleKey), 0xFFDDDDDD, secondaryColor, background);
    }

    public record BannerLayout(ResourceLocation sprite, Component text, int color, int secondaryColor, int background) {}
}
