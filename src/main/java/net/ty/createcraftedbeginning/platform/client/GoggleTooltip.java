package net.ty.createcraftedbeginning.platform.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GoggleTooltip extends ArrayList<Component> {
    private final Set<Section> hiddenSections;

    public GoggleTooltip(Set<Section> hiddenSections) {
        this.hiddenSections = Set.copyOf(hiddenSections);
    }

    public static boolean isVisible(List<Component> tooltip, Section section) {
        return !(tooltip instanceof GoggleTooltip filtered) || !filtered.hiddenSections.contains(section);
    }

    public enum Section {
        GAS_STORAGE,
        OVERPRESSURE,
        PIPE_TELEMETRY,
        BREEZE_TIME,
        ITEM_STORAGE,
        FLUID_STORAGE,
        PRESS_HEAD,
        PROCESSING_MATERIAL
    }
}
