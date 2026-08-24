package net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class TeslaTurbineNozzleTooltip {
    private final TeslaTurbineNozzleBlockEntity nozzle;

    TeslaTurbineNozzleTooltip(TeslaTurbineNozzleBlockEntity nozzle) {
        this.nozzle = nozzle;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        CCBLang.translate("gui.tesla_turbine_nozzle.header").forGoggles(tooltip);
        CCBLang.translate("gui.tesla_turbine_nozzle.flow_direction").style(ChatFormatting.GRAY).forGoggles(tooltip);

        boolean isClockwise = nozzle.getBlockState().getValue(TeslaTurbineNozzleBlock.CLOCKWISE);
        String directionKey = isClockwise ? "gui.tesla_turbine_nozzle.flow_direction.clockwise" : "gui.tesla_turbine_nozzle.flow_direction.counter_clockwise";
        CCBLang.translate(directionKey).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }
}
