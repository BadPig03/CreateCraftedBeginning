package net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class AirtightAssemblyDriverFlowMeter {
    private static final int SAMPLE_RATE = 5;
    private static final int SAMPLES_COUNT = 10;
    private static final int SUPPLY_PER_LEVEL = 16;
    private static final float MIN_GAS_SUPPLY_THRESHOLD = 0.01f;

    private static final String COMPOUND_KEY_GAS = "Gas";
    private static final String COMPOUND_KEY_GAS_SUPPLY = "GasSupply";
    private static final String COMPOUND_KEY_PREVIOUS_GAS_SUPPLY = "PreviousGasSupply";
    private static final String COMPOUND_KEY_CURRENT_INDEX = "CurrentIndex";
    private static final String COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE = "TicksUntilNextSample";
    private static final String COMPOUND_KEY_SAMPLES = "Samples";

    private final AirtightAssemblyDriverCore driverCore;
    private final float[] supplyOverTime = new float[SAMPLES_COUNT];
    private float gasSupply;
    private GasStack gasType = GasStack.EMPTY;
    private int currentIndex;
    private int ticksUntilNextSample = SAMPLE_RATE;
    private long gatheredSupply;
    private float previousGasSupply = -1;

    public AirtightAssemblyDriverFlowMeter(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    public long fill(@NotNull GasStack resource, @NotNull GasAction action) {
        if (resource.isEmpty()) {
            return 0;
        }

        long amount = resource.getAmount();
        if (action.execute()) {
            gatheredSupply += amount;
            if (!gasType.is(resource.getGasType())) {
                setGasType(resource.copyWithAmount(1));
            }
        }
        return amount;
    }

    public void tick(@NotNull Level level) {
        if (level.isClientSide) {
            return;
        }

        ticksUntilNextSample--;
        if (ticksUntilNextSample > 0) {
            return;
        }

        ticksUntilNextSample = SAMPLE_RATE;
        supplyOverTime[currentIndex] = gatheredSupply > 0 ? (float) gatheredSupply / SAMPLE_RATE : 0;
        currentIndex = (currentIndex + 1) % SAMPLES_COUNT;
        gatheredSupply = 0;
        updateGasSupply();
    }

    private void updateGasSupply() {
        float maxSupply = 0;
        for (float sample : supplyOverTime) {
            if (sample > maxSupply) {
                maxSupply = sample;
            }
        }
        gasSupply = maxSupply;

        if (gasSupply < MIN_GAS_SUPPLY_THRESHOLD && !gasType.isEmpty()) {
            gasSupply = 0;
            setGasType(GasStack.EMPTY);
        }
        if (previousGasSupply != gasSupply) {
            int newLevel = (int) (gasSupply * gasType.getGasType().getEngineEfficiency() / SUPPLY_PER_LEVEL);
            driverCore.getLevelCalculator().updateSupplyLevel(newLevel);
            previousGasSupply = gasSupply;
        }
    }

    public CompoundTag write(Provider provider) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_GAS, gasType.saveOptional(provider));
        compoundTag.putFloat(COMPOUND_KEY_GAS_SUPPLY, gasSupply);
        compoundTag.putFloat(COMPOUND_KEY_PREVIOUS_GAS_SUPPLY, previousGasSupply);
        compoundTag.putInt(COMPOUND_KEY_CURRENT_INDEX, currentIndex);
        compoundTag.putInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE, ticksUntilNextSample);
        ListTag samplesTag = new ListTag();
        for (int i = 0; i < SAMPLES_COUNT; i++) {
            samplesTag.add(FloatTag.valueOf(supplyOverTime[i]));
        }
        compoundTag.put(COMPOUND_KEY_SAMPLES, samplesTag);
        return compoundTag;
    }

    public void read(@NotNull CompoundTag compoundTag, Provider provider) {
        gatheredSupply = 0;
        if (compoundTag.contains(COMPOUND_KEY_CURRENT_INDEX)) {
            currentIndex = compoundTag.getInt(COMPOUND_KEY_CURRENT_INDEX);
        }
        if (compoundTag.contains(COMPOUND_KEY_GAS_SUPPLY)) {
            gasSupply = compoundTag.getFloat(COMPOUND_KEY_GAS_SUPPLY);
        }
        if (compoundTag.contains(COMPOUND_KEY_GAS)) {
            gasType = GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_GAS));
        }
        if (compoundTag.contains(COMPOUND_KEY_PREVIOUS_GAS_SUPPLY)) {
            previousGasSupply = compoundTag.getFloat(COMPOUND_KEY_PREVIOUS_GAS_SUPPLY);
        }
        if (compoundTag.contains(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE)) {
            ticksUntilNextSample = compoundTag.getInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE);
        }
        if (compoundTag.contains(COMPOUND_KEY_SAMPLES, Tag.TAG_LIST)) {
            ListTag samplesTag = compoundTag.getList(COMPOUND_KEY_SAMPLES, Tag.TAG_FLOAT);
            for (int i = 0; i < Math.min(SAMPLES_COUNT, samplesTag.size()); i++) {
                supplyOverTime[i] = samplesTag.getFloat(i);
            }
        }
    }

    public float getGasSupply() {
        return gasSupply;
    }

    public GasStack getGasType() {
        return gasType;
    }

    private void setGasType(GasStack gasStack) {
        gasType = gasStack;
        reset(false);
    }

    public void reset(boolean resetGasType) {
        gasSupply = 0;
        gatheredSupply = 0;
        ticksUntilNextSample = SAMPLE_RATE;
        currentIndex = 0;
        Arrays.fill(supplyOverTime, 0);
        if (resetGasType) {
            gasType = GasStack.EMPTY;
        }
        driverCore.getLevelCalculator().updateSupplyLevel(0);
        driverCore.getResidueManager().applyRemovalPenalty(true);
    }
}
