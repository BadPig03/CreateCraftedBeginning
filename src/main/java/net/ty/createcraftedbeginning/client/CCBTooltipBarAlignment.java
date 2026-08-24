package net.ty.createcraftedbeginning.client;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class CCBTooltipBarAlignment {
    private static final ResourceLocation PIXEL_SPACING_FONT = CCBAPI.asResource("pixel_spacing");
    private static final String ONE_PIXEL_SPACE = "\uE000";
    private static final String BASE_LEADER = "....... ";

    private CCBTooltipBarAlignment() {
    }

    public static void addAlignedBars(List<Component> tooltip, int indent, List<? extends Component> labels, List<? extends Component> bars) {
        if (labels.size() != bars.size()) {
            throw new IllegalArgumentException("Progress-bar labels and bars must have the same size");
        }
        if (labels.isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        int maximumLabelWidth = labels.stream().mapToInt(font::width).max().orElse(0);
        int trailingGapWidth = font.width(" ");
        int minimumLeaderWidth = font.width(BASE_LEADER);
        int dotWidth = Math.max(1, font.width("."));
        int targetBarStart = maximumLabelWidth + minimumLeaderWidth;
        for (int labelIndex = 0; labelIndex < labels.size(); labelIndex++) {
            Component label = labels.get(labelIndex);
            int leaderWidth = Math.max(trailingGapWidth, targetBarStart - font.width(label));
            int dotCount = Math.max(0, leaderWidth - trailingGapWidth) / dotWidth;
            MutableComponent tooltipLine = label.copy();
            if (dotCount > 0) {
                tooltipLine.append(Component.literal(".".repeat(dotCount)).withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltipLine.append(createPixelSpacing(leaderWidth - dotCount * dotWidth));
            tooltipLine.append(bars.get(labelIndex));
            CCBLang.builder().add(tooltipLine).forGoggles(tooltip, indent);
        }
    }

    private static Component createPixelSpacing(int pixels) {
        if (pixels <= 0) {
            return Component.empty();
        }
        return Component.literal(ONE_PIXEL_SPACE.repeat(pixels)).withStyle(style -> style.withFont(PIXEL_SPACING_FONT));
    }
}
