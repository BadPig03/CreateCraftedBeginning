package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.content.opticalpower.energy.OpticalEnergyStorage;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystSoundSuppressorSerialization {
    private static final String COMPOUND_KEY_SHOW_OUTLINE = "ShowOutline";
    private static final String COMPOUND_KEY_RECEIVED_FLUX_LM = "ReceivedFluxLm";
    private static final String COMPOUND_KEY_QUALIFIED_FLUX_LM = "QualifiedFluxLm";
    private static final String COMPOUND_KEY_LAST_GROWTH_TICK = "LastGrowthTick";

    private AmethystSoundSuppressorSerialization() {
    }

    public static void writeShowOutline(CompoundTag tag, boolean showOutline) {
        tag.putBoolean(COMPOUND_KEY_SHOW_OUTLINE, showOutline);
    }

    public static void writeIllumination(CompoundTag tag, double receivedFluxLm, double qualifiedFluxLm) {
        tag.putDouble(COMPOUND_KEY_RECEIVED_FLUX_LM, receivedFluxLm);
        tag.putDouble(COMPOUND_KEY_QUALIFIED_FLUX_LM, qualifiedFluxLm);
    }

    static void write(CompoundTag tag, OpticalEnergyStorage storage, long lastGrowthTick, double receivedFluxLm, double qualifiedFluxLm, boolean clientPacket) {
        storage.write(tag);
        tag.putLong(COMPOUND_KEY_LAST_GROWTH_TICK, lastGrowthTick);
        if (!clientPacket) {
            return;
        }

        writeIllumination(tag, receivedFluxLm, qualifiedFluxLm);
    }

    static boolean readShowOutline(@Nullable CompoundTag tag, boolean fallback) {
        if (tag == null) {
            return fallback;
        }

        return NbtValues.getBooleanOrDefault(tag, COMPOUND_KEY_SHOW_OUTLINE, fallback);
    }

    static long readGrowth(CompoundTag tag, OpticalEnergyStorage storage) {
        storage.read(tag);
        if (!tag.contains(COMPOUND_KEY_LAST_GROWTH_TICK)) {
            return -1;
        }

        return tag.getLong(COMPOUND_KEY_LAST_GROWTH_TICK);
    }

    static Illumination readIllumination(CompoundTag tag) {
        double received = NbtValues.getDoubleOrDefault(tag, COMPOUND_KEY_RECEIVED_FLUX_LM, Double.NaN);
        double qualified = NbtValues.getDoubleOrDefault(tag, COMPOUND_KEY_QUALIFIED_FLUX_LM, Double.NaN);
        if (!Double.isFinite(received) || received < 0 || !Double.isFinite(qualified) || qualified < 0 || qualified > received) {
            return new Illumination(0, 0);
        }

        return new Illumination(received, qualified);
    }

    record Illumination(double receivedFluxLm, double qualifiedFluxLm) {}
}
