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
    private static final int HEAT_BAR_SEGMENTS = 30;

    private static final int HEATING_THRESHOLD_PERCENT = 50;

    static boolean addToGoggleTooltip(List<Component> tooltip, PhotothermalReceiverController controller) {
        int receivedPowerLp = controller.getReceivedPowerLp();
        int offeredPowerLp = controller.getOfferedPowerLp();
        int heatLevel = controller.getHeatLevel();
        CCBLang.translate("gui.photothermal_receiver.header").forGoggles(tooltip);
        CCBLang.translate("gui.photothermal_receiver.input").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(receivedPowerLp).translate("gui.unit.optical_power").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        CCBLang.translate("gui.photothermal_receiver.heat").style(ChatFormatting.GRAY).forGoggles(tooltip);
        String heatStatus = switch (heatLevel) {
            case 1 -> "gui.photothermal_receiver.heated";
            case 2 -> "gui.photothermal_receiver.superheated";
            default -> "gui.photothermal_receiver.inactive";
        };
        TemperatureCondition condition = switch (heatLevel) {
            case 1 -> TemperatureCondition.HEATED;
            case 2 -> TemperatureCondition.SUPERHEATED;
            default -> TemperatureCondition.NONE;
        };
        int heatColor = condition.getColor();
        CCBLang.translate(heatStatus).color(heatColor).forGoggles(tooltip, 1);
        addHeatProgress(tooltip, controller.getHeatProgress());
        if (offeredPowerLp > receivedPowerLp) {
            CCBLang.translate("gui.photothermal_receiver.excess").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.number(offeredPowerLp - receivedPowerLp).translate("gui.unit.optical_power").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        }
        return true;
    }

    private static void addHeatProgress(List<Component> tooltip, int heatProgress) {
        CCBLang.translate("gui.photothermal_receiver.progress").style(ChatFormatting.GRAY).forGoggles(tooltip);
        int filledSegments = Mth.clamp(heatProgress * HEAT_BAR_SEGMENTS / PhotothermalHeatState.MAX_HEAT_PROGRESS, 0, HEAT_BAR_SEGMENTS);
        int heatingMarker = HEAT_BAR_SEGMENTS * PhotothermalHeatState.HEATING_THRESHOLD / PhotothermalHeatState.MAX_HEAT_PROGRESS;
        MutableComponent heatBar = Component.empty();
        for (int segment = 1; segment <= HEAT_BAR_SEGMENTS; segment++) {
            ChatFormatting color = ChatFormatting.DARK_GRAY;
            if (segment <= filledSegments) {
                color = ChatFormatting.DARK_GREEN;
                if (segment == heatingMarker || segment == HEAT_BAR_SEGMENTS) {
                    color = ChatFormatting.GREEN;
                }
            }

            heatBar.append(Component.literal("|").withStyle(color));
        }
        CCBLang.builder().add(heatBar).space().add(CCBLang.number((double) heatProgress * HEATING_THRESHOLD_PERCENT / PhotothermalHeatState.HEATING_THRESHOLD).text("%").style(ChatFormatting.GRAY).component()).forGoggles(tooltip, 1);
    }
}
