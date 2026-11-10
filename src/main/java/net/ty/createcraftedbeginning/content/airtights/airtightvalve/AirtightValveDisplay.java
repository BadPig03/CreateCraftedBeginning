package net.ty.createcraftedbeginning.content.airtights.airtightvalve;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasPipeReadout.ValveState;
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

    void appendGoggleTooltip(List<Component> tooltip) {
        if (!GoggleTooltip.isVisible(tooltip, Section.PIPE_TELEMETRY)) {
            return;
        }

        ValveState readout = valve.getGasPipeReadout();
        CCBLang.translate("gui.airtight_valve").forGoggles(tooltip);
        CCBLang.translate("gui.airtight_valve.state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        boolean open = readout.open();
        CCBLang.translate(open ? "gui.airtight_valve.open" : "gui.airtight_valve.closed").style(open ? ChatFormatting.GREEN : ChatFormatting.RED).forGoggles(tooltip, 1);
    }
}
