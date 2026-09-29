package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility;
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
        if (!AirtightTelemetryVisibility.canReadPipePressure(manometer)) {
            return false;
        }

        if (!GoggleTooltip.isVisible(tooltip, Section.PIPE_TELEMETRY)) {
            return false;
        }

        CCBLang.translate("gui.airtight_manometer").forGoggles(tooltip);
        CCBLang.translate("gui.airtight_manometer.max_pressure").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (!manometer.hasPressureReading()) {
            CCBLang.text("--").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
            CCBLang.translate("gui.gas_pipe.pressure_difference").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.text("--").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
            return true;
        }

        long maxPressurePa = manometer.getMaxPressurePa();
        ChatFormatting pressureColor = GasPressureLimits.isOverpressure(maxPressurePa) ? ChatFormatting.RED : ChatFormatting.GOLD;
        CCBLang.text(GasPressure.formatAtm(maxPressurePa)).style(pressureColor).forGoggles(tooltip, 1);
        CCBLang.translate("gui.gas_pipe.pressure_difference").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.text(GasPressure.formatAtm(manometer.getPressureDifferencePa())).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }
}
