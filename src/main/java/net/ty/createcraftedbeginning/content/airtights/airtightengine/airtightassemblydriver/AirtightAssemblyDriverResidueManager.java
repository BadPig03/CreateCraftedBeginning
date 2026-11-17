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

    private static final String COMPOUND_KEY_GENERATION_COOLDOWN = "GenerationCooldown";
    private static final String COMPOUND_KEY_ITEM_DISTRIBUTION_CURSOR = "ItemDistributionCursor";
    private static final String COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR = "FluidDistributionCursor";

    private final AirtightAssemblyDriverCore driverCore;
    private List<BlockPos> outletsPositions = List.of();

    private int generationCooldown = GENERATION_INTERVAL_TICKS;
    private int itemDistributionCursor;
    private int fluidDistributionCursor;

    AirtightAssemblyDriverResidueManager(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    private static int getTotalFluidGenerationAmount(int currentLevel) {
        if (currentLevel == 0) {
            return 0;
        }

        return Math.max(0, (int) (currentLevel * BASE_FLUID_RESIDUE_PER_LEVEL * (double) CCBConfig.server().machines.airtightAssemblyDriver.fluidResidueQuantityMultiplier.getF()));
    }

    private static int getTotalItemGenerationUnits(int currentLevel) {
        if (currentLevel == 0) {
            return 0;
        }

        return (int) (currentLevel * BASE_ITEM_RESIDUE_COEFFICIENT * (double) CCBConfig.server().machines.airtightAssemblyDriver.itemResidueQuantityMultiplier.getF() * ResidueOutletInsertionTarget.ITEM_PROGRESS_UNITS_PER_ITEM / ITEM_GENERATION_DENOMINATOR);
    }

    void tick(Level level) {
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
        boolean hadResidueState = generationCooldown != GENERATION_INTERVAL_TICKS || itemDistributionCursor != 0 || fluidDistributionCursor != 0 || !outletsPositions.isEmpty();
        outletsPositions = List.of();
        generationCooldown = GENERATION_INTERVAL_TICKS;
        itemDistributionCursor = 0;
        fluidDistributionCursor = 0;
        if (!hadResidueState) {
            return;
        }

        driverCore.markForSave();
    }

    void loadEmptyPersistentState() {
        outletsPositions = List.of();
        generationCooldown = GENERATION_INTERVAL_TICKS;
        itemDistributionCursor = 0;
        fluidDistributionCursor = 0;
    }

    void clearOutletsPositions() {
        outletsPositions = List.of();
    }

    void updateOutletsPositions(Set<BlockPos> newPositions) {
        List<BlockPos> sortedPositions = new ArrayList<>(newPositions);
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

        if (previousItemCursor == itemDistributionCursor && previousFluidCursor == fluidDistributionCursor) {
            return;
        }

        driverCore.markForSave();
    }

    CompoundTag writePersistent() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(COMPOUND_KEY_GENERATION_COOLDOWN, generationCooldown);
        tag.putInt(COMPOUND_KEY_ITEM_DISTRIBUTION_CURSOR, itemDistributionCursor);
        tag.putInt(COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR, fluidDistributionCursor);
        return tag;
    }

    void readPersistent(CompoundTag compoundTag) {
        outletsPositions = List.of();
        generationCooldown = Mth.clamp(NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_GENERATION_COOLDOWN, GENERATION_INTERVAL_TICKS), 0, GENERATION_INTERVAL_TICKS);
        itemDistributionCursor = Math.max(0, NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_ITEM_DISTRIBUTION_CURSOR, 0));
        fluidDistributionCursor = Math.max(0, NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR, 0));
    }

    private void scanAndGenerateResidues(Level level) {
        int outletCount = outletsPositions.size();
        if (outletCount == 0) {
            return;
        }

        AirtightAssemblyDriverFlowMeter flowMeter = driverCore.getFlowMeter();
        int generationLevel = driverCore.getLevelCalculator().getCurrentLevel();
        if (generationLevel <= 0 || flowMeter.getGasType().isEmpty()) {
            return;
        }

        ResidueOutput residueOutput = ResidueRecipeLookup.findOutput(level, flowMeter.getGasType());
        boolean fluidResidue = residueOutput.hasFluid();
        if (!fluidResidue && !residueOutput.hasItem()) {
            return;
        }

        int requiredCapacity = fluidResidue ? getTotalFluidGenerationAmount(generationLevel) : getTotalItemGenerationUnits(generationLevel);
        boolean useRoundRobin = fluidResidue ? CCBConfig.server().machines.airtightAssemblyDriver.useFluidResidueRoundRobin.get() : CCBConfig.server().machines.airtightAssemblyDriver.useItemResidueRoundRobin.get();
        int distributionCursor = fluidResidue ? fluidDistributionCursor : itemDistributionCursor;
        int startIndex = useRoundRobin ? Mth.positiveModulo(distributionCursor, outletCount) : 0;
        GenerationPlan generationPlan = AirtightAssemblyDriverResiduePlanner.create(level, outletsPositions, residueOutput, requiredCapacity, startIndex);
        if (generationPlan == null) {
            return;
        }

        if (!AirtightAssemblyDriverResiduePlanner.commit(generationPlan)) {
            return;
        }

        if (!useRoundRobin || !advanceDistributionCursor(fluidResidue, generationPlan.lastOutletIndex(), outletCount)) {
            return;
        }

        driverCore.markForSave();
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
