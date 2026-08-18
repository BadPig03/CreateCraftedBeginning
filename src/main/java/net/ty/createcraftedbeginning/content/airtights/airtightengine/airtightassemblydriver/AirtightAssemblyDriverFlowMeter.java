package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandler;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlerUtils;
import net.ty.createcraftedbeginning.api.enginehandlers.DefaultEngineHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightAssemblyDriverFlowMeter {
    private static final int SAMPLE_RATE = 5;
    private static final int SAMPLES_COUNT = 10;
    private static final int SAMPLE_WINDOW_TICKS = SAMPLE_RATE * SAMPLES_COUNT;
    private static final int SUPPLY_PER_LEVEL = 16;
    private static final long MAX_SAMPLE_INPUT = Long.MAX_VALUE / SAMPLES_COUNT;
    private static final float MIN_DISPLAYED_GAS_SUPPLY = 0.005f;

    private static final String COMPOUND_KEY_GAS = "Gas";
    private static final String COMPOUND_KEY_GAS_SUPPLY = "GasSupply";
    private static final String COMPOUND_KEY_CURRENT_INDEX = "CurrentIndex";
    private static final String COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE = "TicksUntilNextSample";
    private static final String COMPOUND_KEY_GATHERED_SUPPLY = "GatheredSupply";
    private static final String COMPOUND_KEY_SAMPLES = "Samples";

    private final AirtightAssemblyDriverCore driverCore;
    private final long[] suppliedPerSample = new long[SAMPLES_COUNT];

    private float gasSupply;
    private GasStack gasType = GasStack.EMPTY;
    private int currentIndex;
    private int ticksUntilNextSample = SAMPLE_RATE;
    private long gatheredSupply;
    private long rollingSupply;

    AirtightAssemblyDriverFlowMeter(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    private static float getMaxDisplayedGasSupply() {
        return (float) AirtightAssemblyDriverCore.MAX_LEVEL * SUPPLY_PER_LEVEL;
    }

    private static AirtightEngineHandler getHandler(GasStack gasStack) {
        return gasStack.isEmpty() ? DefaultEngineHandler.INSTANCE : AirtightEngineHandlerUtils.of(gasStack);
    }

    private static double getWorkFactor(AirtightEngineHandler handler) {
        double workFactor = handler.getWorkFactor();
        return GasConsumptions.isFinite(workFactor) && workFactor > 0 ? workFactor : 0;
    }

    private static int getMaxLevel(AirtightEngineHandler handler) {
        return Math.clamp(handler.getMaxLevel(), 0, AirtightAssemblyDriverCore.MAX_LEVEL);
    }

    private static long getMaxInput(AirtightEngineHandler handler) {
        double workFactor = getWorkFactor(handler);
        int maxLevel = getMaxLevel(handler);
        if (workFactor <= 0 || maxLevel <= 0) {
            return 0;
        }

        double requiredInput = (double) maxLevel * SUPPLY_PER_LEVEL * SAMPLE_RATE / workFactor;
        if (!GasConsumptions.isFinite(requiredInput) || requiredInput >= MAX_SAMPLE_INPUT) {
            return MAX_SAMPLE_INPUT;
        }
        return Math.max(1, (long) Mth.ceil(requiredInput));
    }

    private static GasStack readNormalizedGas(CompoundTag compoundTag, Provider provider) {
        if (!compoundTag.contains(COMPOUND_KEY_GAS)) {
            return GasStack.EMPTY;
        }

        GasStack storedGas = GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_GAS));
        return storedGas.isEmpty() ? GasStack.EMPTY : storedGas.copyWithAmount(1);
    }

    long fill(GasStack resource, GasAction action) {
        if (resource.isEmpty() || !canAcceptGas(resource)) {
            return 0;
        }

        long remainingInput = getRemainingInput(resource);
        long acceptedAmount = Math.min(resource.getAmount(), remainingInput);
        if (acceptedAmount <= 0) {
            return 0;
        }

        if (!action.execute()) {
            return acceptedAmount;
        }

        if (gasType.isEmpty()) {
            setGasType(resource.copyWithAmount(1));
        }
        gatheredSupply += acceptedAmount;
        driverCore.markForSave();
        return acceptedAmount;
    }

    void tick(Level level) {
        if (level.isClientSide) {
            return;
        }

        if (gasType.isEmpty() && rollingSupply == 0 && gatheredSupply == 0) {
            return;
        }

        ticksUntilNextSample--;
        if (ticksUntilNextSample > 0) {
            return;
        }

        ticksUntilNextSample = SAMPLE_RATE;
        boolean hadDisplayableSupply = hasDisplayableGasSupply();
        rollingSupply -= suppliedPerSample[currentIndex];
        suppliedPerSample[currentIndex] = gatheredSupply;
        rollingSupply += gatheredSupply;
        currentIndex = (currentIndex + 1) % SAMPLES_COUNT;
        gatheredSupply = 0;
        updateGasSupply();
        driverCore.markForSave();
        if (hadDisplayableSupply == hasDisplayableGasSupply()) {
            return;
        }

        driverCore.markForClientSync();
    }

    CompoundTag write(Provider provider, boolean clientPacket) {
        CompoundTag tag = new CompoundTag();
        tag.put(COMPOUND_KEY_GAS, gasType.saveOptional(provider));
        if (clientPacket) {
            tag.putFloat(COMPOUND_KEY_GAS_SUPPLY, gasSupply);
            return tag;
        }

        tag.putInt(COMPOUND_KEY_CURRENT_INDEX, currentIndex);
        tag.putInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE, ticksUntilNextSample);
        tag.putLong(COMPOUND_KEY_GATHERED_SUPPLY, gatheredSupply);
        tag.putLongArray(COMPOUND_KEY_SAMPLES, suppliedPerSample);
        return tag;
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
        driverCore.getLevelCalculator().loadSupplyLevel(0);
    }

    void reset() {
        boolean samplesChanged = rollingSupply != 0 || gatheredSupply != 0 || currentIndex != 0 || ticksUntilNextSample != SAMPLE_RATE;
        boolean gasChanged = !gasType.isEmpty();
        clearSamples();
        gasType = GasStack.EMPTY;
        driverCore.getLevelCalculator().updateSupplyLevel(0);
        driverCore.getResidueManager().applyRemovalPenalty();
        if (samplesChanged || gasChanged) {
            driverCore.markForSave();
        }
        if (!gasChanged) {
            return;
        }

        driverCore.markForClientSync();
    }

    boolean hasDisplayableGasSupply() {
        return gasSupply >= MIN_DISPLAYED_GAS_SUPPLY;
    }

    GasStack getGasType() {
        return gasType;
    }

    private void setGasType(GasStack gasStack) {
        GasStack normalized = gasStack.isEmpty() ? GasStack.EMPTY : gasStack.copyWithAmount(1);
        if (GasStack.isSameGasSameComponents(gasType, normalized)) {
            return;
        }

        gasType = normalized;
        driverCore.getResidueManager().applyRemovalPenalty();
        driverCore.markForSaveAndClientSync();
    }

    private void readClient(CompoundTag compoundTag, Provider provider) {
        gasType = readNormalizedGas(compoundTag, provider);
        float storedSupply = compoundTag.contains(COMPOUND_KEY_GAS_SUPPLY) ? compoundTag.getFloat(COMPOUND_KEY_GAS_SUPPLY) : 0;
        gasSupply = GasConsumptions.isFinite(storedSupply) ? Math.clamp(storedSupply, 0, getMaxDisplayedGasSupply()) : 0;
        if (!gasType.isEmpty()) {
            return;
        }

        gasSupply = 0;
    }

    private void readPersistent(CompoundTag compoundTag, Provider provider) {
        clearRuntimeState();
        gasType = readNormalizedGas(compoundTag, provider);
        AirtightEngineHandler handler = getHandler(gasType);
        if (gasType.isEmpty() || getWorkFactor(handler) <= 0 || getMaxLevel(handler) <= 0) {
            gasType = GasStack.EMPTY;
            driverCore.getLevelCalculator().loadSupplyLevel(0);
            return;
        }

        long maxInput = getMaxInput(handler);
        currentIndex = Math.floorMod(compoundTag.getInt(COMPOUND_KEY_CURRENT_INDEX), SAMPLES_COUNT);
        ticksUntilNextSample = compoundTag.contains(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE) ? Math.clamp(compoundTag.getInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE), 1, SAMPLE_RATE) : SAMPLE_RATE;
        long storedSupply = compoundTag.getLong(COMPOUND_KEY_GATHERED_SUPPLY);
        gatheredSupply = Math.clamp(storedSupply, 0, maxInput);
        readSamples(compoundTag, maxInput);
        if (rollingSupply == 0 && gatheredSupply == 0) {
            gasType = GasStack.EMPTY;
        }
        updateDerivedSupply(false);
    }

    private void readSamples(CompoundTag compoundTag, long maxInput) {
        Arrays.fill(suppliedPerSample, 0);
        if (compoundTag.contains(COMPOUND_KEY_SAMPLES)) {
            long[] storedSamples = compoundTag.getLongArray(COMPOUND_KEY_SAMPLES);
            for (int i = 0; i < Math.min(SAMPLES_COUNT, storedSamples.length); i++) {
                suppliedPerSample[i] = Math.clamp(storedSamples[i], 0, maxInput);
            }
        }

        rollingSupply = 0;
        for (long sample : suppliedPerSample) {
            rollingSupply += sample;
        }
    }

    private void updateGasSupply() {
        updateDerivedSupply(true);
        if (rollingSupply != 0 || gatheredSupply != 0 || gasType.isEmpty()) {
            return;
        }

        setGasType(GasStack.EMPTY);
    }

    private void updateDerivedSupply(boolean notifyChanges) {
        gasSupply = (float) rollingSupply / SAMPLE_WINDOW_TICKS;
        AirtightEngineHandler handler = getHandler(gasType);
        double workFactor = getWorkFactor(handler);
        int maxLevel = getMaxLevel(handler);
        int supplyLevel = workFactor <= 0 || maxLevel <= 0 ? 0 : (int) Math.min(maxLevel, Math.floor(rollingSupply * workFactor / (SAMPLE_WINDOW_TICKS * (double) SUPPLY_PER_LEVEL)));
        if (notifyChanges) {
            driverCore.getLevelCalculator().updateSupplyLevel(supplyLevel);
            return;
        }

        driverCore.getLevelCalculator().loadSupplyLevel(supplyLevel);
    }

    private boolean canAcceptGas(GasStack resource) {
        return gasType.isEmpty() || GasStack.isSameGasSameComponents(gasType, resource);
    }

    private long getRemainingInput(GasStack gas) {
        AirtightEngineHandler handler = getHandler(gas);
        if (getWorkFactor(handler) <= 0 || getMaxLevel(handler) <= 0) {
            return 0;
        }
        return Math.max(0, getMaxInput(handler) - gatheredSupply);
    }

    private void clearRuntimeState() {
        gasType = GasStack.EMPTY;
        clearSamples();
    }

    private void clearSamples() {
        gasSupply = 0;
        currentIndex = 0;
        ticksUntilNextSample = SAMPLE_RATE;
        gatheredSupply = 0;
        rollingSupply = 0;
        Arrays.fill(suppliedPerSample, 0);
    }
}
