package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightManometerDisplay {
    private final AirtightManometerBlockEntity manometer;

    AirtightManometerDisplay(AirtightManometerBlockEntity manometer) {
        this.manometer = manometer;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        if (!GoggleTooltip.isVisible(tooltip, Section.PIPE_TELEMETRY)) {
            return false;
        }

        boolean overpressure = GasPressureLimits.isOverpressure(manometer.getMaxPressurePa());
        CCBLang.translate("gui.airtight_manometer").forGoggles(tooltip);
        CCBLang.translate("gui.airtight_manometer.max_pressure").style(ChatFormatting.GRAY).forGoggles(tooltip);
        long maximum = manometer.getAverageMaxPressurePa();
        long difference = manometer.getAverageDifferencePa();
        if (maximum < 0 || difference < 0) {
            CCBLang.text("--").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
            CCBLang.translate("gui.gas_pipe.pressure_difference").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.text("--").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
            return true;
        }

        ChatFormatting pressureColor = overpressure ? ChatFormatting.RED : ChatFormatting.GOLD;
        CCBLang.text(GasPressure.formatAtm(maximum)).style(pressureColor).forGoggles(tooltip, 1);
        CCBLang.translate("gui.gas_pipe.pressure_difference").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.text(GasPressure.formatAtm(difference)).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }
}
