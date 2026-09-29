package net.ty.createcraftedbeginning.gas.visual;

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

        Snapshot snapshot = snapshot(overpressure, gasHandler);
        if (snapshot == null) {
            return false;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.overpressure.structural_stress").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.text(String.valueOf(snapshot.stressPercent()) + '%').style(snapshot.stressColor()).forGoggles(tooltip, 1);

        CCBLang.translate("gui.overpressure.status").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(snapshot.status().translationKey()).style(snapshot.status().color()).forGoggles(tooltip, 1);
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
        CRITICAL("gui.overpressure.status.critical", ChatFormatting.DARK_RED),
        OVERPRESSURE("gui.overpressure.status.overpressure", ChatFormatting.RED),
        RECOVERING("gui.overpressure.status.recovering", ChatFormatting.GREEN);

        private final String translationKey;
        private final ChatFormatting color;

        Status(String translationKey, ChatFormatting color) {
            this.translationKey = translationKey;
            this.color = color;
        }

        public String translationKey() {
            return translationKey;
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
