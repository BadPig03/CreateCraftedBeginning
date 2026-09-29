package net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility.PipeReadout;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightRegulatorPumpDisplay {
    private final AirtightRegulatorPumpBlockEntity regulatorPump;

    AirtightRegulatorPumpDisplay(AirtightRegulatorPumpBlockEntity regulatorPump) {
        this.regulatorPump = regulatorPump;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        if (AirtightTelemetryVisibility.pipeReadout(regulatorPump) != PipeReadout.REGULATOR_CONFIGURATION) {
            return false;
        }

        if (!GoggleTooltip.isVisible(tooltip, Section.PIPE_TELEMETRY)) {
            return true;
        }

        CCBLang.translate("gui.airtight_regulator_pump").forGoggles(tooltip);
        CCBLang.translate("gui.airtight_regulator_pump.outlet_set_pressure").style(ChatFormatting.GRAY).forGoggles(tooltip);
        long outletSetPressurePa = regulatorPump.getOutletSetPressurePa();
        ChatFormatting outletPressureColor = GasPressureLimits.isOverpressure(outletSetPressurePa) ? ChatFormatting.RED : ChatFormatting.GOLD;
        CCBLang.text(GasPressure.formatAtm(outletSetPressurePa)).style(outletPressureColor).forGoggles(tooltip, 1);

        CCBLang.translate("gui.airtight_regulator_pump.max_pressure_rise").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.text(GasPressure.formatAtm(regulatorPump.getMaxPressureRisePa())).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }
}
