package net.ty.createcraftedbeginning.api.gas.gases;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.data.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasAmountUtils {
    public static final long MILLIBUCKETS_PER_BUCKET = FluidType.BUCKET_VOLUME;
    public static final long MILLIBUCKETS_PER_KILOBUCKET = MILLIBUCKETS_PER_BUCKET * 1000;
    public static final long MILLIBUCKETS_PER_MEGABUCKET = MILLIBUCKETS_PER_KILOBUCKET * 1000;

    private GasAmountUtils() {
    }

    /**
     * Creates a precisely formatted gas-amount component.
     *
     * @param mb the gas amount, in millibuckets
     * @return the resulting lang builder
     */
    public static LangBuilder precise(long mb) {
        return CCBLang.number(mb).add(CCBLang.translate("gui.unit.milli_buckets"));
    }

    /**
     * Formats the supplied amount with precise units.
     *
     * @param mb the gas amount, in millibuckets
     * @return the formatted text
     */
    public static String formatPrecise(long mb) {
        return precise(mb).component().getString();
    }

    /**
     * Formats the supplied amount using compact units.
     *
     * @param mb the gas amount, in millibuckets
     * @return the formatted text
     */
    public static String formatCompact(long mb) {
        if (mb < MILLIBUCKETS_PER_BUCKET) {
            return mb + "mB";
        }

        if (mb < MILLIBUCKETS_PER_KILOBUCKET) {
            return formatTenths(mb, MILLIBUCKETS_PER_BUCKET) + 'B';
        }
        return formatTenths(mb, MILLIBUCKETS_PER_KILOBUCKET) + "kB";
    }

    /**
     * Formats the supplied amount for stock-keeper displays.
     *
     * @param mb the gas amount, in millibuckets
     * @return the formatted text
     */
    public static String formatStockKeeper(long mb) {
        if (mb >= MILLIBUCKETS_PER_MEGABUCKET / 10) {
            return formatTenths(mb, MILLIBUCKETS_PER_MEGABUCKET) + "mb";
        }

        if (mb >= MILLIBUCKETS_PER_KILOBUCKET / 10) {
            return formatTenths(mb, MILLIBUCKETS_PER_KILOBUCKET) + "kb";
        }

        if (mb >= MILLIBUCKETS_PER_BUCKET / 10) {
            return formatTenths(mb, MILLIBUCKETS_PER_BUCKET) + 'b';
        }
        return mb + "mb";
    }

    /**
     * Formats the supplied amount compactly without discarding precision.
     *
     * @param mb the gas amount, in millibuckets
     * @return the formatted text
     */
    public static String formatLosslessCompact(long mb) {
        if (mb >= MILLIBUCKETS_PER_MEGABUCKET && mb % (MILLIBUCKETS_PER_MEGABUCKET / 10) == 0) {
            return formatTenths(mb, MILLIBUCKETS_PER_MEGABUCKET) + "mB";
        }

        if (mb >= MILLIBUCKETS_PER_KILOBUCKET && mb % (MILLIBUCKETS_PER_KILOBUCKET / 10) == 0) {
            return formatTenths(mb, MILLIBUCKETS_PER_KILOBUCKET) + "kB";
        }

        if (mb >= MILLIBUCKETS_PER_BUCKET && mb % (MILLIBUCKETS_PER_BUCKET / 10) == 0) {
            return formatTenths(mb, MILLIBUCKETS_PER_BUCKET) + 'B';
        }
        return formatPrecise(mb);
    }

    /**
     * Converts this value to a whole buckets clamped representation.
     *
     * @param mb the gas amount, in millibuckets
     * @return the converted value
     */
    public static int toWholeBucketsClamped(long mb) {
        return Math.clamp(mb / MILLIBUCKETS_PER_BUCKET, 0, Integer.MAX_VALUE);
    }

    /**
     * Converts this value to a millibuckets clamped representation.
     *
     * @param mb the gas amount, in millibuckets
     * @return the converted value
     */
    public static int toMillibucketsClamped(long mb) {
        return Math.clamp(mb, 0, Integer.MAX_VALUE);
    }

    /**
     * Formats the supplied amount as whole buckets.
     *
     * @param b the gas amount, in buckets
     * @return the formatted text
     */
    public static MutableComponent formatWholeBuckets(long b) {
        return CCBLang.number(b).space().add(CCBLang.translate("gui.threshold.buckets")).component();
    }

    private static String formatTenths(long amount, long unit) {
        long tenths = amount / (unit / 10);
        long whole = tenths / 10;
        long fraction = tenths % 10;
        return fraction == 0 ? Long.toString(whole) : whole + "." + fraction;
    }
}
