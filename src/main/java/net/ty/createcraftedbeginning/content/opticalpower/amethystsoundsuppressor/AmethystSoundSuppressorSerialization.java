package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.api.CCBAPI;
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

    static @Nullable ClientSnapshot readClient(CompoundTag tag, BlockPos pos, boolean reportFailure) {
        double quantity = OpticalEnergyStorage.readQuantityLumenSeconds(tag);
        if (Double.isNaN(quantity)) {
            if (reportFailure) {
                CCBAPI.LOGGER.warn("Rejected the amethyst sound suppressor update at {}: 'QuantityLumenSeconds' must be a finite non-negative number in lm·s.", pos);
            }

            return null;
        }

        boolean hasReceived = tag.contains(COMPOUND_KEY_RECEIVED_FLUX_LM);
        boolean hasQualified = tag.contains(COMPOUND_KEY_QUALIFIED_FLUX_LM);
        double received = hasReceived ? NbtValues.getDoubleOrDefault(tag, COMPOUND_KEY_RECEIVED_FLUX_LM, Double.NaN) : 0;
        double qualified = hasQualified ? NbtValues.getDoubleOrDefault(tag, COMPOUND_KEY_QUALIFIED_FLUX_LM, Double.NaN) : 0;
        if (!Double.isFinite(received) || received < 0 || !Double.isFinite(qualified) || qualified < 0 || hasReceived && hasQualified && qualified > received) {
            if (reportFailure) {
                CCBAPI.LOGGER.warn("Rejected the amethyst sound suppressor update at {}: luminous flux must be numeric and finite with 0 <= qualified <= received when both fields are present; got received={}, qualified={} lm.", pos, received, qualified);
            }

            return null;
        }

        long growthTick = readGrowthTick(tag);
        if (!hasReceived || !hasQualified) {
            return new ClientSnapshot(quantity, growthTick, 0, 0);
        }

        return new ClientSnapshot(quantity, growthTick, received, qualified);
    }

    static long readGrowthTick(CompoundTag tag) {
        if (!tag.contains(COMPOUND_KEY_LAST_GROWTH_TICK)) {
            return -1;
        }

        return tag.getLong(COMPOUND_KEY_LAST_GROWTH_TICK);
    }

    record ClientSnapshot(double quantityLumenSeconds, long lastGrowthTick, double receivedFluxLm, double qualifiedFluxLm) {}
}
