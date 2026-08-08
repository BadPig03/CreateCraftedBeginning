package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlockEntity.ResidueInsertionPlan;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletInventory;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.recipe.ResidueGenerationRecipe;
import net.ty.createcraftedbeginning.recipe.ResidueGenerationRecipe.ResidueOutput;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightAssemblyDriverResidueManager {
    private static final int ITEM_GENERATION_DENOMINATOR = MAX_LEVEL * MAX_LEVEL;
    private static final int GENERATION_MAX_COOLDOWN = 12;
    private static final int FAILURE_MAX_COOLDOWN = 120;
    private static final int CONSECUTIVE_SUCCESSES_COUNT = 20;

    private static final String COMPOUND_KEY_GENERATION_COOLDOWN = "GenerationCooldown";
    private static final String COMPOUND_KEY_FAILURE_COOLDOWN = "FailureCooldown";
    private static final String COMPOUND_KEY_SUCCESS_COUNT = "SuccessCount";
    private static final String COMPOUND_KEY_ITEM_DISTRIBUTION_CURSOR = "ItemDistributionCursor";
    private static final String COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR = "FluidDistributionCursor";

    private final AirtightAssemblyDriverCore driverCore;
    private List<BlockPos> outletsPositions = List.of();

    private int successCount;
    private int generationCooldown = GENERATION_MAX_COOLDOWN;
    private int failureCooldown = FAILURE_MAX_COOLDOWN;
    private int itemDistributionCursor;
    private int fluidDistributionCursor;

    public AirtightAssemblyDriverResidueManager(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    private static int getItemQuantityMultiplier() {
        return CCBConfig.server().airtights.itemQuantityMultiplier.get();
    }

    private static int getFluidQuantityMultiplier() {
        return CCBConfig.server().airtights.fluidQuantityMultiplier.get();
    }

    private static boolean useItemResidueRoundRobin() {
        return CCBConfig.server().airtights.useItemResidueRoundRobin.get();
    }

    private static boolean useFluidResidueRoundRobin() {
        return CCBConfig.server().airtights.useFluidResidueRoundRobin.get();
    }

    private static int readBoundedInt(CompoundTag compoundTag, String key, int fallback, int max) {
        return compoundTag.contains(key) ? Mth.clamp(compoundTag.getInt(key), 0, max) : fallback;
    }

    private static @Nullable ResidueInsertionPlan createOutletInsertionPlan(BlockPos pos, Level level, ResidueOutput output, int maxAmount) {
        if (!(level.getBlockEntity(pos) instanceof ResidueOutletBlockEntity outlet)) {
            return null;
        }
        return outlet.createResidueInsertionPlan(output.fluidStack(), output.itemStack(), maxAmount);
    }

    private static boolean commitGenerationPlan(GenerationPlan plan) {
        ResourceTransaction transaction = new ResourceTransaction();
        for (ResidueInsertionPlan insertion : plan.insertions()) {
            insertion.addTo(transaction);
        }
        return transaction.commit();
    }

    public void tick(Level level) {
        if (failureCooldown > 0) {
            failureCooldown--;
        }
        if (outletsPositions.isEmpty() && driverCore.getLevelCalculator().getResidueLevel() > 0) {
            removeResidueLevel(true);
            return;
        }

        if (generationCooldown > 0) {
            generationCooldown--;
            return;
        }

        scanAndGenerateResidues(level);
        generationCooldown = GENERATION_MAX_COOLDOWN;
    }

    public void reset() {
        boolean changed = successCount != 0 || generationCooldown != GENERATION_MAX_COOLDOWN || failureCooldown != FAILURE_MAX_COOLDOWN || itemDistributionCursor != 0 || fluidDistributionCursor != 0 || !outletsPositions.isEmpty();
        outletsPositions = List.of();
        successCount = 0;
        generationCooldown = GENERATION_MAX_COOLDOWN;
        failureCooldown = FAILURE_MAX_COOLDOWN;
        itemDistributionCursor = 0;
        fluidDistributionCursor = 0;
        if (!changed) {
            return;
        }

        driverCore.markForSave();
    }

    public void loadEmptyPersistentState() {
        outletsPositions = List.of();
        successCount = 0;
        generationCooldown = GENERATION_MAX_COOLDOWN;
        failureCooldown = FAILURE_MAX_COOLDOWN;
        itemDistributionCursor = 0;
        fluidDistributionCursor = 0;
    }

    public void clearOutletsPositions() {
        outletsPositions = List.of();
    }

    public void updateOutletsPositions(Set<BlockPos> newPositions) {
        List<BlockPos> sorted = new ArrayList<>(newPositions);
        sorted.sort(Comparator.comparingLong(BlockPos::asLong));
        outletsPositions = List.copyOf(sorted);
        int previousItemCursor = itemDistributionCursor;
        int previousFluidCursor = fluidDistributionCursor;
        if (outletsPositions.isEmpty()) {
            itemDistributionCursor = 0;
            fluidDistributionCursor = 0;
        }
        else {
            itemDistributionCursor = Math.floorMod(itemDistributionCursor, outletsPositions.size());
            fluidDistributionCursor = Math.floorMod(fluidDistributionCursor, outletsPositions.size());
        }
        if (previousItemCursor == itemDistributionCursor && previousFluidCursor == fluidDistributionCursor) {
            return;
        }

        driverCore.markForSave();
    }

    public void applyRemovalPenalty(boolean clear) {
        removeResidueLevel(clear);
    }

    public CompoundTag writePersistent() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(COMPOUND_KEY_GENERATION_COOLDOWN, generationCooldown);
        tag.putInt(COMPOUND_KEY_FAILURE_COOLDOWN, failureCooldown);
        tag.putInt(COMPOUND_KEY_SUCCESS_COUNT, successCount);
        tag.putInt(COMPOUND_KEY_ITEM_DISTRIBUTION_CURSOR, itemDistributionCursor);
        tag.putInt(COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR, fluidDistributionCursor);
        return tag;
    }

    public void readPersistent(CompoundTag compoundTag) {
        outletsPositions = List.of();
        generationCooldown = readBoundedInt(compoundTag, COMPOUND_KEY_GENERATION_COOLDOWN, GENERATION_MAX_COOLDOWN, GENERATION_MAX_COOLDOWN);
        failureCooldown = readBoundedInt(compoundTag, COMPOUND_KEY_FAILURE_COOLDOWN, FAILURE_MAX_COOLDOWN, FAILURE_MAX_COOLDOWN);
        successCount = readBoundedInt(compoundTag, COMPOUND_KEY_SUCCESS_COUNT, 0, CONSECUTIVE_SUCCESSES_COUNT);
        itemDistributionCursor = compoundTag.contains(COMPOUND_KEY_ITEM_DISTRIBUTION_CURSOR) ? Math.max(0, compoundTag.getInt(COMPOUND_KEY_ITEM_DISTRIBUTION_CURSOR)) : 0;
        fluidDistributionCursor = compoundTag.contains(COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR) ? Math.max(0, compoundTag.getInt(COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR)) : 0;
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

        ResidueOutput output = ResidueGenerationRecipe.findOutput(level, flowMeter.getGasType());
        if (!output.hasFluid() && !output.hasItem()) {
            handleGenerationSuccess(false, false, -1, outletCount);
            return;
        }

        int generatedAmount = output.hasFluid() ? getTotalFluidGenerationAmount() : getTotalItemGenerationUnits();
        int requiredCapacity = Math.max(1, generatedAmount);
        boolean roundRobin = output.hasFluid() ? useFluidResidueRoundRobin() : useItemResidueRoundRobin();
        GenerationPlan plan = createGenerationPlan(level, output, requiredCapacity, roundRobin);
        if (plan == null) {
            handleGenerationFailure();
            return;
        }

        if (generatedAmount > 0 && !commitGenerationPlan(plan)) {
            handleGenerationFailure();
            return;
        }

        handleGenerationSuccess(roundRobin && generatedAmount > 0, output.hasFluid(), plan.lastOutletIndex(), outletCount);
    }

    private @Nullable GenerationPlan createGenerationPlan(Level level, ResidueOutput output, int requiredAmount, boolean roundRobin) {
        int outletCount = outletsPositions.size();
        int distributionCursor = output.hasFluid() ? fluidDistributionCursor : itemDistributionCursor;
        int startIndex = roundRobin ? Math.floorMod(distributionCursor, outletCount) : 0;
        int remainingAmount = requiredAmount;
        int lastOutletIndex = -1;
        List<ResidueInsertionPlan> insertions = new ArrayList<>();
        for (int offset = 0; offset < outletCount && remainingAmount > 0; offset++) {
            int outletIndex = (startIndex + offset) % outletCount;
            ResidueInsertionPlan insertion = createOutletInsertionPlan(outletsPositions.get(outletIndex), level, output, remainingAmount);
            if (insertion == null) {
                continue;
            }

            insertions.add(insertion);
            remainingAmount -= insertion.plannedAmount();
            lastOutletIndex = outletIndex;
        }

        return remainingAmount == 0 ? new GenerationPlan(List.copyOf(insertions), lastOutletIndex) : null;
    }

    private void handleGenerationSuccess(boolean shouldAdvanceCursor, boolean fluidOutput, int lastOutletIndex, int outletCount) {
        boolean changed = shouldAdvanceCursor && advanceDistributionCursor(fluidOutput, lastOutletIndex, outletCount);
        int residueLevel = driverCore.getLevelCalculator().getResidueLevel();
        if (residueLevel >= MAX_LEVEL) {
            if (successCount != 0) {
                successCount = 0;
                changed = true;
            }
            if (changed) {
                driverCore.markForSave();
            }
            return;
        }

        if (residueLevel == 0) {
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
        if (successCount != 0) {
            successCount = 0;
            driverCore.markForSave();
        }
        if (failureCooldown != 0) {
            return;
        }

        removeResidueLevel(false);
    }

    private void addResidueLevel(int level) {
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();
        int residueLevel = Mth.clamp(levelCalculator.getResidueLevel() + level, 0, MAX_LEVEL);
        levelCalculator.updateResidueLevel(residueLevel);
        resetProgress();
    }

    private void removeResidueLevel(boolean clear) {
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();
        int residueLevel = clear ? 0 : Mth.clamp(levelCalculator.getResidueLevel() - 1, 0, MAX_LEVEL);
        levelCalculator.updateResidueLevel(residueLevel);
        resetProgress();
    }

    private void resetProgress() {
        boolean changed = successCount != 0 || generationCooldown != GENERATION_MAX_COOLDOWN || failureCooldown != FAILURE_MAX_COOLDOWN;
        successCount = 0;
        generationCooldown = GENERATION_MAX_COOLDOWN;
        failureCooldown = FAILURE_MAX_COOLDOWN;
        if (!changed) {
            return;
        }

        driverCore.markForSave();
    }

    private int getTotalFluidGenerationAmount() {
        int currentLevel = driverCore.getLevelCalculator().getCurrentLevel();
        return currentLevel == 0 ? 0 : Math.max(0, currentLevel * getFluidQuantityMultiplier());
    }

    private int getTotalItemGenerationUnits() {
        int currentLevel = driverCore.getLevelCalculator().getCurrentLevel();
        if (currentLevel == 0) {
            return 0;
        }
        return currentLevel * getItemQuantityMultiplier() * ResidueOutletInventory.ITEM_PROGRESS_UNITS_PER_ITEM / ITEM_GENERATION_DENOMINATOR;
    }

    private boolean advanceDistributionCursor(boolean fluidOutput, int lastOutletIndex, int outletCount) {
        if (lastOutletIndex < 0 || outletCount <= 0) {
            return false;
        }

        int newCursor = (lastOutletIndex + 1) % outletCount;
        if (fluidOutput) {
            if (fluidDistributionCursor == newCursor) {
                return false;
            }

            fluidDistributionCursor = newCursor;
            return true;
        }

        if (itemDistributionCursor == newCursor) {
            return false;
        }

        itemDistributionCursor = newCursor;
        return true;
    }

    private record GenerationPlan(List<ResidueInsertionPlan> insertions, int lastOutletIndex) {}
}
