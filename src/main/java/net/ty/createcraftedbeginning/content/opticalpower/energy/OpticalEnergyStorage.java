package net.ty.createcraftedbeginning.content.opticalpower.energy;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.ApiStatus.Internal;

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

    public void read(CompoundTag compoundTag) {
        Tag quantityTag = compoundTag.get(COMPOUND_KEY_QUANTITY_LUMEN_SECONDS);
        if (quantityTag == null) {
            quantityLumenSeconds = 0;
            return;
        }

        if (!(quantityTag instanceof NumericTag numericTag)) {
            throw new IllegalArgumentException("Saved optical quantity '" + COMPOUND_KEY_QUANTITY_LUMEN_SECONDS + "' must be numeric; got " + quantityTag.getType().getPrettyName() + '.');
        }

        double savedLumenSeconds = numericTag.getAsDouble();
        if (!Double.isFinite(savedLumenSeconds) || savedLumenSeconds < 0) {
            throw new IllegalArgumentException("Saved optical quantity must be finite and non-negative; got " + savedLumenSeconds + " lm·s.");
        }

        quantityLumenSeconds = Math.min(savedLumenSeconds, capacityLumenSeconds);
    }
}
