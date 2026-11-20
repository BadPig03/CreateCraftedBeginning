package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class MeterReadingWindow {
    private static final int TICKS = 10;

    private final long[] samples = new long[TICKS];
    private int size;
    private int next;
    private long quotientSum;
    private int remainderSum;

    static MeterReadingWindow startingAtZero() {
        MeterReadingWindow window = new MeterReadingWindow();
        window.size = TICKS;
        return window;
    }

    long sample(long value) {
        long previous = size == TICKS ? samples[next] : 0;
        size = Math.min(TICKS, size + 1);
        samples[next] = value;
        next = (next + 1) % TICKS;
        quotientSum += value / TICKS - previous / TICKS;
        remainderSum += (int) (value % TICKS - previous % TICKS);
        if (size == TICKS) {
            return quotientSum + (remainderSum + TICKS / 2) / TICKS;
        }

        long remainder = quotientSum % size * TICKS + remainderSum;
        return quotientSum / size * TICKS + (remainder + size / 2) / size;
    }

    void clear() {
        size = 0;
        next = 0;
        quotientSum = 0;
        remainderSum = 0;
    }
}
