package net.ty.createcraftedbeginning.api.gas.pressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GasPressureTier {
    VACUUM(0),
    AMBIENT(1),
    LOW(6),
    MEDIUM(10),
    HIGH(14),
    EXTREME(GasPressureLimits.SAFE_PRESSURE_ATM);

    public static final long EXTREME_MAX_PRESSURE_PA = EXTREME.maximumPressurePa;
    public static final long MAX_DEFINED_PRESSURE_PA = EXTREME_MAX_PRESSURE_PA;

    private static final GasPressureTier[] VALUES = values();

    private final int maximumPressureAtm;
    private final long maximumPressurePa;

    GasPressureTier(int maximumPressureAtm) {
        this.maximumPressureAtm = maximumPressureAtm;
        maximumPressurePa = GasPressure.pascals(maximumPressureAtm);
    }

    public static GasPressureTier resolve(long pressurePa) {
        long normalizedPressurePa = normalizePressurePa(pressurePa);
        for (GasPressureTier tier : VALUES) {
            if (normalizedPressurePa > tier.maximumPressurePa) {
                continue;
            }

            return tier;
        }
        return EXTREME;
    }

    public static GasPressureTier highestSatisfied(long pressurePa) {
        return resolve(pressurePa);
    }

    public static Optional<GasPressureTier> nearestUnsatisfied(long pressurePa) {
        return nextTier(highestSatisfied(pressurePa));
    }

    public static Optional<GasPressureTier> nextTier(GasPressureTier tier) {
        int nextOrdinal = tier.ordinal() + 1;
        if (nextOrdinal >= VALUES.length) {
            return Optional.empty();
        }

        return Optional.of(VALUES[nextOrdinal]);
    }

    @SuppressWarnings("unused")
    public static Optional<GasPressureTier> previousTier(GasPressureTier tier) {
        int previousOrdinal = tier.ordinal() - 1;
        if (previousOrdinal < 0) {
            return Optional.empty();
        }

        return Optional.of(VALUES[previousOrdinal]);
    }

    public static boolean satisfies(long pressurePa, GasPressureTier requiredTier) {
        return highestSatisfied(pressurePa).ordinal() >= requiredTier.ordinal();
    }

    public static long minimumPressureFor(GasPressureTier tier) {
        if (tier == VACUUM) {
            return GasPressure.VACUUM_PA;
        }

        return VALUES[tier.ordinal() - 1].maximumPressurePa + 1;
    }

    @SuppressWarnings("unused")
    public static long maximumPressureFor(GasPressureTier tier) {
        return tier.maximumPressurePa;
    }

    @SuppressWarnings("unused")
    public static long pressureNeededForNextTier(long pressurePa) {
        Optional<GasPressureTier> nextTier = nearestUnsatisfied(pressurePa);
        if (nextTier.isEmpty()) {
            return 0;
        }

        long normalizedPressurePa = normalizePressurePa(pressurePa);
        return Math.max(0, minimumPressureFor(nextTier.get()) - normalizedPressurePa);
    }

    public static long normalizePressurePa(long pressurePa) {
        return Mth.clamp(pressurePa, GasPressure.VACUUM_PA, MAX_DEFINED_PRESSURE_PA);
    }

    public long minimumPressurePa() {
        return minimumPressureFor(this);
    }

    @SuppressWarnings("unused")
    public int maximumPressureAtm() {
        return maximumPressureAtm;
    }

    public long maximumPressurePa() {
        return maximumPressurePa;
    }

    public boolean contains(long pressurePa) {
        return resolve(pressurePa) == this;
    }

    @SuppressWarnings("unused")
    public boolean isSatisfiedBy(long pressurePa) {
        return satisfies(pressurePa, this);
    }
}
