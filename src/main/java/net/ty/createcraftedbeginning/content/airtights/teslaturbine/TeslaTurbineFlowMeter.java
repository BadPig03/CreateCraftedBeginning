package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineFlowMeter {
    private static final int SAMPLE_RATE = 5;
    private static final int SAMPLES_COUNT = 10;
    private static final int SUPPLY_PER_LEVEL = 32;
    private static final float MIN_GAS_SUPPLY_THRESHOLD = 0.01f;

    private static final String COMPOUND_KEY_GAS = "Gas";
    private static final String COMPOUND_KEY_NET_FLOW = "NetFlow";
    private static final String COMPOUND_KEY_ABSOLUTE_FLOW = "AbsoluteFlow";
    private static final String COMPOUND_KEY_PREVIOUS_NET_FLOW = "PreviousNetFlow";
    private static final String COMPOUND_KEY_CURRENT_INDEX = "CurrentIndex";
    private static final String COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE = "TicksUntilNextSample";
    private static final String COMPOUND_KEY_HAS_MIXED_GASES = "HasMixedGases";
    private static final String COMPOUND_KEY_NET_SAMPLES = "NetSamples";
    private static final String COMPOUND_KEY_ABSOLUTE_SAMPLES = "AbsoluteSamples";

    private final TeslaTurbineCore core;
    private final TeslaTurbineBlockEntity turbine;
    private final float[] netFlowOverTime = new float[SAMPLES_COUNT];
    private final float[] absoluteFlowOverTime = new float[SAMPLES_COUNT];

    private boolean hasMixedGases;
    private float absoluteFlow;
    private float netFlow;
    private float previousNetFlow = -1;
    private GasStack gasType = GasStack.EMPTY;
    private int currentIndex;
    private int ticksUntilNextSample = SAMPLE_RATE;
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

        boolean newGasType = !gasType.is(resource.getGasType());
        if (!gasType.isEmpty() && newGasType) {
            hasMixedGases = true;
            turbine.setChanged();
            return 0;
        }

        if (newGasType) {
            setGasType(resource.copyWithAmount(1));
        }

        if (clockwise) {
            gatheredClockwise = saturatedAdd(gatheredClockwise, amount);
        }
        else {
            gatheredCounterClockwise = saturatedAdd(gatheredCounterClockwise, amount);
        }
        return amount;
    }

    public void tick() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        if (hasMixedGases) {
            core.getStructureManager().triggerExplosion();
            reset(true);
            return;
        }

        ticksUntilNextSample--;
        if (ticksUntilNextSample > 0) {
            return;
        }

        ticksUntilNextSample = SAMPLE_RATE;
        float clockwiseRate = gatheredClockwise > 0 ? (float) gatheredClockwise / SAMPLE_RATE : 0;
        float counterClockwiseRate = gatheredCounterClockwise > 0 ? (float) gatheredCounterClockwise / SAMPLE_RATE : 0;
        netFlowOverTime[currentIndex] = clockwiseRate - counterClockwiseRate;
        absoluteFlowOverTime[currentIndex] = clockwiseRate + counterClockwiseRate;
        currentIndex = (currentIndex + 1) % SAMPLES_COUNT;
        gatheredClockwise = 0;
        gatheredCounterClockwise = 0;
        updateFlow();
    }

    private void updateFlow() {
        float totalNetFlow = 0;
        float totalAbsoluteFlow = 0;
        for (int i = 0; i < SAMPLES_COUNT; i++) {
            totalNetFlow += netFlowOverTime[i];
            totalAbsoluteFlow += absoluteFlowOverTime[i];
        }

        netFlow = totalNetFlow / SAMPLES_COUNT;
        absoluteFlow = totalAbsoluteFlow / SAMPLES_COUNT;
        if (absoluteFlow < MIN_GAS_SUPPLY_THRESHOLD && !gasType.isEmpty()) {
            setGasType(GasStack.EMPTY);
            return;
        }

        if (Float.compare(previousNetFlow, netFlow) == 0) {
            return;
        }

        int newLevel = gasType.isEmpty() ? 0 : (int) (Math.abs(netFlow) / SUPPLY_PER_LEVEL);
        core.getLevelCalculator().updateSupplyLevel(newLevel);
        previousNetFlow = netFlow;
        turbine.setChanged();
    }

    public boolean isClockwiseFlow() {
        return netFlow > 0;
    }

    public CompoundTag write(Provider lookupProvider) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_GAS, gasType.saveOptional(lookupProvider));
        compoundTag.putBoolean(COMPOUND_KEY_HAS_MIXED_GASES, hasMixedGases);
        compoundTag.putFloat(COMPOUND_KEY_ABSOLUTE_FLOW, absoluteFlow);
        compoundTag.putFloat(COMPOUND_KEY_NET_FLOW, netFlow);
        compoundTag.putFloat(COMPOUND_KEY_PREVIOUS_NET_FLOW, previousNetFlow);
        compoundTag.putInt(COMPOUND_KEY_CURRENT_INDEX, currentIndex);
        compoundTag.putInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE, ticksUntilNextSample);
        ListTag netSamplesTag = new ListTag();
        for (int i = 0; i < SAMPLES_COUNT; i++) {
            netSamplesTag.add(FloatTag.valueOf(netFlowOverTime[i]));
        }
        compoundTag.put(COMPOUND_KEY_NET_SAMPLES, netSamplesTag);

        ListTag absoluteSamplesTag = new ListTag();
        for (int i = 0; i < SAMPLES_COUNT; i++) {
            absoluteSamplesTag.add(FloatTag.valueOf(absoluteFlowOverTime[i]));
        }
        compoundTag.put(COMPOUND_KEY_ABSOLUTE_SAMPLES, absoluteSamplesTag);
        return compoundTag;
    }

    public void read(CompoundTag compoundTag, Provider lookupProvider) {
        gatheredClockwise = 0;
        gatheredCounterClockwise = 0;
        if (compoundTag.contains(COMPOUND_KEY_ABSOLUTE_FLOW)) {
            absoluteFlow = compoundTag.getFloat(COMPOUND_KEY_ABSOLUTE_FLOW);
        }
        if (compoundTag.contains(COMPOUND_KEY_CURRENT_INDEX)) {
            currentIndex = compoundTag.getInt(COMPOUND_KEY_CURRENT_INDEX);
        }
        if (compoundTag.contains(COMPOUND_KEY_GAS)) {
            gasType = GasStack.parseOptional(lookupProvider, compoundTag.getCompound(COMPOUND_KEY_GAS));
        }
        if (compoundTag.contains(COMPOUND_KEY_HAS_MIXED_GASES)) {
            hasMixedGases = compoundTag.getBoolean(COMPOUND_KEY_HAS_MIXED_GASES);
        }
        if (compoundTag.contains(COMPOUND_KEY_NET_FLOW)) {
            netFlow = compoundTag.getFloat(COMPOUND_KEY_NET_FLOW);
        }
        if (compoundTag.contains(COMPOUND_KEY_PREVIOUS_NET_FLOW)) {
            previousNetFlow = compoundTag.getFloat(COMPOUND_KEY_PREVIOUS_NET_FLOW);
        }
        if (compoundTag.contains(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE)) {
            ticksUntilNextSample = compoundTag.getInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE);
        }
        if (compoundTag.contains(COMPOUND_KEY_NET_SAMPLES, Tag.TAG_LIST)) {
            ListTag netSamplesTag = compoundTag.getList(COMPOUND_KEY_NET_SAMPLES, Tag.TAG_FLOAT);
            for (int i = 0; i < Math.min(SAMPLES_COUNT, netSamplesTag.size()); i++) {
                netFlowOverTime[i] = netSamplesTag.getFloat(i);
            }
        }
        if (compoundTag.contains(COMPOUND_KEY_ABSOLUTE_SAMPLES, Tag.TAG_LIST)) {
            ListTag absoluteSamplesTag = compoundTag.getList(COMPOUND_KEY_ABSOLUTE_SAMPLES, Tag.TAG_FLOAT);
            for (int i = 0; i < Math.min(SAMPLES_COUNT, absoluteSamplesTag.size()); i++) {
                absoluteFlowOverTime[i] = absoluteSamplesTag.getFloat(i);
            }
        }
    }

    public float getNetFlow() {
        return netFlow;
    }

    public float getAbsoluteFlow() {
        return absoluteFlow;
    }

    public GasStack getGasType() {
        return gasType;
    }

    private void setGasType(GasStack newGasStack) {
        boolean changed = !GasStack.isSameGas(gasType, newGasStack);
        gasType = newGasStack;
        reset(false);
        if (!changed) {
            return;
        }

        turbine.setChanged();
    }

    public void reset(boolean resetGasType) {
        boolean changed = netFlow != 0 || absoluteFlow != 0 || gatheredClockwise != 0 || gatheredCounterClockwise != 0 || previousNetFlow != -1 || currentIndex != 0 || ticksUntilNextSample != SAMPLE_RATE || hasMixedGases || resetGasType && !gasType.isEmpty();
        netFlow = 0;
        absoluteFlow = 0;
        gatheredClockwise = 0;
        gatheredCounterClockwise = 0;
        ticksUntilNextSample = SAMPLE_RATE;
        previousNetFlow = -1;
        currentIndex = 0;
        hasMixedGases = false;
        Arrays.fill(netFlowOverTime, 0);
        Arrays.fill(absoluteFlowOverTime, 0);
        if (resetGasType) {
            gasType = GasStack.EMPTY;
        }

        TeslaTurbineLevelCalculator levelCalculator = core.getLevelCalculator();
        levelCalculator.updateTypeLevel();
        levelCalculator.updateSupplyLevel(0);
        if (!changed) {
            return;
        }

        turbine.setChanged();
    }
}