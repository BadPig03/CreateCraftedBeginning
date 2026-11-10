package net.ty.createcraftedbeginning.gas.visual;

import net.createmod.catnip.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.behaviour.OverpressureBehaviour;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OverpressureTooltips {
    private OverpressureTooltips() {
    }

    public static boolean addStatus(List<Component> tooltip, OverpressureBehaviour overpressure, GasStorageHandler gasHandler) {
        if (!GoggleTooltip.isVisible(tooltip, Section.OVERPRESSURE)) {
            return false;
        }

        return addStatus(tooltip, snapshot(overpressure, gasHandler));
    }

    public static boolean addStatus(List<Component> tooltip, @Nullable Snapshot snapshot) {
        if (!GoggleTooltip.isVisible(tooltip, Section.OVERPRESSURE) || snapshot == null) {
            return false;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.overpressure.structural_stress").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.text(String.valueOf(snapshot.stressPercent()) + '%').style(snapshot.stressColor()).forGoggles(tooltip, 1);
        Status status = snapshot.status();
        if (status != Status.RECOVERING) {
            return true;
        }

        CCBLang.translate("gui.overpressure.status").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(status.translationKey()).style(status.color()).forGoggles(tooltip, 1);
        return true;
    }

    public static boolean addWarning(List<Component> tooltip, OverpressureBehaviour overpressure, GasStorageHandler gasHandler) {
        return addWarning(tooltip, snapshot(overpressure, gasHandler));
    }

    public static boolean addWarning(List<Component> tooltip, @Nullable Snapshot snapshot) {
        if (snapshot == null) {
            return false;
        }

        Status status = snapshot.status();
        if (status == Status.RECOVERING) {
            return false;
        }

        String warningKey = status == Status.CRITICAL ? "gui.overpressure.warning.critical" : "gui.overpressure.warning.overpressure";
        CCBLang.translate("gui.overpressure.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        CCBLang.addToGoggles(tooltip, warningKey);
        return true;
    }

    public static @Nullable Snapshot snapshot(OverpressureBehaviour overpressure, GasStorageHandler gasHandler) {
        int trackedChannels = Math.min(overpressure.getChannelCount(), gasHandler.getTanks());
        boolean overpressureActive = false;
        boolean critical = false;
        for (int channel = 0; channel < trackedChannels; channel++) {
            boolean channelOverpressure = GasPressureLimits.isOverpressure(gasHandler.getTankPressurePa(channel));
            overpressureActive |= channelOverpressure;
            critical |= channelOverpressure && overpressure.isAtFailureThreshold(channel);
        }

        float stress = overpressure.getStressFraction();
        if (!overpressureActive && stress <= 0.0F) {
            return null;
        }

        Status status;
        if (critical) {
            status = Status.CRITICAL;
        }
        else if (overpressureActive) {
            status = Status.OVERPRESSURE;
        }
        else {
            status = Status.RECOVERING;
        }
        return new Snapshot(stress, status);
    }

    public enum Status {
        CRITICAL(ChatFormatting.DARK_RED),
        OVERPRESSURE(ChatFormatting.RED),
        RECOVERING(ChatFormatting.GREEN);

        private final ChatFormatting color;

        Status(ChatFormatting color) {
            this.color = color;
        }

        public String translationKey() {
            return "gui.overpressure.status." + Lang.asId(name());
        }

        public ChatFormatting color() {
            return color;
        }
    }

    public record Snapshot(float stress, Status status) {
        public Snapshot {
            stress = Mth.clamp(stress, 0.0F, 1.0F);
        }

        public int stressPercent() {
            return Mth.clamp(Math.round(stress * 100.0F), 0, 100);
        }

        public ChatFormatting stressColor() {
            if (stress >= 1.0F) {
                return ChatFormatting.DARK_RED;
            }

            if (stress >= 0.75F) {
                return ChatFormatting.RED;
            }

            if (stress >= 0.5F) {
                return ChatFormatting.GOLD;
            }

            if (status == Status.RECOVERING) {
                return ChatFormatting.GREEN;
            }

            return ChatFormatting.YELLOW;
        }
    }
}
