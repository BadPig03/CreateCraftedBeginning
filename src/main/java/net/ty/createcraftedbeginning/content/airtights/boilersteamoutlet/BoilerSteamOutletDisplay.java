package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BoilerSteamOutletDisplay {
    private static final int GENERATION_BAR_SEGMENTS = 20;

    private BoilerSteamOutletDisplay() {
    }

    static void appendGoggleTooltip(List<Component> tooltip, double generationRatio) {
        CCBLang.translate("gui.boiler_steam_outlet.header").forGoggles(tooltip);
        int filledSegments = Mth.ceil(generationRatio * GENERATION_BAR_SEGMENTS);
        MutableComponent bar = Component.empty();
        for (int segment = 1; segment <= GENERATION_BAR_SEGMENTS; segment++) {
            ChatFormatting color;
            if (segment > filledSegments) {
                color = ChatFormatting.DARK_RED;
            }
            else if (segment == filledSegments) {
                color = ChatFormatting.GREEN;
            }
            else {
                color = ChatFormatting.DARK_GREEN;
            }
            bar.append(Component.literal("|").withStyle(color));
        }

        CCBLang.translate("gui.boiler_steam_outlet.steam_generation").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.builder().add(bar).forGoggles(tooltip, 1);
    }
}
