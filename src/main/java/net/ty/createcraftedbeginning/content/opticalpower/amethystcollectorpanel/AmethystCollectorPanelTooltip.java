package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelOutput.Limitation;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystCollectorPanelTooltip {
    static void addToGoggleTooltip(List<Component> tooltip, AmethystCollectorPanelOutput output) {
        CCBLang.translate("gui.amethyst_collector_panel.header").forGoggles(tooltip);
        CCBLang.translate("gui.amethyst_collector_panel.power").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(output.powerLp()).translate("gui.unit.optical_power").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        Limitation limitation = output.limitation();
        if (limitation == Limitation.NONE) {
            return;
        }

        String reason = switch (limitation) {
            case OVERSIZED -> "gui.amethyst_collector_panel.oversized";
            case NO_SKYLIGHT -> "gui.amethyst_collector_panel.no_skylight";
            case NIGHT -> "gui.amethyst_collector_panel.night";
            case OBSTRUCTED -> "gui.amethyst_collector_panel.obstructed";
            case RAIN -> "gui.amethyst_collector_panel.rain";
            default -> throw new IllegalStateException("Unexpected collector output limitation: " + limitation + '.');
        };
        CCBLang.translate(reason).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
    }
}
