package net.ty.createcraftedbeginning.api.gas.gases;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasAmounts {
    public static final long MILLIBUCKETS_PER_BUCKET = FluidType.BUCKET_VOLUME;
    public static final long MILLIBUCKETS_PER_KILOBUCKET = MILLIBUCKETS_PER_BUCKET * 1000;
    public static final long MILLIBUCKETS_PER_MEGABUCKET = MILLIBUCKETS_PER_KILOBUCKET * 1000;

    private GasAmounts() {
    }

    public static LangBuilder precise(long mb) {
        return new LangBuilder(CCBAPI.MOD_ID).text(LangNumberFormat.format(mb)).add(new LangBuilder(CCBAPI.MOD_ID).translate("gui.unit.milli_buckets"));
    }

    public static String formatPrecise(long mb) {
        return precise(mb).component().getString();
    }

    public static String formatCompact(long mb) {
        if (mb < MILLIBUCKETS_PER_BUCKET) {
            return mb + "mB";
        }

        if (mb < MILLIBUCKETS_PER_KILOBUCKET) {
            return formatTenths(mb, MILLIBUCKETS_PER_BUCKET) + 'B';
        }
        return formatTenths(mb, MILLIBUCKETS_PER_KILOBUCKET) + "kB";
    }

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

    public static int toWholeBucketsClamped(long mb) {
        return CCBMathUtils.clampToNonNegativeInt(mb / MILLIBUCKETS_PER_BUCKET);
    }

    public static int toMillibucketsClamped(long mb) {
        return CCBMathUtils.clampToNonNegativeInt(mb);
    }

    public static MutableComponent formatWholeBuckets(long b) {
        return new LangBuilder(CCBAPI.MOD_ID).text(LangNumberFormat.format(b)).space().add(new LangBuilder(CCBAPI.MOD_ID).translate("gui.threshold.buckets")).component();
    }

    private static String formatTenths(long amount, long unit) {
        long tenths = amount / (unit / 10);
        long whole = tenths / 10;
        long fraction = tenths % 10;
        if (fraction == 0) {
            return Long.toString(whole);
        }
        return whole + "." + fraction;
    }
}
