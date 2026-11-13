package net.ty.createcraftedbeginning.content.opticalpower.photothermalchamber;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class PhotothermalChamberTooltip {
    private static final int COOLING_BAR_SEGMENTS = 30;

    private PhotothermalChamberTooltip() {
    }

    static void addToGoggleTooltip(List<Component> tooltip, double incidentFluxLm, int outputHeatLevel, int heatProgress, boolean unavailable) {
        CCBLang.translate("gui.photothermal_chamber.header").forGoggles(tooltip);
        if (unavailable) {
            CCBLang.translate("gui.optical_power.state_unavailable").style(ChatFormatting.GRAY).forGoggles(tooltip);
            return;
        }

        CCBLang.translate("gui.photothermal_chamber.input").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(incidentFluxLm).translate("gui.unit.luminous_flux").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        CCBLang.translate("gui.photothermal_chamber.heat").style(ChatFormatting.GRAY).forGoggles(tooltip);
        String heatStatus = switch (outputHeatLevel) {
            case 1 -> "gui.photothermal_chamber.heated";
            case 2 -> "gui.photothermal_chamber.superheated";
            default -> "gui.photothermal_chamber.inactive";
        };
        TemperatureCondition condition = switch (outputHeatLevel) {
            case 1 -> TemperatureCondition.HEATED;
            case 2 -> TemperatureCondition.SUPERHEATED;
            default -> TemperatureCondition.NONE;
        };
        int heatColor = condition.getColor();
        CCBLang.translate(heatStatus).color(heatColor).forGoggles(tooltip, 1);
        if (incidentFluxLm != 0 || heatProgress <= 0) {
            return;
        }

        CCBLang.translate("gui.photothermal_chamber.cooling").style(ChatFormatting.GRAY).forGoggles(tooltip);
        double coolingRatio = Mth.clamp((double) heatProgress / PhotothermalHeatState.MAX_HEAT_PROGRESS, 0, 1);
        int filledSegments = (int) Math.round(coolingRatio * COOLING_BAR_SEGMENTS);
        MutableComponent coolingBar = Component.empty();
        for (int segment = 1; segment <= COOLING_BAR_SEGMENTS; segment++) {
            ChatFormatting color = segment <= filledSegments ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY;
            coolingBar.append(Component.literal("|").withStyle(color));
        }

        CCBLang.builder().add(coolingBar).forGoggles(tooltip, 1);
    }
}
