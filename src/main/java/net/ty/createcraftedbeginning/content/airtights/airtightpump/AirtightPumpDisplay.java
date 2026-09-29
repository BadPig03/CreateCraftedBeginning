package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility.PipeReadout;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightPumpDisplay {
    private final AirtightPumpBlockEntity pump;

    AirtightPumpDisplay(AirtightPumpBlockEntity pump) {
        this.pump = pump;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        if (AirtightTelemetryVisibility.pipeReadout(pump) != PipeReadout.PUMP_CONFIGURATION) {
            return false;
        }

        if (!GoggleTooltip.isVisible(tooltip, Section.PIPE_TELEMETRY)) {
            return true;
        }

        CCBLang.translate("gui.airtight_pump").forGoggles(tooltip);
        CCBLang.translate("gui.airtight_pump.max_pressure_boost").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.text(GasPressure.formatAtm(pump.getPumpMaxPressureBoostPa())).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }
}
