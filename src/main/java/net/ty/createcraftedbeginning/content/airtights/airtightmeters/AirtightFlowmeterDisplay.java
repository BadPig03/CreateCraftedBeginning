package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightFlowmeterDisplay {
    private final AirtightFlowmeterBlockEntity flowmeter;

    AirtightFlowmeterDisplay(AirtightFlowmeterBlockEntity flowmeter) {
        this.flowmeter = flowmeter;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        if (!AirtightTelemetryVisibility.canReadPipeFlow(flowmeter)) {
            return false;
        }

        if (!GoggleTooltip.isVisible(tooltip, Section.PIPE_TELEMETRY)) {
            return false;
        }

        CCBLang.translate("gui.airtight_flowmeter").forGoggles(tooltip);
        CCBLang.translate("gui.gas_pipe.flow_rate").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.text(GasUnitFormat.formatRate(flowmeter.getFlowRate())).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }
}
