package net.ty.createcraftedbeginning.content.opticalpower.amethystloader;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystLoaderSerialization {
    private static final String COMPOUND_KEY_NETWORK_NODE = "LoadingNodeId";
    private static final String COMPOUND_KEY_BEAM_COLOR = "BeamColor";
    private static final String COMPOUND_KEY_BEAM_ENABLED = "BeamEnabled";
    private static final String COMPOUND_KEY_ACTIVE = "Active";
    private static final String COMPOUND_KEY_DISPLAY_NAME = "DisplayName";
    private static final String COMPOUND_KEY_SHOW_OUTLINE = "ShowOutline";
    private static final String COMPOUND_KEY_LOADING_ACTIVE = "MovingLoadingActive";

    private AmethystLoaderSerialization() {
    }

    public static @Nullable UUID readNetworkNodeId(@Nullable CompoundTag tag) {
        if (tag == null || !tag.hasUUID(COMPOUND_KEY_NETWORK_NODE)) {
            return null;
        }

        return tag.getUUID(COMPOUND_KEY_NETWORK_NODE);
    }

    static void write(AmethystLoaderBlockEntity loader, CompoundTag tag, boolean clientPacket) {
        UUID identity = loader.networkNodeId();
        if (identity != null) {
            tag.putUUID(COMPOUND_KEY_NETWORK_NODE, identity);
        }
        writeShowOutline(tag, loader.showOutline());
        writeBeamEnabled(tag, loader.beamEnabled());
        writeBeamColor(tag, loader.beamColor());
        tag.putString(COMPOUND_KEY_DISPLAY_NAME, loader.displayName());
        if (!clientPacket) {
            return;
        }

        tag.putBoolean(COMPOUND_KEY_ACTIVE, loader.isLoadingActive());
    }

    static void read(AmethystLoaderBlockEntity loader, CompoundTag tag, boolean clientPacket) {
        UUID identity = readNetworkNodeId(tag);
        boolean showOutline = readShowOutline(tag);
        boolean beamEnabled = readBeamEnabled(tag);
        String displayName = tag.getString(COMPOUND_KEY_DISPLAY_NAME);
        boolean active = loader.isLoadingActive();
        if (clientPacket) {
            active = tag.getBoolean(COMPOUND_KEY_ACTIVE);
        }
        loader.restoreState(identity, showOutline, beamEnabled, displayName, active, readBeamColor(tag));
    }

    static int readBeamColor(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains(COMPOUND_KEY_BEAM_COLOR)) {
            return LoadingNetworkSavedData.UNBOUND_COLOR;
        }

        return tag.getInt(COMPOUND_KEY_BEAM_COLOR);
    }

    static void writeBeamColor(CompoundTag tag, int color) {
        tag.putInt(COMPOUND_KEY_BEAM_COLOR, color);
    }

    static boolean readBeamEnabled(@Nullable CompoundTag compoundTag) {
        return compoundTag == null || NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_BEAM_ENABLED, true);
    }

    static void writeBeamEnabled(CompoundTag compoundTag, boolean beamEnabled) {
        compoundTag.putBoolean(COMPOUND_KEY_BEAM_ENABLED, beamEnabled);
    }

    static boolean readShowOutline(@Nullable CompoundTag compoundTag) {
        return compoundTag == null || NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_SHOW_OUTLINE, true);
    }

    static void writeShowOutline(CompoundTag compoundTag, boolean showOutline) {
        compoundTag.putBoolean(COMPOUND_KEY_SHOW_OUTLINE, showOutline);
    }

    static boolean readMovingActive(@Nullable CompoundTag compoundTag) {
        return compoundTag != null && compoundTag.getBoolean(COMPOUND_KEY_LOADING_ACTIVE);
    }

    static boolean hasMovingActive(@Nullable CompoundTag compoundTag, boolean active) {
        return compoundTag != null && compoundTag.contains(COMPOUND_KEY_LOADING_ACTIVE) && compoundTag.getBoolean(COMPOUND_KEY_LOADING_ACTIVE) == active;
    }

    static void writeMovingActive(CompoundTag compoundTag, boolean active) {
        compoundTag.putBoolean(COMPOUND_KEY_LOADING_ACTIVE, active);
    }
}
