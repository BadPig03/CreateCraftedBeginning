package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.foundation.NbtValues;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class TeslaTurbineFlowMeter {
    private static final float MIN_GAS_SUPPLY_THRESHOLD = 0.01F;
    private static final int FLOW_SAMPLE_COUNT = 10;
    private static final int FLOW_SAMPLE_RATE = 5;
    private static final int FLOW_PER_SUPPLY_LEVEL = 64;
    private static final String COMPOUND_KEY_GAS = "Gas";
    private static final String COMPOUND_KEY_NET_FLOW = "NetFlow";
    private static final String COMPOUND_KEY_ABSOLUTE_FLOW = "AbsoluteFlow";
    private static final String COMPOUND_KEY_CURRENT_INDEX = "CurrentIndex";
    private static final String COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE = "TicksUntilNextSample";
    private static final String COMPOUND_KEY_GATHERED_CLOCKWISE = "GatheredClockwise";
    private static final String COMPOUND_KEY_GATHERED_COUNTER_CLOCKWISE = "GatheredCounterClockwise";
    private static final String COMPOUND_KEY_GATHERED_WEIGHTED_LEVELS = "GatheredWeightedLevels";
    private static final String COMPOUND_KEY_HAS_MIXED_GASES = "HasMixedGases";
    private static final String COMPOUND_KEY_NET_SAMPLES = "NetSamples";
    private static final String COMPOUND_KEY_ABSOLUTE_SAMPLES = "AbsoluteSamples";
    private static final String COMPOUND_KEY_TYPE_LEVEL_SAMPLES = "TypeLevelSamples";

    private final TeslaTurbineCore core;
    private final TeslaTurbineBlockEntity turbine;
    private final float[] netFlowOverTime = new float[FLOW_SAMPLE_COUNT];
    private final float[] absoluteFlowOverTime = new float[FLOW_SAMPLE_COUNT];
    private final float[] typeLevelOverTime = new float[FLOW_SAMPLE_COUNT];
    private final GatheredFlow gatheredFlow = new GatheredFlow();
    private boolean hasMixedGases;
    private float absoluteFlow;
    private float netFlow;
    private GasStack gasType = GasStack.EMPTY;
    private int currentSampleIndex;
    private int ticksUntilNextSample = FLOW_SAMPLE_RATE;

    TeslaTurbineFlowMeter(TeslaTurbineCore core, TeslaTurbineBlockEntity turbine) {
        this.core = core;
        this.turbine = turbine;
    }

    private static GasStack readNormalizedGas(CompoundTag compoundTag, Provider provider) {
        if (!compoundTag.contains(COMPOUND_KEY_GAS)) {
            return GasStack.EMPTY;
        }

        GasStack parsedGas = GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_GAS));
        if (parsedGas.isEmpty()) {
            return GasStack.EMPTY;
        }

        return parsedGas.copyWithAmount(1);
    }

    private static float readFiniteFloat(CompoundTag compoundTag, String key) {
        float storedValue = NbtValues.getFloatOrDefault(compoundTag, key, 0);
        if (!GasConsumptionMath.isFinite(storedValue)) {
            return 0;
        }

        return storedValue;
    }

    private static void readSamples(CompoundTag compoundTag, String key, float[] samples, boolean clampNonNegative) {
        Arrays.fill(samples, 0);
        if (!compoundTag.contains(key, Tag.TAG_LIST)) {
            return;
        }

        ListTag samplesTag = compoundTag.getList(key, Tag.TAG_FLOAT);
        for (int sampleIndex = 0; sampleIndex < Math.min(FLOW_SAMPLE_COUNT, samplesTag.size()); sampleIndex++) {
            float sample = samplesTag.getFloat(sampleIndex);
            if (!GasConsumptionMath.isFinite(sample)) {
                continue;
            }

            samples[sampleIndex] = clampNonNegative ? Math.max(0, sample) : sample;
        }
    }

    private static void sanitizeSamplePairs(float[] netSamples, float[] absoluteSamples) {
        for (int sampleIndex = 0; sampleIndex < FLOW_SAMPLE_COUNT; sampleIndex++) {
            float absoluteSample = absoluteSamples[sampleIndex];
            netSamples[sampleIndex] = BoundedMath.clampMagnitude(netSamples[sampleIndex], absoluteSample);
        }
    }

    private static ListTag createSampleTag(float[] samples) {
        ListTag samplesTag = new ListTag();
        for (float sample : samples) {
            samplesTag.add(FloatTag.valueOf(sample));
        }
        return samplesTag;
    }

    private static float finiteFloat(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }

        if (value > Float.MAX_VALUE) {
            return Float.MAX_VALUE;
        }

        if (value < -Float.MAX_VALUE) {
            return -Float.MAX_VALUE;
        }

        return (float) value;
    }

    long fill(GasStack resource, float typeLevel, GasAction action, boolean isClockwise) {
        if (resource.isEmpty()) {
            return 0;
        }

        if (hasMixedGases) {
            return resource.getAmount();
        }

        long requestedAmount = resource.getAmount();
        GasStack normalizedGas = resource.copyWithAmount(1);
        boolean mixesWithStoredGas = !gasType.isEmpty() && !GasStack.isSameGasSameComponents(gasType, normalizedGas);
        if (mixesWithStoredGas) {
            if (action.execute()) {
                hasMixedGases = true;
                core.markForSave();
            }
            return requestedAmount;
        }

        long gatheredAmount = gatheredFlow.amount(isClockwise);
        long acceptedAmount = Math.min(requestedAmount, Long.MAX_VALUE - gatheredAmount);
        if (acceptedAmount <= 0 || !action.execute()) {
            return acceptedAmount;
        }

        if (gasType.isEmpty()) {
            setFuelMode(normalizedGas);
        }

        gatheredFlow.add(acceptedAmount, typeLevel, isClockwise);
        core.markForSave();
        return acceptedAmount;
    }

    void tick() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        if (hasMixedGases) {
            if (CCBConfig.server().machines.teslaTurbine.explodesOnIncompatibleGases.get()) {
                core.getStructureManager().triggerExplosion();
            }
            reset();
            return;
        }

        ticksUntilNextSample--;
        if (ticksUntilNextSample > 0) {
            return;
        }

        ticksUntilNextSample = FLOW_SAMPLE_RATE;
        float previousNetFlow = netFlow;
        float previousAbsoluteFlow = absoluteFlow;
        boolean hadPersistentSampleState = hasPersistentSampleState();
        recordSample();
        updateDerivedFlow(true);
        if (hadPersistentSampleState) {
            core.markForSave();
        }
        if (previousNetFlow == netFlow && previousAbsoluteFlow == absoluteFlow) {
            return;
        }

        core.markForClientSync();
    }

    boolean isClockwiseFlow() {
        return netFlow > 0;
    }

    CompoundTag write(Provider provider, boolean clientPacket) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_GAS, gasType.saveOptional(provider));
        if (clientPacket) {
            compoundTag.putFloat(COMPOUND_KEY_NET_FLOW, netFlow);
            compoundTag.putFloat(COMPOUND_KEY_ABSOLUTE_FLOW, absoluteFlow);
            return compoundTag;
        }

        compoundTag.putBoolean(COMPOUND_KEY_HAS_MIXED_GASES, hasMixedGases);
        compoundTag.putInt(COMPOUND_KEY_CURRENT_INDEX, currentSampleIndex);
        compoundTag.putInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE, ticksUntilNextSample);
        compoundTag.putLong(COMPOUND_KEY_GATHERED_CLOCKWISE, gatheredFlow.clockwise);
        compoundTag.putLong(COMPOUND_KEY_GATHERED_COUNTER_CLOCKWISE, gatheredFlow.counterClockwise);
        compoundTag.putDouble(COMPOUND_KEY_GATHERED_WEIGHTED_LEVELS, gatheredFlow.weightedLevels);
        compoundTag.put(COMPOUND_KEY_NET_SAMPLES, createSampleTag(netFlowOverTime));
        compoundTag.put(COMPOUND_KEY_ABSOLUTE_SAMPLES, createSampleTag(absoluteFlowOverTime));
        compoundTag.put(COMPOUND_KEY_TYPE_LEVEL_SAMPLES, createSampleTag(typeLevelOverTime));
        return compoundTag;
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            readClient(compoundTag, provider);
            return;
        }

        readPersistent(compoundTag, provider);
    }

    void loadEmptyState() {
        clearRuntimeState();
        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        levelCalculator.loadSupplyLevel(0);
        levelCalculator.loadTypeLevel(0);
    }

    GasStack getGasType() {
        return gasType;
    }

    private void setGasType() {
        setFuelMode(GasStack.EMPTY);
    }

    private void setFuelMode(GasStack newGasType) {
        GasStack normalizedGas = newGasType.isEmpty() ? GasStack.EMPTY : newGasType.copyWithAmount(1);
        if (GasStack.isSameGasSameComponents(gasType, normalizedGas)) {
            return;
        }

        gasType = normalizedGas;
        clearFlowSamples();
        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        levelCalculator.loadSupplyLevel(0);
        levelCalculator.loadTypeLevel(0);
        core.markForSaveAndClientSync();
    }

    private void reset() {
        boolean hadRuntimeState = hasRuntimeState();
        clearRuntimeState();

        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        levelCalculator.loadSupplyLevel(0);
        levelCalculator.loadTypeLevel(0);
        if (!hadRuntimeState) {
            return;
        }

        core.markForSaveAndClientSync();
    }

    private void readClient(CompoundTag compoundTag, Provider provider) {
        gasType = readNormalizedGas(compoundTag, provider);
        absoluteFlow = Math.max(0, readFiniteFloat(compoundTag, COMPOUND_KEY_ABSOLUTE_FLOW));
        netFlow = BoundedMath.clampMagnitude(readFiniteFloat(compoundTag, COMPOUND_KEY_NET_FLOW), absoluteFlow);
        if (!gasType.isEmpty()) {
            return;
        }

        netFlow = 0;
        absoluteFlow = 0;
    }

    private void readPersistent(CompoundTag compoundTag, Provider provider) {
        clearRuntimeState();
        gasType = readNormalizedGas(compoundTag, provider);
        hasMixedGases = !gasType.isEmpty() && compoundTag.getBoolean(COMPOUND_KEY_HAS_MIXED_GASES);
        currentSampleIndex = Mth.positiveModulo(compoundTag.getInt(COMPOUND_KEY_CURRENT_INDEX), FLOW_SAMPLE_COUNT);
        ticksUntilNextSample = Mth.clamp(NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE, FLOW_SAMPLE_RATE), 1, FLOW_SAMPLE_RATE);
        readGatheredFlow(compoundTag);
        readSamples(compoundTag, COMPOUND_KEY_NET_SAMPLES, netFlowOverTime, false);
        readSamples(compoundTag, COMPOUND_KEY_ABSOLUTE_SAMPLES, absoluteFlowOverTime, true);
        sanitizeSamplePairs(netFlowOverTime, absoluteFlowOverTime);
        readSamples(compoundTag, COMPOUND_KEY_TYPE_LEVEL_SAMPLES, typeLevelOverTime, true);
        for (int index = 0; index < FLOW_SAMPLE_COUNT; index++) {
            typeLevelOverTime[index] = Mth.clamp(typeLevelOverTime[index], 0, TeslaTurbineBlock.MAX_LEVEL);
        }

        if (gasType.isEmpty()) {
            gatheredFlow.clear();
            Arrays.fill(netFlowOverTime, 0);
            Arrays.fill(absoluteFlowOverTime, 0);
            Arrays.fill(typeLevelOverTime, 0);
        }
        core.getLevelCalculator().loadTypeLevel(0);
        updateDerivedFlow(false);
    }

    private void updateDerivedFlow(boolean shouldNotify) {
        double totalNetFlow = 0;
        double totalAbsoluteFlow = 0;
        double weightedTypeLevels = 0;
        for (int sampleIndex = 0; sampleIndex < FLOW_SAMPLE_COUNT; sampleIndex++) {
            totalNetFlow += netFlowOverTime[sampleIndex];
            totalAbsoluteFlow += absoluteFlowOverTime[sampleIndex];
            weightedTypeLevels += (double) typeLevelOverTime[sampleIndex] * absoluteFlowOverTime[sampleIndex];
        }

        netFlow = finiteFloat(totalNetFlow / FLOW_SAMPLE_COUNT);
        absoluteFlow = Math.max(0, finiteFloat(totalAbsoluteFlow / FLOW_SAMPLE_COUNT));
        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        boolean gasSupplyEnded = absoluteFlow < MIN_GAS_SUPPLY_THRESHOLD && gatheredFlow.isEmpty() && !gasType.isEmpty();
        if (gasSupplyEnded) {
            if (shouldNotify) {
                setGasType();
            }
            else {
                gasType = GasStack.EMPTY;
                levelCalculator.loadTypeLevel(0);
                levelCalculator.loadSupplyLevel(0);
            }
            return;
        }

        float typeLevel = gasType.isEmpty() || totalAbsoluteFlow <= 0 ? 0 : (float) (weightedTypeLevels / totalAbsoluteFlow);
        float supplyLevel = gasType.isEmpty() ? 0 : Math.min(TeslaTurbineBlock.MAX_LEVEL, Mth.abs(netFlow) / FLOW_PER_SUPPLY_LEVEL);
        if (!shouldNotify) {
            levelCalculator.loadTypeLevel(typeLevel);
            levelCalculator.loadSupplyLevel(supplyLevel);
            return;
        }

        levelCalculator.updateTypeLevel(typeLevel);
        levelCalculator.updateSupplyLevel(supplyLevel);
    }

    private boolean hasPersistentSampleState() {
        return !gasType.isEmpty() || !gatheredFlow.isEmpty() || netFlowOverTime[currentSampleIndex] != 0 || absoluteFlowOverTime[currentSampleIndex] != 0;
    }

    private boolean hasRuntimeState() {
        return netFlow != 0 || absoluteFlow != 0 || !gatheredFlow.isEmpty() || currentSampleIndex != 0 || ticksUntilNextSample != FLOW_SAMPLE_RATE || hasMixedGases || !gasType.isEmpty();
    }

    private void recordSample() {
        double clockwiseRate = (double) gatheredFlow.clockwise / FLOW_SAMPLE_RATE;
        double counterClockwiseRate = (double) gatheredFlow.counterClockwise / FLOW_SAMPLE_RATE;
        double absoluteAmount = (double) gatheredFlow.clockwise + gatheredFlow.counterClockwise;
        typeLevelOverTime[currentSampleIndex] = absoluteAmount <= 0 ? 0 : (float) (gatheredFlow.weightedLevels / absoluteAmount);
        netFlowOverTime[currentSampleIndex] = finiteFloat(clockwiseRate - counterClockwiseRate);
        absoluteFlowOverTime[currentSampleIndex] = Math.max(0, finiteFloat(clockwiseRate + counterClockwiseRate));
        currentSampleIndex = (currentSampleIndex + 1) % FLOW_SAMPLE_COUNT;
        gatheredFlow.clear();
    }

    private void readGatheredFlow(CompoundTag compoundTag) {
        gatheredFlow.clockwise = Math.max(0, compoundTag.getLong(COMPOUND_KEY_GATHERED_CLOCKWISE));
        gatheredFlow.counterClockwise = Math.max(0, compoundTag.getLong(COMPOUND_KEY_GATHERED_COUNTER_CLOCKWISE));
        double weightedLevels = compoundTag.getDouble(COMPOUND_KEY_GATHERED_WEIGHTED_LEVELS);
        if (!Double.isFinite(weightedLevels)) {
            gatheredFlow.clear();
            return;
        }

        double maximumWeightedLevels = ((double) gatheredFlow.clockwise + gatheredFlow.counterClockwise) * TeslaTurbineBlock.MAX_LEVEL;
        gatheredFlow.weightedLevels = Mth.clamp(weightedLevels, 0, maximumWeightedLevels);
    }

    private void clearRuntimeState() {
        clearFlowSamples();
        hasMixedGases = false;
        gasType = GasStack.EMPTY;
    }

    private void clearFlowSamples() {
        netFlow = 0;
        absoluteFlow = 0;
        gatheredFlow.clear();
        ticksUntilNextSample = FLOW_SAMPLE_RATE;
        currentSampleIndex = 0;
        Arrays.fill(netFlowOverTime, 0);
        Arrays.fill(absoluteFlowOverTime, 0);
        Arrays.fill(typeLevelOverTime, 0);
    }

    private static final class GatheredFlow {
        private long clockwise;
        private long counterClockwise;
        private double weightedLevels;

        private long amount(boolean isClockwise) {
            if (isClockwise) {
                return clockwise;
            }

            return counterClockwise;
        }

        private void add(long amount, float typeLevel, boolean isClockwise) {
            weightedLevels += (double) amount * typeLevel;
            if (isClockwise) {
                clockwise += amount;
                return;
            }

            counterClockwise += amount;
        }

        private boolean isEmpty() {
            return clockwise == 0 && counterClockwise == 0;
        }

        private void clear() {
            clockwise = 0;
            counterClockwise = 0;
            weightedLevels = 0;
        }
    }
}
