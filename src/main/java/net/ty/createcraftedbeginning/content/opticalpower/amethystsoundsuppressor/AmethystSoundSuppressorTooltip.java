package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystSoundSuppressorTooltip {
    private static final int STORAGE_BAR_SEGMENTS = 20;

    private AmethystSoundSuppressorTooltip() {
    }

    static void addToGoggleTooltip(List<Component> tooltip, AmethystSoundSuppressorController controller, boolean unavailable) {
        CCBLang.translate("gui.amethyst_sound_suppressor.header").forGoggles(tooltip);
        if (unavailable) {
            CCBLang.translate("gui.optical_power.state_unavailable").style(ChatFormatting.GRAY).forGoggles(tooltip);
            return;
        }

        CCBLang.translate("gui.amethyst_sound_suppressor.power").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(controller.getReceivedFluxLm()).translate("gui.unit.luminous_flux").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        CCBLang.translate("gui.amethyst_sound_suppressor.storage").style(ChatFormatting.GRAY).forGoggles(tooltip);
        double quantity = controller.getQuantityLumenSeconds();
        int completedSegments = Mth.floor(quantity / AmethystSoundSuppressorController.CAPACITY_LUMEN_SECONDS * STORAGE_BAR_SEGMENTS);
        MutableComponent bar = Component.empty();
        for (int segment = 0; segment < STORAGE_BAR_SEGMENTS; segment++) {
            ChatFormatting color = ChatFormatting.DARK_GRAY;
            if (segment < completedSegments) {
                color = ChatFormatting.GREEN;
            }
            bar.append(Component.literal("|").withStyle(color));
        }

        CCBLang.builder().add(bar).forGoggles(tooltip, 1);
    }
}
