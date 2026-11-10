package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Locale;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorDisplay {
    private AmethystMirrorDisplay() {
    }

    static Component getAimStatus(AmethystMirrorAimStatus status, boolean showInstructions) {
        if (showInstructions) {
            return Component.translatable("createcraftedbeginning.gui.amethyst_mirror.aim.instructions").withStyle(ChatFormatting.WHITE);
        }

        AmethystMirrorAimStatus message = switch (status) {
            case NO_TARGET, SELF -> AmethystMirrorAimStatus.NO_TARGET;
            case NO_INPUT, MULTIPLE_INPUTS -> AmethystMirrorAimStatus.NO_INPUT;
            case CHECKING, READY, TOO_FAR, SUBMITTED, CANCELLED -> status;
            case UNSUPPORTED, NO_SOLUTION, MIRROR_FRAME, BEAM_RANGE, BLOCKED, INVALID -> AmethystMirrorAimStatus.BLOCKED;
        };
        int color = switch (message) {
            case NO_TARGET, SUBMITTED, CANCELLED -> 0xFFFFFF;
            case CHECKING -> 0xAAAAAA;
            case READY -> 0x9EDE73;
            default -> 0xFF7171;
        };
        return Component.translatable("createcraftedbeginning.gui.amethyst_mirror.aim." + message.getSerializedName()).withStyle(style -> style.withColor(color));
    }

    static void appendGoggleTooltip(List<Component> tooltip, Quaternionf rotation) {
        CCBLang.translate("gui.amethyst_mirror.title").forGoggles(tooltip);
        Vector3f angles = rotation.getEulerAnglesYXZ(new Vector3f());
        CCBLang.translate("gui.amethyst_mirror.yaw").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.text(String.format(Locale.ROOT, "%.1f°", Math.round(Math.toDegrees(angles.y) * 10) / 10.0)).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        CCBLang.translate("gui.amethyst_mirror.pitch").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.text(String.format(Locale.ROOT, "%.1f°", Math.round(Math.toDegrees(angles.x) * 10) / 10.0)).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        CCBLang.translate("gui.amethyst_mirror.roll").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.text(String.format(Locale.ROOT, "%.1f°", Math.round(Math.toDegrees(angles.z) * 10) / 10.0)).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
    }
}
