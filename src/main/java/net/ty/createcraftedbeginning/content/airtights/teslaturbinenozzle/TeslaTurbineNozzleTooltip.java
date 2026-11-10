package net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineGeometry.NozzlePort;
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

    void appendGoggleTooltip(List<Component> tooltip) {
        CCBLang.translate("gui.tesla_turbine_nozzle.header").forGoggles(tooltip);
        CCBLang.translate("gui.tesla_turbine_nozzle.flow_direction").style(ChatFormatting.GRAY).forGoggles(tooltip);

        BlockState state = nozzle.getBlockState();
        Level level = nozzle.getLevel();
        NozzlePort port = level == null ? null : TeslaTurbineNozzleBlock.findPort(level, state.getValue(TeslaTurbineNozzleBlock.FACING).getOpposite(), nozzle.getBlockPos());
        boolean isClockwise = port == null ? state.getValue(TeslaTurbineNozzleBlock.CLOCKWISE) : port.clockwise();
        String directionKey = isClockwise ? "gui.tesla_turbine_nozzle.flow_direction.clockwise" : "gui.tesla_turbine_nozzle.flow_direction.counter_clockwise";
        CCBLang.translate(directionKey).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
    }
}
