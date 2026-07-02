package net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver;

import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.InternalFluidHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletInventory;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletInventory.InternalStackHandler;
import net.ty.createcraftedbeginning.recipe.ResidueGenerationRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver.AirtightAssemblyDriverCore.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightAssemblyDriverResidueManager {
    private static final String COMPOUND_KEY_GENERATION_COOLDOWN = "GenerationCooldown";
    private static final String COMPOUND_KEY_FAILURE_COOLDOWN = "FailureCooldown";
    private static final String COMPOUND_KEY_SUCCESS_COUNT = "SuccessCount";
    private static final String COMPOUND_KEY_OUTLETS_POSITIONS = "OutletsPositions";
    private static final String COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR = "FluidDistributionCursor";

    private final AirtightAssemblyDriverCore driverCore;
    private final Set<BlockPos> outletsPositions = new HashSet<>();

    private int successCount;
    private int generationCooldown = getGenerationMaxCooldown();
    private int failureCooldown = getFailureMaxCooldown();
    private int fluidDistributionCursor;

    public AirtightAssemblyDriverResidueManager(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    private static int getFailureMaxCooldown() {
        return CCBConfig.server().airtights.residueFailureCooldown.get();
    }

    private static int getGenerationMaxCooldown() {
        return CCBConfig.server().airtights.residueGenerationCooldown.get();
    }

    private static int getConsecutiveSuccessesCount() {
        return CCBConfig.server().airtights.consecutiveSuccessesCount.get();
    }

    private static int getItemQuantityMultiplier() {
        return CCBConfig.server().airtights.itemQuantityMultiplier.get();
    }

    private static int getFluidQuantityMultiplier() {
        return CCBConfig.server().airtights.fluidQuantityMultiplier.get();
    }

    private static int getFluidGenerationAmountForOutlet(int totalFluidAmount, int outletCount, int outletIndex, int distributionCursor) {
        if (totalFluidAmount <= 0 || outletCount <= 0) {
            return 0;
        }

        int baseAmount = totalFluidAmount / outletCount;
        int remainingAmount = totalFluidAmount % outletCount;
        int relativeIndex = Math.floorMod(outletIndex - distributionCursor, outletCount);
        return baseAmount + (relativeIndex < remainingAmount ? 1 : 0);
    }

    private static boolean canReceiveItemResidue(ResidueOutletInventory inventory, ItemStack outputItemStack) {
        return outputItemStack.isEmpty() || getItemQuantityMultiplier() <= 0 || inventory.canAcceptItem(outputItemStack);
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
        generationCooldown = getGenerationMaxCooldown();
    }

    public void reset() {
        successCount = 0;
        generationCooldown = getGenerationMaxCooldown();
        failureCooldown = getFailureMaxCooldown();
        if (outletsPositions.isEmpty()) {
            fluidDistributionCursor = 0;
        }
        driverCore.getLevelCalculator().update(true);
    }

    public void updateOutletsPositions(Set<BlockPos> newPositions) {
        outletsPositions.clear();
        outletsPositions.addAll(newPositions);
        fluidDistributionCursor = outletsPositions.isEmpty() ? 0 : Math.floorMod(fluidDistributionCursor, outletsPositions.size());
    }

    public void applyRemovalPenalty(boolean clear) {
        removeResidueLevel(clear);
    }

    public CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt(COMPOUND_KEY_GENERATION_COOLDOWN, generationCooldown);
        compoundTag.putInt(COMPOUND_KEY_FAILURE_COOLDOWN, failureCooldown);
        compoundTag.putInt(COMPOUND_KEY_SUCCESS_COUNT, successCount);
        compoundTag.putLongArray(COMPOUND_KEY_OUTLETS_POSITIONS, outletsPositions.stream().mapToLong(BlockPos::asLong).toArray());
        compoundTag.putInt(COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR, fluidDistributionCursor);
        return compoundTag;
    }

    public void read(CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_GENERATION_COOLDOWN)) {
            generationCooldown = compoundTag.getInt(COMPOUND_KEY_GENERATION_COOLDOWN);
        }
        if (compoundTag.contains(COMPOUND_KEY_FAILURE_COOLDOWN)) {
            failureCooldown = compoundTag.getInt(COMPOUND_KEY_FAILURE_COOLDOWN);
        }
        if (compoundTag.contains(COMPOUND_KEY_SUCCESS_COUNT)) {
            successCount = compoundTag.getInt(COMPOUND_KEY_SUCCESS_COUNT);
        }
        if (compoundTag.contains(COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR)) {
            fluidDistributionCursor = compoundTag.getInt(COMPOUND_KEY_FLUID_DISTRIBUTION_CURSOR);
        }
        if (!compoundTag.contains(COMPOUND_KEY_OUTLETS_POSITIONS)) {
            return;
        }

        outletsPositions.clear();
        Arrays.stream(compoundTag.getLongArray(COMPOUND_KEY_OUTLETS_POSITIONS)).mapToObj(BlockPos::of).forEach(outletsPositions::add);
        fluidDistributionCursor = outletsPositions.isEmpty() ? 0 : Math.floorMod(fluidDistributionCursor, outletsPositions.size());
    }

    private void scanAndGenerateResidues(Level level) {
        List<BlockPos> sortedOutletsPositions = getSortedOutletPositions();
        int attachedOutlets = sortedOutletsPositions.size();
        if (attachedOutlets == 0) {
            removeResidueLevel(true);
            return;
        }

        int totalFluidAmount = getTotalFluidGenerationAmount();
        int distributionCursor = Math.floorMod(fluidDistributionCursor, attachedOutlets);
        boolean allResidueSuccess = true;
        for (int i = 0; i < attachedOutlets; i++) {
            BlockPos outletPos = sortedOutletsPositions.get(i);
            int fluidAmount = getFluidGenerationAmountForOutlet(totalFluidAmount, attachedOutlets, i, distributionCursor);
            if (!tryGenerateResidue(outletPos, level, fluidAmount)) {
                allResidueSuccess = false;
            }
        }

        if (allResidueSuccess) {
            advanceFluidDistributionCursor(totalFluidAmount, attachedOutlets);
            successCount++;
            driverCore.markDirty();
            int residueLevel = driverCore.getLevelCalculator().getResidueLevel();
            if (residueLevel == 0) {
                addResidueLevel(MAX_LEVEL / 2);
                return;
            }
            if (successCount >= getConsecutiveSuccessesCount()) {
                addResidueLevel(1);
            }
            return;
        }

        if (failureCooldown != 0) {
            return;
        }

        removeResidueLevel(false);
    }

    private List<BlockPos> getSortedOutletPositions() {
        List<BlockPos> sortedOutletsPositions = new ArrayList<>(outletsPositions);
        sortedOutletsPositions.sort(Comparator.comparingLong(BlockPos::asLong));
        return sortedOutletsPositions;
    }

    private void addResidueLevel(int level) {
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();
        levelCalculator.updateResidueLevel(Mth.clamp(levelCalculator.getResidueLevel() + level, 0, MAX_LEVEL));
        reset();
    }

    private void removeResidueLevel(boolean clear) {
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();
        levelCalculator.updateResidueLevel(clear ? 0 : Mth.clamp(levelCalculator.getResidueLevel() - 1, 0, MAX_LEVEL));
        reset();
    }

    private boolean tryGenerateResidue(BlockPos blockPos, Level level, int fluidAmount) {
        if (!(level.getBlockEntity(blockPos) instanceof ResidueOutletBlockEntity outlet)) {
            return false;
        }
        if (!(outlet.getFluidTankBehaviour().getCapability() instanceof InternalFluidHandler fluidHandler)) {
            return false;
        }
        if (!(outlet.getInventory().getCapability() instanceof InternalStackHandler)) {
            return false;
        }

        AirtightAssemblyDriverFlowMeter flowMeter = driverCore.getFlowMeter();
        if (flowMeter.getGasSupply() == 0 || flowMeter.getGasType().isEmpty()) {
            return false;
        }

        Gas gasType = flowMeter.getGasType().getGasType();
        FluidStack outputFluidStack = ResidueGenerationRecipe.getRequiredFluid(level, gasType);
        ItemStack outputItemStack = ResidueGenerationRecipe.getRequiredItem(level, gasType);
        ResidueOutletInventory inventory = outlet.getInventory();
        if (!canReceiveResidueTargets(fluidHandler, inventory, outputFluidStack, outputItemStack)) {
            return false;
        }

        int currentLevel = driverCore.getLevelCalculator().getCurrentLevel();
        if (currentLevel == 0) {
            return true;
        }

        float itemCount = getItemGenerationCount();
        if (!canGenerateFluidResidue(fluidHandler, outputFluidStack, fluidAmount) || !inventory.canAddPartialItemCount(itemCount, outputItemStack)) {
            return false;
        }

        boolean fluidSuccess = generateFluidResidue(fluidHandler, outputFluidStack, fluidAmount);
        boolean itemSuccess = inventory.addPartialItemCount(itemCount, outputItemStack);
        return fluidSuccess && itemSuccess;
    }

    private boolean canReceiveResidueTargets(InternalFluidHandler fluidHandler, ResidueOutletInventory inventory, FluidStack outputFluidStack, ItemStack outputItemStack) {
        return canReceiveFluidResidue(fluidHandler, outputFluidStack) && canReceiveItemResidue(inventory, outputItemStack);
    }

    private boolean canReceiveFluidResidue(InternalFluidHandler fluidHandler, FluidStack outputFluidStack) {
        return outputFluidStack.isEmpty() || getFluidQuantityMultiplier() <= 0 || fluidHandler.forceFill(outputFluidStack.copyWithAmount(1), FluidAction.SIMULATE) >= 1;
    }

    private boolean canGenerateFluidResidue(InternalFluidHandler fluidHandler, FluidStack outputFluidStack, int fluidAmount) {
        return outputFluidStack.isEmpty() || fluidAmount <= 0 || fluidHandler.forceFill(outputFluidStack.copyWithAmount(fluidAmount), FluidAction.SIMULATE) >= fluidAmount;
    }

    private boolean generateFluidResidue(InternalFluidHandler fluidHandler, FluidStack outputFluidStack, int fluidAmount) {
        if (outputFluidStack.isEmpty() || fluidAmount <= 0) {
            return true;
        }

        int filledAmount = fluidHandler.forceFill(outputFluidStack.copyWithAmount(fluidAmount), FluidAction.EXECUTE);
        return filledAmount >= fluidAmount;
    }

    private int getTotalFluidGenerationAmount() {
        int currentLevel = driverCore.getLevelCalculator().getCurrentLevel();
        return currentLevel == 0 ? 0 : Math.max(0, currentLevel * getFluidQuantityMultiplier());
    }

    private void advanceFluidDistributionCursor(int totalFluidAmount, int outletCount) {
        if (totalFluidAmount <= 0 || outletCount <= 0) {
            return;
        }

        fluidDistributionCursor = Math.floorMod(fluidDistributionCursor + totalFluidAmount, outletCount);
    }

    private float getItemGenerationCount() {
        return getResidueGenerationRate() / MAX_LEVEL / MAX_LEVEL * getItemQuantityMultiplier();
    }

    private float getResidueGenerationRate() {
        int currentLevel = driverCore.getLevelCalculator().getCurrentLevel();
        int count = outletsPositions.size();
        return currentLevel == 0 || count == 0 ? 0 : (float) currentLevel / count;
    }
}