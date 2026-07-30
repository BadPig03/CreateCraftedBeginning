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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout.CCBCreativeTabSection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class CCBCreativeTabBanners {
    public static final int DEFAULT_SECONDARY_TITLE_COLOR = 0xFFCCCCCC;
    public static final int DEFAULT_TITLE_BACKGROUND = 0xBB001E3C;

    private CCBCreativeTabBanners() {
    }

    public static void render(GuiGraphics graphics, BannerLayout banner, int visibleRow) {
        int y = 17 + visibleRow * 18;
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        Window window = minecraft.getWindow();
        Component text = banner.text();

        graphics.blitSprite(banner.sprite(), 8, y, 162, 18);
        graphics.fill(10, y + 2, Math.min(168, 16 + font.width(text)), y + 16, banner.background());
        graphics.drawString(font, text, 13, y + 5, banner.secondaryColor(), true);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 1);

        Matrix4f pose = poseStack.last().pose();
        Vector3f topLeft = pose.transformPosition(new Vector3f(13, y + 5, 0));
        Vector3f bottomRight = pose.transformPosition(new Vector3f(13 + font.width(text), y + 5 + font.lineHeight / 1.8f, 0));
        float guiScale = (float) window.getGuiScale();
        topLeft.mul(guiScale);
        bottomRight.mul(guiScale);

        RenderSystem.enableScissor((int) topLeft.x, window.getHeight() - (int) topLeft.y - Math.max(1, (int) (bottomRight.y - topLeft.y)), Math.max(1, (int) (bottomRight.x - topLeft.x)), Math.max(1, (int) (bottomRight.y - topLeft.y)));
        graphics.drawString(font, text, 13, y + 5, banner.color(), false);
        RenderSystem.disableScissor();

        poseStack.popPose();
    }

    public static BannerLayout getBanner(CCBCreativeTabSection section) {
        return switch (section) {
            case AIRTIGHTS -> banner("airtights_banner", "item_groups.airtights_creative_tab", 0xFF878FAA, 0xBB313436);
            case PHOTO_STRESSES -> banner("photo_stresses_banner", "item_groups.photo_stresses_creative_tab", DEFAULT_SECONDARY_TITLE_COLOR, DEFAULT_TITLE_BACKGROUND);
            case ENDS -> banner("ends_banner", "item_groups.ends_creative_tab", DEFAULT_SECONDARY_TITLE_COLOR, DEFAULT_TITLE_BACKGROUND);
            case DECORATIONS -> banner("decorations_banner", "item_groups.decorations_creative_tab", DEFAULT_SECONDARY_TITLE_COLOR, DEFAULT_TITLE_BACKGROUND);
            case CANISTERS -> banner("canisters_banner", "item_groups.canisters_creative_tab", 0xFFB160AA, 0xBB5B2E64);
        };
    }

    private static BannerLayout banner(String spritePath, String titleKey, int secondaryColor, int background) {
        return new BannerLayout(CreateCraftedBeginning.asResource(spritePath), CCBLang.translateDirect(titleKey), 0xFFDDDDDD, secondaryColor, background);
    }

    public record BannerLayout(ResourceLocation sprite, Component text, int color, int secondaryColor, int background) {}
}
