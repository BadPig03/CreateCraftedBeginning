package net.ty.createcraftedbeginning.content.opticalpower.amethystlens;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystLensDisplay {
    private AmethystLensDisplay() {
    }

    static void appendGoggleTooltip(List<Component> tooltip, Component name, int focalLength) {
        CCBLang.text("").add(name).forGoggles(tooltip);
        CCBLang.translate("gui.amethyst_lens.focal_length").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(focalLength).space().translate("gui.amethyst_lens.blocks").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
    }
}
