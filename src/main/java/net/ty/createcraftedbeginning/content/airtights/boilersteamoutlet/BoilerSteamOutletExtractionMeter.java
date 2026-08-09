package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BoilerSteamOutletExtractionMeter {
    private static final int SAMPLE_RATE = 5;
    private static final int SAMPLE_COUNT = 10;
    private static final int SAMPLE_WINDOW_TICKS = SAMPLE_RATE * SAMPLE_COUNT;

    private static final String COMPOUND_KEY_CURRENT_INDEX = "CurrentIndex";
    private static final String COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE = "TicksUntilNextSample";
    private static final String COMPOUND_KEY_GATHERED_EXTRACTION = "GatheredExtraction";
    private static final String COMPOUND_KEY_SAMPLES = "ExtractionSamples";
    private static final String COMPOUND_KEY_AVERAGE_EXTRACTION_RATE = "AverageExtractionRate";

    private final long[] extractedPerSample = new long[SAMPLE_COUNT];
    private int currentIndex;
    private int ticksUntilNextSample = SAMPLE_RATE;
    private long gatheredExtraction;
    private long rollingExtraction;
    private double averageExtractionRate;

    static long saturatedAdd(long current, long amount) {
        if (amount <= 0) {
            return current;
        }
        return Long.MAX_VALUE - current < amount ? Long.MAX_VALUE : current + amount;
    }

    TickResult tick() {
        if (!hasSampleState()) {
            ticksUntilNextSample = SAMPLE_RATE;
            return TickResult.NONE;
        }

        ticksUntilNextSample--;
        if (ticksUntilNextSample > 0) {
            return TickResult.NONE;
        }

        ticksUntilNextSample = SAMPLE_RATE;
        double previousAverage = averageExtractionRate;
        recordSample();
        return Double.compare(previousAverage, averageExtractionRate) == 0 ? TickResult.RECORDED : TickResult.AVERAGE_CHANGED;
    }

    boolean recordExtraction(GasStack drained, GasAction action) {
        if (action.simulate() || drained.isEmpty()) {
            return false;
        }

        gatheredExtraction = saturatedAdd(gatheredExtraction, drained.getAmount());
        return true;
    }

    void write(CompoundTag tag, boolean clientPacket) {
        tag.putDouble(COMPOUND_KEY_AVERAGE_EXTRACTION_RATE, averageExtractionRate);
        if (clientPacket) {
            return;
        }

        tag.putInt(COMPOUND_KEY_CURRENT_INDEX, currentIndex);
        tag.putInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE, ticksUntilNextSample);
        tag.putLong(COMPOUND_KEY_GATHERED_EXTRACTION, gatheredExtraction);
        tag.putLongArray(COMPOUND_KEY_SAMPLES, extractedPerSample);
    }

    void read(CompoundTag tag, boolean clientPacket) {
        averageExtractionRate = Math.max(0, tag.getDouble(COMPOUND_KEY_AVERAGE_EXTRACTION_RATE));
        if (clientPacket) {
            return;
        }

        clearSamplingState();
        currentIndex = Math.floorMod(tag.getInt(COMPOUND_KEY_CURRENT_INDEX), SAMPLE_COUNT);
        ticksUntilNextSample = tag.contains(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE) ? Math.clamp(tag.getInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE), 1, SAMPLE_RATE) : SAMPLE_RATE;
        gatheredExtraction = Math.max(0, tag.getLong(COMPOUND_KEY_GATHERED_EXTRACTION));
        long[] storedSamples = tag.getLongArray(COMPOUND_KEY_SAMPLES);
        for (int i = 0; i < Math.min(storedSamples.length, SAMPLE_COUNT); i++) {
            extractedPerSample[i] = Math.max(0, storedSamples[i]);
        }
        recalculateRollingExtraction();
    }

    private boolean hasSampleState() {
        return rollingExtraction != 0 || gatheredExtraction != 0;
    }

    private void recordSample() {
        rollingExtraction = Math.max(0, rollingExtraction - extractedPerSample[currentIndex]);
        extractedPerSample[currentIndex] = gatheredExtraction;
        rollingExtraction = saturatedAdd(rollingExtraction, gatheredExtraction);
        currentIndex = (currentIndex + 1) % SAMPLE_COUNT;
        gatheredExtraction = 0;
        averageExtractionRate = (double) rollingExtraction / SAMPLE_WINDOW_TICKS;
    }

    private void recalculateRollingExtraction() {
        rollingExtraction = 0;
        for (long sample : extractedPerSample) {
            rollingExtraction = saturatedAdd(rollingExtraction, sample);
        }
        averageExtractionRate = (double) rollingExtraction / SAMPLE_WINDOW_TICKS;
    }

    private void clearSamplingState() {
        currentIndex = 0;
        ticksUntilNextSample = SAMPLE_RATE;
        gatheredExtraction = 0;
        rollingExtraction = 0;
        averageExtractionRate = 0;
        Arrays.fill(extractedPerSample, 0);
    }

    enum TickResult {
        NONE,
        RECORDED,
        AVERAGE_CHANGED
    }
}
