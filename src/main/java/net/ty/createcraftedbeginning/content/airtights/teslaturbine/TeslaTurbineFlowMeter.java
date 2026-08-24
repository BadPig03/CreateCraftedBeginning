package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class TeslaTurbineFlowMeter {
    private static final String COMPOUND_KEY_GAS = "Gas";
    private static final String COMPOUND_KEY_NET_FLOW = "NetFlow";
    private static final String COMPOUND_KEY_ABSOLUTE_FLOW = "AbsoluteFlow";
    private static final String COMPOUND_KEY_CURRENT_INDEX = "CurrentIndex";
    private static final String COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE = "TicksUntilNextSample";
    private static final String COMPOUND_KEY_GATHERED_CLOCKWISE = "GatheredClockwise";
    private static final String COMPOUND_KEY_GATHERED_COUNTER_CLOCKWISE = "GatheredCounterClockwise";
    private static final String COMPOUND_KEY_HAS_MIXED_GASES = "HasMixedGases";
    private static final String COMPOUND_KEY_NET_SAMPLES = "NetSamples";
    private static final String COMPOUND_KEY_ABSOLUTE_SAMPLES = "AbsoluteSamples";

    private final TeslaTurbineCore core;
    private final TeslaTurbineBlockEntity turbine;
    private final float[] netFlowOverTime = new float[TeslaTurbineUtils.FLOW_SAMPLE_COUNT];
    private final float[] absoluteFlowOverTime = new float[TeslaTurbineUtils.FLOW_SAMPLE_COUNT];

    private boolean hasMixedGases;
    private float absoluteFlow;
    private float netFlow;
    private GasStack gasType = GasStack.EMPTY;
    private int currentSampleIndex;
    private int ticksUntilNextSample = TeslaTurbineUtils.FLOW_SAMPLE_RATE;
    private long gatheredClockwise;
    private long gatheredCounterClockwise;

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
        if (!compoundTag.contains(key)) {
            return 0;
        }

        float storedValue = compoundTag.getFloat(key);
        return GasConsumptions.isFinite(storedValue) ? storedValue : 0;
    }

    private static void readSamples(CompoundTag compoundTag, String key, float[] samples, boolean clampNonNegative) {
        Arrays.fill(samples, 0);
        if (!compoundTag.contains(key, Tag.TAG_LIST)) {
            return;
        }

        ListTag samplesTag = compoundTag.getList(key, Tag.TAG_FLOAT);
        for (int sampleIndex = 0; sampleIndex < Math.min(TeslaTurbineUtils.FLOW_SAMPLE_COUNT, samplesTag.size()); sampleIndex++) {
            float sample = samplesTag.getFloat(sampleIndex);
            if (!GasConsumptions.isFinite(sample)) {
                continue;
            }

            samples[sampleIndex] = clampNonNegative ? Math.max(0, sample) : sample;
        }
    }

    private static void sanitizeSamplePairs(float[] netSamples, float[] absoluteSamples) {
        for (int sampleIndex = 0; sampleIndex < TeslaTurbineUtils.FLOW_SAMPLE_COUNT; sampleIndex++) {
            float absoluteSample = absoluteSamples[sampleIndex];
            netSamples[sampleIndex] = Mth.clamp(netSamples[sampleIndex], -absoluteSample, absoluteSample);
        }
    }

    private static ListTag createSampleTag(float[] samples) {
        ListTag samplesTag = new ListTag();
        for (float sample : samples) {
            samplesTag.add(FloatTag.valueOf(sample));
        }
        return samplesTag;
    }

    private static int readSampleDelay(CompoundTag compoundTag) {
        if (!compoundTag.contains(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE)) {
            return TeslaTurbineUtils.FLOW_SAMPLE_RATE;
        }
        return Mth.clamp(compoundTag.getInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE), 1, TeslaTurbineUtils.FLOW_SAMPLE_RATE);
    }

    long fill(GasStack resource, GasAction action, boolean isClockwise) {
        if (resource.isEmpty()) {
            return 0;
        }

        if (hasMixedGases) {
            return 0;
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

        long gatheredAmount = isClockwise ? gatheredClockwise : gatheredCounterClockwise;
        long acceptedAmount = Math.min(requestedAmount, Long.MAX_VALUE - gatheredAmount);
        if (acceptedAmount <= 0 || !action.execute()) {
            return acceptedAmount;
        }

        if (gasType.isEmpty()) {
            setGasType(normalizedGas);
        }

        if (isClockwise) {
            gatheredClockwise += acceptedAmount;
        }
        else {
            gatheredCounterClockwise += acceptedAmount;
        }
        core.markForSave();
        return acceptedAmount;
    }

    void tick() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        if (hasMixedGases) {
            if (CCBConfig.server().airtights.teslaTurbineExplodesOnMixedGases.get()) {
                core.getStructureManager().triggerExplosion();
            }
            reset();
            return;
        }

        ticksUntilNextSample--;
        if (ticksUntilNextSample > 0) {
            return;
        }

        ticksUntilNextSample = TeslaTurbineUtils.FLOW_SAMPLE_RATE;
        float previousNetFlow = netFlow;
        float previousAbsoluteFlow = absoluteFlow;
        boolean hadPersistentSampleState = hasPersistentSampleState();
        recordSample();
        updateDerivedFlow(true);
        if (hadPersistentSampleState) {
            core.markForSave();
        }
        if (Float.compare(previousNetFlow, netFlow) == 0 && Float.compare(previousAbsoluteFlow, absoluteFlow) == 0) {
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
        compoundTag.putLong(COMPOUND_KEY_GATHERED_CLOCKWISE, gatheredClockwise);
        compoundTag.putLong(COMPOUND_KEY_GATHERED_COUNTER_CLOCKWISE, gatheredCounterClockwise);

        compoundTag.put(COMPOUND_KEY_NET_SAMPLES, createSampleTag(netFlowOverTime));
        compoundTag.put(COMPOUND_KEY_ABSOLUTE_SAMPLES, createSampleTag(absoluteFlowOverTime));
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
        levelCalculator.loadTypeLevel();
    }

    GasStack getGasType() {
        return gasType;
    }

    private void setGasType(GasStack newGasType) {
        GasStack normalizedGas = newGasType.isEmpty() ? GasStack.EMPTY : newGasType.copyWithAmount(1);
        if (GasStack.isSameGasSameComponents(gasType, normalizedGas)) {
            return;
        }

        gasType = normalizedGas;
        clearFlowSamples();
        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        levelCalculator.loadSupplyLevel(0);
        levelCalculator.loadTypeLevel();
        core.markForSaveAndClientSync();
    }

    private void reset() {
        boolean hadRuntimeState = hasRuntimeState();
        clearRuntimeState();

        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        levelCalculator.loadSupplyLevel(0);
        levelCalculator.loadTypeLevel();
        if (!hadRuntimeState) {
            return;
        }

        core.markForSaveAndClientSync();
    }

    private void readClient(CompoundTag compoundTag, Provider provider) {
        gasType = readNormalizedGas(compoundTag, provider);
        absoluteFlow = Math.max(0, readFiniteFloat(compoundTag, COMPOUND_KEY_ABSOLUTE_FLOW));
        netFlow = Mth.clamp(readFiniteFloat(compoundTag, COMPOUND_KEY_NET_FLOW), -absoluteFlow, absoluteFlow);
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
        currentSampleIndex = Math.floorMod(compoundTag.getInt(COMPOUND_KEY_CURRENT_INDEX), TeslaTurbineUtils.FLOW_SAMPLE_COUNT);
        ticksUntilNextSample = readSampleDelay(compoundTag);
        gatheredClockwise = Math.max(0, compoundTag.getLong(COMPOUND_KEY_GATHERED_CLOCKWISE));
        gatheredCounterClockwise = Math.max(0, compoundTag.getLong(COMPOUND_KEY_GATHERED_COUNTER_CLOCKWISE));
        readSamples(compoundTag, COMPOUND_KEY_NET_SAMPLES, netFlowOverTime, false);
        readSamples(compoundTag, COMPOUND_KEY_ABSOLUTE_SAMPLES, absoluteFlowOverTime, true);
        sanitizeSamplePairs(netFlowOverTime, absoluteFlowOverTime);

        if (gasType.isEmpty()) {
            gatheredClockwise = 0;
            gatheredCounterClockwise = 0;
            Arrays.fill(netFlowOverTime, 0);
            Arrays.fill(absoluteFlowOverTime, 0);
        }

        core.getLevelCalculator().loadTypeLevel();
        updateDerivedFlow(false);
    }

    private void updateDerivedFlow(boolean shouldNotify) {
        float totalNetFlow = 0;
        float totalAbsoluteFlow = 0;
        for (int sampleIndex = 0; sampleIndex < TeslaTurbineUtils.FLOW_SAMPLE_COUNT; sampleIndex++) {
            totalNetFlow += netFlowOverTime[sampleIndex];
            totalAbsoluteFlow += absoluteFlowOverTime[sampleIndex];
        }

        netFlow = GasConsumptions.isFinite(totalNetFlow) ? totalNetFlow / TeslaTurbineUtils.FLOW_SAMPLE_COUNT : 0;
        absoluteFlow = GasConsumptions.isFinite(totalAbsoluteFlow) ? Math.max(0, totalAbsoluteFlow / TeslaTurbineUtils.FLOW_SAMPLE_COUNT) : 0;
        boolean gasSupplyEnded = absoluteFlow < TeslaTurbineUtils.MIN_GAS_SUPPLY_THRESHOLD && gatheredClockwise == 0 && gatheredCounterClockwise == 0 && !gasType.isEmpty();
        if (gasSupplyEnded) {
            if (shouldNotify) {
                setGasType(GasStack.EMPTY);
            }
            else {
                gasType = GasStack.EMPTY;
                core.getLevelCalculator().loadTypeLevel();
                core.getLevelCalculator().loadSupplyLevel(0);
            }
            return;
        }

        int supplyLevel = gasType.isEmpty() ? 0 : (int) Math.min(TeslaTurbineUtils.MAX_LEVEL, Math.abs(netFlow) / TeslaTurbineUtils.BASE_ROTATION_SPEED);
        if (!shouldNotify) {
            core.getLevelCalculator().loadSupplyLevel(supplyLevel);
            return;
        }

        core.getLevelCalculator().updateSupplyLevel(supplyLevel);
    }

    private boolean hasPersistentSampleState() {
        return !gasType.isEmpty() || gatheredClockwise != 0 || gatheredCounterClockwise != 0 || netFlowOverTime[currentSampleIndex] != 0 || absoluteFlowOverTime[currentSampleIndex] != 0;
    }

    private boolean hasRuntimeState() {
        return netFlow != 0 || absoluteFlow != 0 || gatheredClockwise != 0 || gatheredCounterClockwise != 0 || currentSampleIndex != 0 || ticksUntilNextSample != TeslaTurbineUtils.FLOW_SAMPLE_RATE || hasMixedGases || !gasType.isEmpty();
    }

    private void recordSample() {
        float clockwiseRate = gatheredClockwise > 0 ? (float) gatheredClockwise / TeslaTurbineUtils.FLOW_SAMPLE_RATE : 0;
        float counterClockwiseRate = gatheredCounterClockwise > 0 ? (float) gatheredCounterClockwise / TeslaTurbineUtils.FLOW_SAMPLE_RATE : 0;
        netFlowOverTime[currentSampleIndex] = clockwiseRate - counterClockwiseRate;
        absoluteFlowOverTime[currentSampleIndex] = clockwiseRate + counterClockwiseRate;
        currentSampleIndex = (currentSampleIndex + 1) % TeslaTurbineUtils.FLOW_SAMPLE_COUNT;
        gatheredClockwise = 0;
        gatheredCounterClockwise = 0;
    }

    private void clearRuntimeState() {
        clearFlowSamples();
        hasMixedGases = false;
        gasType = GasStack.EMPTY;
    }

    private void clearFlowSamples() {
        netFlow = 0;
        absoluteFlow = 0;
        gatheredClockwise = 0;
        gatheredCounterClockwise = 0;
        ticksUntilNextSample = TeslaTurbineUtils.FLOW_SAMPLE_RATE;
        currentSampleIndex = 0;
        Arrays.fill(netFlowOverTime, 0);
        Arrays.fill(absoluteFlowOverTime, 0);
    }
}
