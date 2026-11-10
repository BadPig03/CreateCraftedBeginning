package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserEmitterDisplay {
    private LaserEmitterDisplay() {
    }

    static void appendGoggleTooltip(List<Component> tooltip, double fluxLm) {
        CCBLang.translate("gui.laser_emitter.header").forGoggles(tooltip);
        CCBLang.translate("gui.laser_emitter.power").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(fluxLm).translate("gui.unit.luminous_flux").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
    }
}
