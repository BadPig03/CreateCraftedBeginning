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
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineFlowMeter {
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
    private int currentIndex;
    private int ticksUntilNextSample = TeslaTurbineUtils.FLOW_SAMPLE_RATE;
    private long gatheredClockwise;
    private long gatheredCounterClockwise;

    public TeslaTurbineFlowMeter(TeslaTurbineCore core, TeslaTurbineBlockEntity turbine) {
        this.core = core;
        this.turbine = turbine;
    }

    private static long saturatedAdd(long current, long amount) {
        if (amount <= 0) {
            return current;
        }

        if (Long.MAX_VALUE - current < amount) {
            return Long.MAX_VALUE;
        }
        return current + amount;
    }

    private static GasStack readNormalizedGas(CompoundTag compoundTag, Provider provider) {
        if (!compoundTag.contains(COMPOUND_KEY_GAS)) {
            return GasStack.EMPTY;
        }

        GasStack gas = GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_GAS));
        return gas.isEmpty() ? GasStack.EMPTY : gas.copyWithAmount(1);
    }

    private static float readFiniteFloat(CompoundTag compoundTag, String key) {
        if (!compoundTag.contains(key)) {
            return 0;
        }

        float value = compoundTag.getFloat(key);
        return Float.isFinite(value) ? value : 0;
    }

    private static void readSamples(CompoundTag compoundTag, String key, float[] samples, boolean nonNegative) {
        Arrays.fill(samples, 0);
        if (!compoundTag.contains(key, Tag.TAG_LIST)) {
            return;
        }

        ListTag samplesTag = compoundTag.getList(key, Tag.TAG_FLOAT);
        for (int i = 0; i < Math.min(TeslaTurbineUtils.FLOW_SAMPLE_COUNT, samplesTag.size()); i++) {
            float sample = samplesTag.getFloat(i);
            if (!Float.isFinite(sample)) {
                continue;
            }

            samples[i] = nonNegative ? Math.max(0, sample) : sample;
        }
    }

    private static void sanitizeSamplePairs(float[] netSamples, float[] absoluteSamples) {
        for (int i = 0; i < TeslaTurbineUtils.FLOW_SAMPLE_COUNT; i++) {
            float absoluteSample = absoluteSamples[i];
            netSamples[i] = Mth.clamp(netSamples[i], -absoluteSample, absoluteSample);
        }
    }

    private static ListTag createSampleTag(float[] samples) {
        ListTag tag = new ListTag();
        for (float sample : samples) {
            tag.add(FloatTag.valueOf(sample));
        }
        return tag;
    }

    private static int readSampleDelay(CompoundTag tag) {
        if (!tag.contains(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE)) {
            return TeslaTurbineUtils.FLOW_SAMPLE_RATE;
        }
        return Mth.clamp(tag.getInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE), 1, TeslaTurbineUtils.FLOW_SAMPLE_RATE);
    }

    public long fill(GasStack resource, GasAction action, boolean clockwise) {
        if (resource.isEmpty()) {
            return 0;
        }

        long amount = resource.getAmount();
        if (!action.execute()) {
            return amount;
        }

        if (hasMixedGases) {
            return 0;
        }

        GasStack normalizedGas = resource.copyWithAmount(1);
        if (!gasType.isEmpty() && !GasStack.isSameGasSameComponents(gasType, normalizedGas)) {
            hasMixedGases = true;
            core.markForSave();
            return 0;
        }

        if (gasType.isEmpty()) {
            setGasType(normalizedGas);
        }

        if (clockwise) {
            gatheredClockwise = saturatedAdd(gatheredClockwise, amount);
        }
        else {
            gatheredCounterClockwise = saturatedAdd(gatheredCounterClockwise, amount);
        }
        core.markForSave();
        return amount;
    }

    public void tick() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        if (hasMixedGases) {
            if (CCBConfig.server().airtights.teslaTurbineExplodesOnMixedGases.get()) {
                core.getStructureManager().triggerExplosion();
            }
            reset(true);
            return;
        }

        ticksUntilNextSample--;
        if (ticksUntilNextSample > 0) {
            return;
        }

        ticksUntilNextSample = TeslaTurbineUtils.FLOW_SAMPLE_RATE;
        float previousNetFlow = netFlow;
        float previousAbsoluteFlow = absoluteFlow;
        boolean persistentStateChanged = hasPersistentSampleState();
        recordSample();
        updateDerivedFlow(true);
        if (persistentStateChanged) {
            core.markForSave();
        }
        if (Float.compare(previousNetFlow, netFlow) == 0 && Float.compare(previousAbsoluteFlow, absoluteFlow) == 0) {
            return;
        }

        core.markForClientSync();
    }

    public boolean isClockwiseFlow() {
        return netFlow > 0;
    }

    public CompoundTag write(Provider provider, boolean clientPacket) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_GAS, gasType.saveOptional(provider));
        if (clientPacket) {
            compoundTag.putFloat(COMPOUND_KEY_NET_FLOW, netFlow);
            compoundTag.putFloat(COMPOUND_KEY_ABSOLUTE_FLOW, absoluteFlow);
            return compoundTag;
        }

        compoundTag.putBoolean(COMPOUND_KEY_HAS_MIXED_GASES, hasMixedGases);
        compoundTag.putInt(COMPOUND_KEY_CURRENT_INDEX, currentIndex);
        compoundTag.putInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE, ticksUntilNextSample);
        compoundTag.putLong(COMPOUND_KEY_GATHERED_CLOCKWISE, gatheredClockwise);
        compoundTag.putLong(COMPOUND_KEY_GATHERED_COUNTER_CLOCKWISE, gatheredCounterClockwise);

        compoundTag.put(COMPOUND_KEY_NET_SAMPLES, createSampleTag(netFlowOverTime));
        compoundTag.put(COMPOUND_KEY_ABSOLUTE_SAMPLES, createSampleTag(absoluteFlowOverTime));
        return compoundTag;
    }

    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            readClient(compoundTag, provider);
        }
        else {
            readPersistent(compoundTag, provider);
        }
    }

    public void loadEmptyState() {
        clearRuntimeState(true);
        TeslaTurbineLevelCalculator calculator = core.getLevelCalculator();
        calculator.loadSupplyLevel(0);
        calculator.loadTypeLevel();
    }

    public GasStack getGasType() {
        return gasType;
    }

    private void setGasType(GasStack gas) {
        GasStack normalizedGas = gas.isEmpty() ? GasStack.EMPTY : gas.copyWithAmount(1);
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

    public void reset(boolean resetGasType) {
        boolean changed = hasRuntimeState(resetGasType);
        clearRuntimeState(resetGasType);

        TeslaTurbineLevelCalculator calculator = core.getLevelCalculator();
        calculator.loadSupplyLevel(0);
        calculator.loadTypeLevel();
        if (!changed) {
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
        clearRuntimeState(true);
        gasType = readNormalizedGas(compoundTag, provider);
        hasMixedGases = !gasType.isEmpty() && compoundTag.getBoolean(COMPOUND_KEY_HAS_MIXED_GASES);
        currentIndex = Math.floorMod(compoundTag.getInt(COMPOUND_KEY_CURRENT_INDEX), TeslaTurbineUtils.FLOW_SAMPLE_COUNT);
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

    private void updateDerivedFlow(boolean notify) {
        float totalNetFlow = 0;
        float totalAbsoluteFlow = 0;
        for (int i = 0; i < TeslaTurbineUtils.FLOW_SAMPLE_COUNT; i++) {
            totalNetFlow += netFlowOverTime[i];
            totalAbsoluteFlow += absoluteFlowOverTime[i];
        }

        netFlow = Float.isFinite(totalNetFlow) ? totalNetFlow / TeslaTurbineUtils.FLOW_SAMPLE_COUNT : 0;
        absoluteFlow = Float.isFinite(totalAbsoluteFlow) ? Math.max(0, totalAbsoluteFlow / TeslaTurbineUtils.FLOW_SAMPLE_COUNT) : 0;
        boolean gasSupplyEnded = absoluteFlow < TeslaTurbineUtils.MIN_GAS_SUPPLY_THRESHOLD && gatheredClockwise == 0 && gatheredCounterClockwise == 0 && !gasType.isEmpty();
        if (gasSupplyEnded) {
            if (notify) {
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
        if (notify) {
            core.getLevelCalculator().updateSupplyLevel(supplyLevel);
        }
        else {
            core.getLevelCalculator().loadSupplyLevel(supplyLevel);
        }
    }

    private boolean hasPersistentSampleState() {
        return !gasType.isEmpty() || gatheredClockwise != 0 || gatheredCounterClockwise != 0 || netFlowOverTime[currentIndex] != 0 || absoluteFlowOverTime[currentIndex] != 0;
    }

    private boolean hasRuntimeState(boolean resetGasType) {
        return netFlow != 0 || absoluteFlow != 0 || gatheredClockwise != 0 || gatheredCounterClockwise != 0 || currentIndex != 0 || ticksUntilNextSample != TeslaTurbineUtils.FLOW_SAMPLE_RATE || hasMixedGases || resetGasType && !gasType.isEmpty();
    }

    private void recordSample() {
        float clockwiseRate = gatheredClockwise > 0 ? (float) gatheredClockwise / TeslaTurbineUtils.FLOW_SAMPLE_RATE : 0;
        float counterClockwiseRate = gatheredCounterClockwise > 0 ? (float) gatheredCounterClockwise / TeslaTurbineUtils.FLOW_SAMPLE_RATE : 0;
        netFlowOverTime[currentIndex] = clockwiseRate - counterClockwiseRate;
        absoluteFlowOverTime[currentIndex] = clockwiseRate + counterClockwiseRate;
        currentIndex = (currentIndex + 1) % TeslaTurbineUtils.FLOW_SAMPLE_COUNT;
        gatheredClockwise = 0;
        gatheredCounterClockwise = 0;
    }

    private void clearRuntimeState(boolean resetGasType) {
        clearFlowSamples();
        hasMixedGases = false;
        if (!resetGasType) {
            return;
        }

        gasType = GasStack.EMPTY;
    }

    private void clearFlowSamples() {
        netFlow = 0;
        absoluteFlow = 0;
        gatheredClockwise = 0;
        gatheredCounterClockwise = 0;
        ticksUntilNextSample = TeslaTurbineUtils.FLOW_SAMPLE_RATE;
        currentIndex = 0;
        Arrays.fill(netFlowOverTime, 0);
        Arrays.fill(absoluteFlowOverTime, 0);
    }
}
