package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadout.PumpConfiguration;
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

    void appendGoggleTooltip(List<Component> tooltip) {
        if (!GoggleTooltip.isVisible(tooltip, Section.PIPE_TELEMETRY)) {
            return;
        }

        PumpConfiguration readout = pump.getGasPipeReadout();
        CCBLang.translate("gui.airtight_pump").forGoggles(tooltip);
        CCBLang.translate("gui.airtight_pump.max_pressure_boost").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.text(GasPressure.formatAtm(readout.maxPressureBoostPa())).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
    }
}
