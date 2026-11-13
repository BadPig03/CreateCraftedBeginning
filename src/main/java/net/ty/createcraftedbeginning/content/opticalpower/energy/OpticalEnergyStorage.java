package net.ty.createcraftedbeginning.content.opticalpower.energy;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalEnergyStorage {
    private static final String COMPOUND_KEY_QUANTITY_LUMEN_SECONDS = "QuantityLumenSeconds";
    private static final double QUANTITY_RELATIVE_TOLERANCE = 1.0E-12;

    private final double capacityLumenSeconds;
    private double quantityLumenSeconds;

    public OpticalEnergyStorage(double capacityLumenSeconds) {
        if (!Double.isFinite(capacityLumenSeconds) || capacityLumenSeconds < 0) {
            throw new IllegalArgumentException("Optical capacity must be finite and non-negative; got " + capacityLumenSeconds + " lm·s.");
        }

        this.capacityLumenSeconds = capacityLumenSeconds;
    }

    public static double readQuantityLumenSeconds(CompoundTag compoundTag) {
        return decodeQuantity(compoundTag.get(COMPOUND_KEY_QUANTITY_LUMEN_SECONDS));
    }

    private static double decodeQuantity(@Nullable Tag quantityTag) {
        if (quantityTag == null) {
            return 0;
        }

        if (!(quantityTag instanceof NumericTag number)) {
            return Double.NaN;
        }

        double quantity = number.getAsDouble();
        if (!Double.isFinite(quantity) || quantity < 0) {
            return Double.NaN;
        }

        return quantity;
    }

    public double receive(double offeredLumenSeconds) {
        if (!Double.isFinite(offeredLumenSeconds) || offeredLumenSeconds < 0) {
            throw new IllegalArgumentException("Offered optical quantity must be finite and non-negative; got " + offeredLumenSeconds + " lm·s.");
        }

        double previousLumenSeconds = quantityLumenSeconds;
        quantityLumenSeconds = Math.min(capacityLumenSeconds, previousLumenSeconds + Math.min(offeredLumenSeconds, capacityLumenSeconds - previousLumenSeconds));
        return quantityLumenSeconds - previousLumenSeconds;
    }

    public double extract(double requestedLumenSeconds) {
        if (!Double.isFinite(requestedLumenSeconds) || requestedLumenSeconds < 0) {
            throw new IllegalArgumentException("Requested optical quantity must be finite and non-negative; got " + requestedLumenSeconds + " lm·s.");
        }

        double previousLumenSeconds = quantityLumenSeconds;
        quantityLumenSeconds = Math.max(0, previousLumenSeconds - Math.min(requestedLumenSeconds, previousLumenSeconds));
        return previousLumenSeconds - quantityLumenSeconds;
    }

    public double getQuantityLumenSeconds() {
        return quantityLumenSeconds;
    }

    public boolean hasAtLeast(double requiredLumenSeconds) {
        if (!Double.isFinite(requiredLumenSeconds) || requiredLumenSeconds < 0) {
            throw new IllegalArgumentException("Required optical quantity must be finite and non-negative; got " + requiredLumenSeconds + " lm·s.");
        }

        return quantityLumenSeconds >= requiredLumenSeconds * (1 - QUANTITY_RELATIVE_TOLERANCE);
    }

    public void write(CompoundTag compoundTag) {
        compoundTag.putDouble(COMPOUND_KEY_QUANTITY_LUMEN_SECONDS, quantityLumenSeconds);
    }

    public void readSaved(CompoundTag compoundTag, BlockPos pos) {
        Tag quantityTag = compoundTag.get(COMPOUND_KEY_QUANTITY_LUMEN_SECONDS);
        double quantity = decodeQuantity(quantityTag);
        if (quantityTag != null && Double.isNaN(quantity)) {
            Object actual = quantityTag instanceof NumericTag number ? number.getAsDouble() : quantityTag.getType().getPrettyName();
            CCBAPI.LOGGER.warn("Reset saved optical quantity '{}' at {} to 0 lm·s: expected a finite non-negative number, got {}.", COMPOUND_KEY_QUANTITY_LUMEN_SECONDS, pos, actual);
            quantity = 0;
        }

        restoreQuantity(quantity);
    }

    public void restoreQuantity(double quantity) {
        if (!Double.isFinite(quantity) || quantity < 0) {
            throw new IllegalArgumentException("Restored optical quantity must be finite and non-negative; got " + quantity + " lm·s.");
        }

        quantityLumenSeconds = Math.min(quantity, capacityLumenSeconds);
    }
}
