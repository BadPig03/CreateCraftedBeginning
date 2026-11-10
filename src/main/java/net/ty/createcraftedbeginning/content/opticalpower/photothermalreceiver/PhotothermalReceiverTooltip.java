package net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver;

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
final class PhotothermalReceiverTooltip {
    private static final int COOLING_BAR_SEGMENTS = 30;

    private PhotothermalReceiverTooltip() {
    }

    static void addToGoggleTooltip(List<Component> tooltip, double incidentFluxLm, int outputHeatLevel, int heatProgress) {
        boolean cooling = incidentFluxLm == 0 && heatProgress > 0;
        CCBLang.translate("gui.photothermal_receiver.header").forGoggles(tooltip);
        CCBLang.translate("gui.photothermal_receiver.input").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(incidentFluxLm).translate("gui.unit.luminous_flux").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        CCBLang.translate("gui.photothermal_receiver.heat").style(ChatFormatting.GRAY).forGoggles(tooltip);
        String heatStatus = switch (outputHeatLevel) {
            case 1 -> "gui.photothermal_receiver.heated";
            case 2 -> "gui.photothermal_receiver.superheated";
            default -> "gui.photothermal_receiver.inactive";
        };
        TemperatureCondition condition = switch (outputHeatLevel) {
            case 1 -> TemperatureCondition.HEATED;
            case 2 -> TemperatureCondition.SUPERHEATED;
            default -> TemperatureCondition.NONE;
        };
        int heatColor = condition.getColor();
        CCBLang.translate(heatStatus).color(heatColor).forGoggles(tooltip, 1);
        if (!cooling) {
            return;
        }

        CCBLang.translate("gui.photothermal_receiver.cooling").style(ChatFormatting.GRAY).forGoggles(tooltip);
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
