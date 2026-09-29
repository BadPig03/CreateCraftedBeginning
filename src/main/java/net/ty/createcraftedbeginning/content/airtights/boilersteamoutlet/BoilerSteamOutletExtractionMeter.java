package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.foundation.NbtValues;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BoilerSteamOutletExtractionMeter {
    private static final int SAMPLE_RATE = 5;
    private static final int SAMPLE_COUNT = 10;
    private static final int SAMPLE_WINDOW_TICKS = SAMPLE_RATE * SAMPLE_COUNT;
    private static final int TICKS_PER_SECOND = 20;

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
        double previousAverageExtractionRate = averageExtractionRate;
        recordSample();
        if (previousAverageExtractionRate != averageExtractionRate) {
            return TickResult.AVERAGE_CHANGED;
        }

        return TickResult.RECORDED;
    }

    double getAverageExtractionRatePerSecond() {
        return averageExtractionRate * TICKS_PER_SECOND;
    }

    boolean recordExtraction(GasStack drainedSteam, GasAction action) {
        if (action.simulate() || drainedSteam.isEmpty()) {
            return false;
        }

        gatheredExtraction = BoundedMath.saturatedAdd(gatheredExtraction, drainedSteam.getAmount());
        return true;
    }

    boolean restoreExtraction(GasStack restoredSteam, GasAction action) {
        if (action.simulate() || restoredSteam.isEmpty()) {
            return false;
        }

        long previousExtraction = gatheredExtraction;
        gatheredExtraction = Math.max(0, gatheredExtraction - restoredSteam.getAmount());
        return gatheredExtraction != previousExtraction;
    }

    void write(CompoundTag compoundTag, boolean clientPacket) {
        compoundTag.putDouble(COMPOUND_KEY_AVERAGE_EXTRACTION_RATE, averageExtractionRate);
        if (clientPacket) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_CURRENT_INDEX, currentIndex);
        compoundTag.putInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE, ticksUntilNextSample);
        compoundTag.putLong(COMPOUND_KEY_GATHERED_EXTRACTION, gatheredExtraction);
        compoundTag.putLongArray(COMPOUND_KEY_SAMPLES, extractedPerSample);
    }

    void read(CompoundTag compoundTag, boolean clientPacket) {
        averageExtractionRate = Math.max(0, compoundTag.getDouble(COMPOUND_KEY_AVERAGE_EXTRACTION_RATE));
        if (clientPacket) {
            return;
        }

        clearSamplingState();
        currentIndex = Mth.positiveModulo(compoundTag.getInt(COMPOUND_KEY_CURRENT_INDEX), SAMPLE_COUNT);
        ticksUntilNextSample = Mth.clamp(NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE, SAMPLE_RATE), 1, SAMPLE_RATE);
        gatheredExtraction = Math.max(0, compoundTag.getLong(COMPOUND_KEY_GATHERED_EXTRACTION));
        long[] storedSamples = compoundTag.getLongArray(COMPOUND_KEY_SAMPLES);
        for (int sampleIndex = 0; sampleIndex < Math.min(storedSamples.length, SAMPLE_COUNT); sampleIndex++) {
            extractedPerSample[sampleIndex] = Math.max(0, storedSamples[sampleIndex]);
        }
        recalculateRollingExtraction();
    }

    private boolean hasSampleState() {
        return rollingExtraction != 0 || gatheredExtraction != 0;
    }

    private void recordSample() {
        rollingExtraction = Math.max(0, rollingExtraction - extractedPerSample[currentIndex]);
        extractedPerSample[currentIndex] = gatheredExtraction;
        rollingExtraction = BoundedMath.saturatedAdd(rollingExtraction, gatheredExtraction);
        currentIndex = (currentIndex + 1) % SAMPLE_COUNT;
        gatheredExtraction = 0;
        averageExtractionRate = (double) rollingExtraction / SAMPLE_WINDOW_TICKS;
    }

    private void recalculateRollingExtraction() {
        rollingExtraction = 0;
        for (long sample : extractedPerSample) {
            rollingExtraction = BoundedMath.saturatedAdd(rollingExtraction, sample);
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
