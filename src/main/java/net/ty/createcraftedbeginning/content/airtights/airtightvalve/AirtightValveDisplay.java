package net.ty.createcraftedbeginning.content.airtights.airtightvalve;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility.PipeReadout;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightValveDisplay {
    private final AirtightValveBlockEntity valve;

    AirtightValveDisplay(AirtightValveBlockEntity valve) {
        this.valve = valve;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        if (AirtightTelemetryVisibility.pipeReadout(valve) != PipeReadout.VALVE_STATE) {
            return false;
        }

        if (!GoggleTooltip.isVisible(tooltip, Section.PIPE_TELEMETRY)) {
            return true;
        }

        CCBLang.translate("gui.airtight_valve").forGoggles(tooltip);
        CCBLang.translate("gui.airtight_valve.state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(valve.isOpen() ? "gui.airtight_valve.open" : "gui.airtight_valve.closed").style(valve.isOpen() ? ChatFormatting.GREEN : ChatFormatting.RED).forGoggles(tooltip, 1);
        return true;
    }
}
