package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandler;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlers;
import net.ty.createcraftedbeginning.api.enginehandlers.DefaultEngineHandler;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfileCompoundTags;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.foundation.NbtValues;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightAssemblyDriverFlowMeter {
    static final int SUPPLY_PER_LEVEL = 256;
    private static final int SAMPLE_RATE = 5;
    private static final int SAMPLES_COUNT = 10;
    private static final int SAMPLE_WINDOW_TICKS = SAMPLE_RATE * SAMPLES_COUNT;
    private static final long MAX_SAMPLE_INPUT = Long.MAX_VALUE / SAMPLES_COUNT;
    private static final float MIN_DISPLAYED_GAS_SUPPLY = 0.005F;
    private static final double MAX_SAMPLE_WORK = AirtightAssemblyDriverCore.MAX_LEVEL * SUPPLY_PER_LEVEL * SAMPLE_RATE;

    private static final String COMPOUND_KEY_GAS = "Gas";
    private static final String COMPOUND_KEY_PRESSURE_PROFILE = "PressureProfile";
    private static final String COMPOUND_KEY_GAS_SUPPLY = "GasSupply";
    private static final String COMPOUND_KEY_CURRENT_INDEX = "CurrentIndex";
    private static final String COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE = "TicksUntilNextSample";
    private static final String COMPOUND_KEY_GATHERED_SUPPLY = "GatheredSupply";
    private static final String COMPOUND_KEY_SAMPLES = "Samples";

    private static final String COMPOUND_KEY_GATHERED_WORK = "GatheredWork";
    private static final String COMPOUND_KEY_GATHERED_WORK_TOLERANCE = "GatheredWorkTolerance";
    private static final String COMPOUND_KEY_WORK_SAMPLES = "WorkSamples";
    private static final String COMPOUND_KEY_WORK_TOLERANCES = "WorkTolerances";

    private final AirtightAssemblyDriverCore driverCore;
    private final long[] suppliedPerSample = new long[SAMPLES_COUNT];

    private final double[] workPerSample = new double[SAMPLES_COUNT];
    private final double[] workTolerancePerSample = new double[SAMPLES_COUNT];

    private double gatheredWork;
    private double gatheredWorkTolerance;
    private float gasSupply;
    private GasStack gasType = GasStack.EMPTY;
    private GameplayPressureProfile pressureProfile = GameplayPressureProfiles.NORMAL;
    private int currentIndex;
    private int ticksUntilNextSample = SAMPLE_RATE;
    private long gatheredSupply;
    private long rollingSupply;

    AirtightAssemblyDriverFlowMeter(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    private static AirtightEngineHandler getHandler(GasStack gasStack, GameplayPressureProfile pressureProfile) {
        if (gasStack.isEmpty()) {
            return DefaultEngineHandler.INSTANCE;
        }

        return AirtightEngineHandlers.resolve(gasStack, pressureProfile);
    }

    private static double getWorkFactor(AirtightEngineHandler engineHandler) {
        double workFactor = engineHandler.getWorkFactor();
        if (!GasConsumptionMath.isFinite(workFactor) || workFactor <= 0) {
            return 0;
        }

        return workFactor;
    }

    private static int getMaxLevel(AirtightEngineHandler engineHandler) {
        return Mth.clamp(engineHandler.getMaxLevel(), 0, AirtightAssemblyDriverCore.MAX_LEVEL);
    }

    private static ListTag writeWorkSamples(double[] samples) {
        ListTag tag = new ListTag();
        for (double sample : samples) {
            tag.add(DoubleTag.valueOf(sample));
        }
        return tag;
    }

    private static double clampWork(double work) {
        if (!Double.isFinite(work)) {
            return 0;
        }

        return Mth.clamp(work, 0, MAX_SAMPLE_WORK);
    }

    private static void readWorkSamples(CompoundTag compoundTag, String key, double[] samples) {
        Arrays.fill(samples, 0);
        ListTag storedSamples = compoundTag.getList(key, Tag.TAG_DOUBLE);
        for (int index = 0; index < Math.min(samples.length, storedSamples.size()); index++) {
            samples[index] = clampWork(storedSamples.getDouble(index));
        }
    }

    private static GasStack readNormalizedGas(CompoundTag compoundTag, Provider provider) {
        if (!compoundTag.contains(COMPOUND_KEY_GAS)) {
            return GasStack.EMPTY;
        }

        GasStack storedGas = GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_GAS));
        if (storedGas.isEmpty()) {
            return GasStack.EMPTY;
        }

        return storedGas.copyWithAmount(1);
    }

    long fill(GasStack resource, long sourcePressurePa, GasAction action) {
        GameplayPressureProfile incomingProfile = GameplayPressureProfiles.resolve(sourcePressurePa);
        if (resource.isEmpty() || !canAcceptGas(resource)) {
            return 0;
        }

        boolean switchesPressureProfile = !gasType.isEmpty() && !pressureProfile.equals(incomingProfile);
        AirtightEngineHandler incomingHandler = getHandler(resource, incomingProfile);
        double workFactor = getWorkFactor(incomingHandler);
        double remainingWork = Math.max(0, (double) getMaxLevel(incomingHandler) * SUPPLY_PER_LEVEL * SAMPLE_RATE - gatheredWork);
        if (workFactor <= 0 || remainingWork <= 0) {
            return 0;
        }

        double requiredInput = Math.ceil(remainingWork / workFactor);
        long remainingInput = (long) Math.min(MAX_SAMPLE_INPUT - gatheredSupply, requiredInput);
        long acceptedAmount = Math.min(resource.getAmount(), remainingInput);
        if (acceptedAmount <= 0 || !action.execute()) {
            return acceptedAmount;
        }

        if (gasType.isEmpty()) {
            setFuelMode(resource.copyWithAmount(1), incomingProfile, true);
        }
        else if (switchesPressureProfile) {
            setFuelMode(resource.copyWithAmount(1), incomingProfile, false);
        }
        gatheredSupply += acceptedAmount;
        gatheredWork += Math.min(remainingWork, acceptedAmount * workFactor);
        gatheredWorkTolerance = Math.max(gatheredWorkTolerance, workFactor);
        driverCore.markForSave();
        return acceptedAmount;
    }

    void tick(Level level) {
        if (level.isClientSide || gasType.isEmpty() && rollingSupply == 0 && gatheredSupply == 0) {
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
        workPerSample[currentIndex] = gatheredWork;
        workTolerancePerSample[currentIndex] = gatheredWorkTolerance;
        rollingSupply += gatheredSupply;
        currentIndex = (currentIndex + 1) % SAMPLES_COUNT;
        gatheredSupply = 0;
        gatheredWork = 0;
        gatheredWorkTolerance = 0;
        updateGasSupply();
        driverCore.markForSave();
        if (hadDisplayableSupply == hasDisplayableGasSupply()) {
            return;
        }

        driverCore.markForClientSync();
    }

    CompoundTag write(Provider provider, boolean clientPacket) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_GAS, gasType.saveOptional(provider));
        GameplayPressureProfileCompoundTags.write(compoundTag, COMPOUND_KEY_PRESSURE_PROFILE, pressureProfile);
        if (clientPacket) {
            compoundTag.putFloat(COMPOUND_KEY_GAS_SUPPLY, gasSupply);
            return compoundTag;
        }

        compoundTag.putInt(COMPOUND_KEY_CURRENT_INDEX, currentIndex);
        compoundTag.putInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE, ticksUntilNextSample);
        compoundTag.putLong(COMPOUND_KEY_GATHERED_SUPPLY, gatheredSupply);
        compoundTag.putLongArray(COMPOUND_KEY_SAMPLES, suppliedPerSample);
        compoundTag.putDouble(COMPOUND_KEY_GATHERED_WORK, gatheredWork);
        compoundTag.putDouble(COMPOUND_KEY_GATHERED_WORK_TOLERANCE, gatheredWorkTolerance);
        compoundTag.put(COMPOUND_KEY_WORK_SAMPLES, writeWorkSamples(workPerSample));
        compoundTag.put(COMPOUND_KEY_WORK_TOLERANCES, writeWorkSamples(workTolerancePerSample));
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
        driverCore.getLevelCalculator().loadSupplyLevel(0);
    }

    void reset() {
        boolean samplesChanged = rollingSupply != 0 || gatheredSupply != 0 || currentIndex != 0 || ticksUntilNextSample != SAMPLE_RATE;
        boolean gasChanged = !gasType.isEmpty();
        clearSamples();
        gasType = GasStack.EMPTY;
        pressureProfile = GameplayPressureProfiles.NORMAL;
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

    private void setFuelMode(GasStack gasStack, GameplayPressureProfile newPressureProfile, boolean applyGasChangePenalty) {
        GasStack normalized = gasStack.isEmpty() ? GasStack.EMPTY : gasStack.copyWithAmount(1);
        GameplayPressureProfile normalizedProfile = normalized.isEmpty() ? GameplayPressureProfiles.NORMAL : newPressureProfile;
        boolean gasChanged = !GasStack.isSameGasSameComponents(gasType, normalized);
        boolean pressureChanged = !pressureProfile.equals(normalizedProfile);
        if (!gasChanged && !pressureChanged) {
            return;
        }

        gasType = normalized;
        pressureProfile = normalizedProfile;
        if (gasChanged && applyGasChangePenalty) {
            driverCore.getResidueManager().applyRemovalPenalty();
        }
        driverCore.markForSaveAndClientSync();
    }

    private void readClient(CompoundTag compoundTag, Provider provider) {
        gasType = readNormalizedGas(compoundTag, provider);
        pressureProfile = gasType.isEmpty() ? GameplayPressureProfiles.NORMAL : GameplayPressureProfileCompoundTags.read(compoundTag, COMPOUND_KEY_PRESSURE_PROFILE);
        float storedSupply = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_GAS_SUPPLY, 0);
        gasSupply = GasConsumptionMath.isFinite(storedSupply) ? Mth.clamp(storedSupply, 0.0F, (float) AirtightAssemblyDriverCore.MAX_LEVEL * SUPPLY_PER_LEVEL) : 0;
        if (!gasType.isEmpty()) {
            return;
        }

        gasSupply = 0;
    }

    private void readPersistent(CompoundTag compoundTag, Provider provider) {
        clearRuntimeState();
        gasType = readNormalizedGas(compoundTag, provider);
        pressureProfile = gasType.isEmpty() ? GameplayPressureProfiles.NORMAL : GameplayPressureProfileCompoundTags.read(compoundTag, COMPOUND_KEY_PRESSURE_PROFILE);
        AirtightEngineHandler engineHandler = getHandler(gasType, pressureProfile);
        if (gasType.isEmpty() || getWorkFactor(engineHandler) <= 0 || getMaxLevel(engineHandler) <= 0) {
            gasType = GasStack.EMPTY;
            pressureProfile = GameplayPressureProfiles.NORMAL;
            driverCore.getLevelCalculator().loadSupplyLevel(0);
            return;
        }

        currentIndex = Mth.positiveModulo(compoundTag.getInt(COMPOUND_KEY_CURRENT_INDEX), SAMPLES_COUNT);
        ticksUntilNextSample = Mth.clamp(NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE, SAMPLE_RATE), 1, SAMPLE_RATE);
        gatheredSupply = Mth.clamp(compoundTag.getLong(COMPOUND_KEY_GATHERED_SUPPLY), 0L, MAX_SAMPLE_INPUT);
        gatheredWork = clampWork(NbtValues.getDoubleOrDefault(compoundTag, COMPOUND_KEY_GATHERED_WORK, 0));
        gatheredWorkTolerance = clampWork(NbtValues.getDoubleOrDefault(compoundTag, COMPOUND_KEY_GATHERED_WORK_TOLERANCE, 0));
        if (gatheredSupply == 0) {
            gatheredWork = 0;
            gatheredWorkTolerance = 0;
        }
        readSamples(compoundTag);
        if (rollingSupply == 0 && gatheredSupply == 0) {
            gasType = GasStack.EMPTY;
            pressureProfile = GameplayPressureProfiles.NORMAL;
        }
        updateDerivedSupply(false);
    }

    private void readSamples(CompoundTag compoundTag) {
        Arrays.fill(suppliedPerSample, 0);
        if (compoundTag.contains(COMPOUND_KEY_SAMPLES)) {
            long[] storedSamples = compoundTag.getLongArray(COMPOUND_KEY_SAMPLES);
            for (int sampleIndex = 0; sampleIndex < Math.min(SAMPLES_COUNT, storedSamples.length); sampleIndex++) {
                suppliedPerSample[sampleIndex] = Mth.clamp(storedSamples[sampleIndex], 0L, MAX_SAMPLE_INPUT);
            }
        }

        readWorkSamples(compoundTag, COMPOUND_KEY_WORK_SAMPLES, workPerSample);
        readWorkSamples(compoundTag, COMPOUND_KEY_WORK_TOLERANCES, workTolerancePerSample);
        rollingSupply = 0;
        for (int index = 0; index < SAMPLES_COUNT; index++) {
            rollingSupply += suppliedPerSample[index];
            if (suppliedPerSample[index] != 0) {
                continue;
            }

            workPerSample[index] = 0;
            workTolerancePerSample[index] = 0;
        }
    }

    private void updateGasSupply() {
        updateDerivedSupply(true);
        if (rollingSupply != 0 || gatheredSupply != 0 || gasType.isEmpty()) {
            return;
        }

        setFuelMode(GasStack.EMPTY, GameplayPressureProfiles.NORMAL, true);
    }

    private void updateDerivedSupply(boolean notifyChanges) {
        gasSupply = (float) rollingSupply / SAMPLE_WINDOW_TICKS;
        double rollingWork = 0;
        double workTolerance = 0;
        for (int index = 0; index < SAMPLES_COUNT; index++) {
            rollingWork += workPerSample[index];
            workTolerance = Math.max(workTolerance, workTolerancePerSample[index]);
        }

        double rawLevels = rollingWork / (SAMPLE_WINDOW_TICKS * SUPPLY_PER_LEVEL);
        double quantizedLevels = Math.floor((rollingWork + workTolerance) / (SAMPLE_WINDOW_TICKS * SUPPLY_PER_LEVEL));
        double supplyLevel = Math.min(AirtightAssemblyDriverCore.MAX_LEVEL, Math.max(rawLevels, quantizedLevels));
        if (notifyChanges) {
            driverCore.getLevelCalculator().updateSupplyLevel(supplyLevel);
            return;
        }

        driverCore.getLevelCalculator().loadSupplyLevel(supplyLevel);
    }

    private boolean canAcceptGas(GasStack resource) {
        return gasType.isEmpty() || GasStack.isSameGasSameComponents(gasType, resource);
    }

    private void clearRuntimeState() {
        gasType = GasStack.EMPTY;
        pressureProfile = GameplayPressureProfiles.NORMAL;
        clearSamples();
    }

    private void clearSamples() {
        gasSupply = 0;
        currentIndex = 0;
        ticksUntilNextSample = SAMPLE_RATE;
        gatheredSupply = 0;
        rollingSupply = 0;
        Arrays.fill(suppliedPerSample, 0);
        Arrays.fill(workPerSample, 0);
        Arrays.fill(workTolerancePerSample, 0);
        gatheredWork = 0;
        gatheredWorkTolerance = 0;
    }
}
