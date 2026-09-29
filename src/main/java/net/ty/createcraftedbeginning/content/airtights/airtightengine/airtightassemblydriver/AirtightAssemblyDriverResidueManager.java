package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverResiduePlanner.GenerationPlan;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletInsertionTarget;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.recipe.ResidueRecipeLookup;
import net.ty.createcraftedbeginning.recipe.ResidueRecipeLookup.ResidueOutput;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightAssemblyDriverResidueManager {
    private static final int BASE_FLUID_RESIDUE_PER_LEVEL = 8;
    private static final int BASE_ITEM_RESIDUE_COEFFICIENT = 8;
    private static final int ITEM_GENERATION_DENOMINATOR = MAX_LEVEL * MAX_LEVEL;
    private static final int GENERATION_INTERVAL_TICKS = 12;
    private static final int FAILURE_PENALTY_TICKS = 120;
    private static final int CONSECUTIVE_SUCCESSES_COUNT = 20;

    private static final String COMPOUND_KEY_GENERATION_COOLDOWN = "GenerationCooldown";
    private static final String COMPOUND_KEY_CONSECUTIVE_FAILURE_TICKS = "ConsecutiveFailureTicks";
    private static final String COMPOUND_KEY_SUCCESS_COUNT = "SuccessCount";
    private static final String COMPOUND_KEY_ITEM_DISTRIBUTION_CURSOR = "ItemDistributionCursor";
    private static final String COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR = "FluidDistributionCursor";
    private static final String COMPOUND_KEY_VERIFIED_OUTLET_POSITIONS = "VerifiedOutletPositions";

    private final AirtightAssemblyDriverCore driverCore;
    private List<BlockPos> outletsPositions = List.of();
    private Set<BlockPos> verifiedOutletPositions = Set.of();

    private int successCount;
    private int generationCooldown = GENERATION_INTERVAL_TICKS;
    private int consecutiveFailureTicks;
    private int itemDistributionCursor;
    private int fluidDistributionCursor;

    AirtightAssemblyDriverResidueManager(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    void tick(Level level) {
        if (outletsPositions.isEmpty() && driverCore.getLevelCalculator().getResidueLevel() > 0) {
            removeResidueLevel(true);
            return;
        }

        if (generationCooldown > 0) {
            generationCooldown--;
        }
        if (generationCooldown > 0) {
            return;
        }

        scanAndGenerateResidues(level);
        generationCooldown = GENERATION_INTERVAL_TICKS;
    }

    void reset() {
        boolean hadResidueState = successCount != 0 || generationCooldown != GENERATION_INTERVAL_TICKS || consecutiveFailureTicks != 0 || itemDistributionCursor != 0 || fluidDistributionCursor != 0 || !outletsPositions.isEmpty() || !verifiedOutletPositions.isEmpty();
        outletsPositions = List.of();
        verifiedOutletPositions = Set.of();
        successCount = 0;
        generationCooldown = GENERATION_INTERVAL_TICKS;
        consecutiveFailureTicks = 0;
        itemDistributionCursor = 0;
        fluidDistributionCursor = 0;
        if (!hadResidueState) {
            return;
        }

        driverCore.markForSave();
    }

    void loadEmptyPersistentState() {
        outletsPositions = List.of();
        verifiedOutletPositions = Set.of();
        successCount = 0;
        generationCooldown = GENERATION_INTERVAL_TICKS;
        consecutiveFailureTicks = 0;
        itemDistributionCursor = 0;
        fluidDistributionCursor = 0;
    }

    void clearOutletsPositions() {
        outletsPositions = List.of();
    }

    void updateOutletsPositions(Set<BlockPos> newPositions) {
        Set<BlockPos> normalizedPositions = Set.copyOf(newPositions);
        boolean topologyChanged = driverCore.getLevelCalculator().getResidueLevel() > 0 && !verifiedOutletPositions.equals(normalizedPositions);

        List<BlockPos> sortedPositions = new ArrayList<>(normalizedPositions);
        sortedPositions.sort(Comparator.comparingLong(BlockPos::asLong));
        outletsPositions = List.copyOf(sortedPositions);
        int previousItemCursor = itemDistributionCursor;
        int previousFluidCursor = fluidDistributionCursor;
        if (outletsPositions.isEmpty()) {
            itemDistributionCursor = 0;
            fluidDistributionCursor = 0;
        }
        else {
            itemDistributionCursor = Mth.positiveModulo(itemDistributionCursor, outletsPositions.size());
            fluidDistributionCursor = Mth.positiveModulo(fluidDistributionCursor, outletsPositions.size());
        }

        if (topologyChanged) {
            removeResidueLevel(false);
            verifiedOutletPositions = normalizedPositions;
        }

        if (previousItemCursor == itemDistributionCursor && previousFluidCursor == fluidDistributionCursor) {
            return;
        }

        driverCore.markForSave();
    }

    void applyRemovalPenalty() {
        removeResidueLevel(true);
    }

    CompoundTag writePersistent() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(COMPOUND_KEY_GENERATION_COOLDOWN, generationCooldown);
        tag.putInt(COMPOUND_KEY_CONSECUTIVE_FAILURE_TICKS, consecutiveFailureTicks);
        tag.putInt(COMPOUND_KEY_SUCCESS_COUNT, successCount);
        tag.putInt(COMPOUND_KEY_ITEM_DISTRIBUTION_CURSOR, itemDistributionCursor);
        tag.putInt(COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR, fluidDistributionCursor);
        tag.putLongArray(COMPOUND_KEY_VERIFIED_OUTLET_POSITIONS, verifiedOutletPositions.stream().mapToLong(BlockPos::asLong).sorted().toArray());
        return tag;
    }

    void readPersistent(CompoundTag compoundTag) {
        outletsPositions = List.of();
        verifiedOutletPositions = readOutletPositions(compoundTag);
        generationCooldown = readBoundedInt(compoundTag, COMPOUND_KEY_GENERATION_COOLDOWN, GENERATION_INTERVAL_TICKS, GENERATION_INTERVAL_TICKS);
        consecutiveFailureTicks = readBoundedInt(compoundTag, COMPOUND_KEY_CONSECUTIVE_FAILURE_TICKS, 0, FAILURE_PENALTY_TICKS);
        successCount = readBoundedInt(compoundTag, COMPOUND_KEY_SUCCESS_COUNT, 0, CONSECUTIVE_SUCCESSES_COUNT);
        itemDistributionCursor = Math.max(0, NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_ITEM_DISTRIBUTION_CURSOR, 0));
        fluidDistributionCursor = Math.max(0, NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR, 0));
    }

    private static int readBoundedInt(CompoundTag compoundTag, String key, int fallback, int max) {
        return Mth.clamp(NbtValues.getIntOrDefault(compoundTag, key, fallback), 0, max);
    }

    private static Set<BlockPos> readOutletPositions(CompoundTag compoundTag) {
        long[] storedPositions = compoundTag.getLongArray(COMPOUND_KEY_VERIFIED_OUTLET_POSITIONS);
        if (storedPositions.length == 0) {
            return Set.of();
        }

        Set<BlockPos> positions = new HashSet<>(storedPositions.length);
        for (long storedPosition : storedPositions) {
            positions.add(BlockPos.of(storedPosition));
        }
        return Set.copyOf(positions);
    }

    private void scanAndGenerateResidues(Level level) {
        int outletCount = outletsPositions.size();
        if (outletCount == 0) {
            removeResidueLevel(true);
            return;
        }

        AirtightAssemblyDriverFlowMeter flowMeter = driverCore.getFlowMeter();
        if (driverCore.getLevelCalculator().getSupplyLevel() <= 0 || flowMeter.getGasType().isEmpty()) {
            handleGenerationFailure();
            return;
        }

        ResidueOutput residueOutput = ResidueRecipeLookup.findOutput(level, flowMeter.getGasType());
        boolean fluidResidue = residueOutput.hasFluid();
        if (!fluidResidue && !residueOutput.hasItem()) {
            handleGenerationSuccess(false, false, -1, outletCount);
            return;
        }

        int generatedAmount = fluidResidue ? getTotalFluidGenerationAmount() : getTotalItemGenerationUnits();
        int requiredCapacity = Math.max(1, generatedAmount);
        boolean useRoundRobin = fluidResidue ? CCBConfig.server().machines.airtightAssemblyDriver.useFluidResidueRoundRobin.get() : CCBConfig.server().machines.airtightAssemblyDriver.useItemResidueRoundRobin.get();
        int distributionCursor = fluidResidue ? fluidDistributionCursor : itemDistributionCursor;
        int startIndex = useRoundRobin ? Mth.positiveModulo(distributionCursor, outletCount) : 0;
        GenerationPlan generationPlan = AirtightAssemblyDriverResiduePlanner.create(level, outletsPositions, residueOutput, requiredCapacity, startIndex);
        if (generationPlan == null) {
            handleGenerationFailure();
            return;
        }

        if (generatedAmount > 0 && !AirtightAssemblyDriverResiduePlanner.commit(generationPlan)) {
            handleGenerationFailure();
            return;
        }

        handleGenerationSuccess(useRoundRobin && generatedAmount > 0, fluidResidue, generationPlan.lastOutletIndex(), outletCount);
    }

    private void handleGenerationSuccess(boolean shouldAdvanceCursor, boolean isFluidOutput, int lastOutletIndex, int outletCount) {
        boolean stateChanged = shouldAdvanceCursor && advanceDistributionCursor(isFluidOutput, lastOutletIndex, outletCount);
        if (consecutiveFailureTicks != 0) {
            consecutiveFailureTicks = 0;
            stateChanged = true;
        }

        int residueLevel = driverCore.getLevelCalculator().getResidueLevel();
        if (residueLevel >= MAX_LEVEL) {
            if (successCount != 0) {
                successCount = 0;
                stateChanged = true;
            }
            if (stateChanged) {
                driverCore.markForSave();
            }
            return;
        }

        if (residueLevel == 0) {
            verifiedOutletPositions = Set.copyOf(outletsPositions);
            addResidueLevel(MAX_LEVEL / 2);
            return;
        }

        successCount++;
        if (successCount >= CONSECUTIVE_SUCCESSES_COUNT) {
            addResidueLevel(1);
            return;
        }

        driverCore.markForSave();
    }

    private void handleGenerationFailure() {
        boolean stateChanged = false;
        if (successCount != 0) {
            successCount = 0;
            stateChanged = true;
        }

        if (driverCore.getLevelCalculator().getResidueLevel() <= 0) {
            if (consecutiveFailureTicks != 0) {
                consecutiveFailureTicks = 0;
                stateChanged = true;
            }
            if (stateChanged) {
                driverCore.markForSave();
            }
            return;
        }

        int updatedFailureTicks = Math.min(FAILURE_PENALTY_TICKS, consecutiveFailureTicks + GENERATION_INTERVAL_TICKS);
        if (updatedFailureTicks != consecutiveFailureTicks) {
            consecutiveFailureTicks = updatedFailureTicks;
            stateChanged = true;
        }
        if (consecutiveFailureTicks == FAILURE_PENALTY_TICKS) {
            removeResidueLevel(false);
            return;
        }

        if (!stateChanged) {
            return;
        }

        driverCore.markForSave();
    }

    private void addResidueLevel(int levelIncrease) {
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();
        int updatedResidueLevel = Mth.clamp(levelCalculator.getResidueLevel() + levelIncrease, 0, MAX_LEVEL);
        levelCalculator.updateResidueLevel(updatedResidueLevel);
        resetProgress();
    }

    private void removeResidueLevel(boolean clearAll) {
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();
        int updatedResidueLevel = clearAll ? 0 : Mth.clamp(levelCalculator.getResidueLevel() - 1, 0, MAX_LEVEL);
        levelCalculator.updateResidueLevel(updatedResidueLevel);
        resetProgress();
    }

    private void resetProgress() {
        boolean hadProgress = successCount != 0 || generationCooldown != GENERATION_INTERVAL_TICKS || consecutiveFailureTicks != 0;
        successCount = 0;
        generationCooldown = GENERATION_INTERVAL_TICKS;
        consecutiveFailureTicks = 0;
        if (!hadProgress) {
            return;
        }

        driverCore.markForSave();
    }

    private int getTotalFluidGenerationAmount() {
        int currentLevel = driverCore.getLevelCalculator().getCurrentLevel();
        if (currentLevel == 0) {
            return 0;
        }

        return Math.max(0, (int) (currentLevel * BASE_FLUID_RESIDUE_PER_LEVEL * (double) CCBConfig.server().machines.airtightAssemblyDriver.fluidResidueQuantityMultiplier.getF()));
    }

    private int getTotalItemGenerationUnits() {
        int currentLevel = driverCore.getLevelCalculator().getCurrentLevel();
        if (currentLevel == 0) {
            return 0;
        }

        return (int) (currentLevel * BASE_ITEM_RESIDUE_COEFFICIENT * (double) CCBConfig.server().machines.airtightAssemblyDriver.itemResidueQuantityMultiplier.getF() * ResidueOutletInsertionTarget.ITEM_PROGRESS_UNITS_PER_ITEM / ITEM_GENERATION_DENOMINATOR);
    }

    private boolean advanceDistributionCursor(boolean isFluidOutput, int lastOutletIndex, int outletCount) {
        if (lastOutletIndex < 0 || outletCount <= 0) {
            return false;
        }

        int nextCursor = (lastOutletIndex + 1) % outletCount;
        if (isFluidOutput) {
            if (fluidDistributionCursor == nextCursor) {
                return false;
            }

            fluidDistributionCursor = nextCursor;
            return true;
        }

        if (itemDistributionCursor == nextCursor) {
            return false;
        }

        itemDistributionCursor = nextCursor;
        return true;
    }
}
