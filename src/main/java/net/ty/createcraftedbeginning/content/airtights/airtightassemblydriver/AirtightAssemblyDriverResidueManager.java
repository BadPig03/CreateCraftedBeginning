package net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver;

import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.InternalFluidHandler;
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
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletInventory.InternalStackHandler;
import net.ty.createcraftedbeginning.recipe.ResidueGenerationRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver.AirtightAssemblyDriverCore.MAX_LEVEL;

public class AirtightAssemblyDriverResidueManager {
    private static final String COMPOUND_KEY_GENERATION_COOLDOWN = "GenerationCooldown";
    private static final String COMPOUND_KEY_FAILURE_COOLDOWN = "FailureCooldown";
    private static final String COMPOUND_KEY_SUCCESS_COUNT = "SuccessCount";
    private static final String COMPOUND_KEY_OUTLETS_POSITIONS = "OutletsPositions";

    private final AirtightAssemblyDriverCore driverCore;
    private final Set<BlockPos> outletsPositions = new HashSet<>();

    private int successCount;
    private int generationCooldown = getGenerationMaxCooldown();
    private int failureCooldown = getFailureMaxCooldown();

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
        driverCore.getLevelCalculator().update();
    }

    public void updateOutletsPositions(Set<BlockPos> newPositions) {
        outletsPositions.clear();
        outletsPositions.addAll(newPositions);
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
        return compoundTag;
    }

    public void read(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_GENERATION_COOLDOWN)) {
            generationCooldown = compoundTag.getInt(COMPOUND_KEY_GENERATION_COOLDOWN);
        }
        if (compoundTag.contains(COMPOUND_KEY_FAILURE_COOLDOWN)) {
            failureCooldown = compoundTag.getInt(COMPOUND_KEY_FAILURE_COOLDOWN);
        }
        if (compoundTag.contains(COMPOUND_KEY_SUCCESS_COUNT)) {
            successCount = compoundTag.getInt(COMPOUND_KEY_SUCCESS_COUNT);
        }
        if (!compoundTag.contains(COMPOUND_KEY_OUTLETS_POSITIONS)) {
            return;
        }

        outletsPositions.clear();
        Arrays.stream(compoundTag.getLongArray(COMPOUND_KEY_OUTLETS_POSITIONS)).mapToObj(BlockPos::of).forEach(outletsPositions::add);
    }

    private void scanAndGenerateResidues(@NotNull Level level) {
        int attachedOutlets = outletsPositions.size();
        if (attachedOutlets == 0) {
            removeResidueLevel(true);
            return;
        }

        boolean allResidueSuccess = true;
        for (BlockPos outletPos : outletsPositions) {
            if (!tryGenerateResidue(outletPos, level)) {
                allResidueSuccess = false;
            }
        }

        if (allResidueSuccess) {
            successCount++;
            int residueLevel = driverCore.getLevelCalculator().getResidueLevel();
            if (residueLevel == 0) {
                addResidueLevel();
                return;
            }
            if (successCount >= getConsecutiveSuccessesCount()) {
                addResidueLevel();
            }
            return;
        }

        if (failureCooldown != 0) {
            return;
        }

        removeResidueLevel(false);
    }

    private void addResidueLevel() {
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();
        levelCalculator.updateResidueLevel(Mth.clamp(levelCalculator.getResidueLevel() + 1, 0, MAX_LEVEL));
        reset();
    }

    private void removeResidueLevel(boolean clear) {
        AirtightAssemblyDriverLevelCalculator levelCalculator = driverCore.getLevelCalculator();
        levelCalculator.updateResidueLevel(clear ? 0 : Mth.clamp(levelCalculator.getResidueLevel() - 1, 0, MAX_LEVEL));
        reset();
    }

    private boolean tryGenerateResidue(BlockPos blockPos, @NotNull Level level) {
        if (!(level.getBlockEntity(blockPos) instanceof ResidueOutletBlockEntity outlet)) {
            return false;
        }
        if (!(outlet.getFluidTankBehaviour().getCapability() instanceof InternalFluidHandler fluidHandler)) {
            return false;
        }
        if (!(outlet.getInventory().getCapability() instanceof InternalStackHandler itemStackHandler)) {
            return false;
        }

        AirtightAssemblyDriverFlowMeter flowMeter = driverCore.getFlowMeter();
        float gasSupply = flowMeter.getGasSupply();
        if (gasSupply == 0) {
            return false;
        }

        int currentLevel = driverCore.getLevelCalculator().getCurrentLevel();
        if (currentLevel == 0) {
            return fluidHandler.getFluidInTank(0).isEmpty() && itemStackHandler.getStackInSlot(0).isEmpty();
        }

        Gas gasType = flowMeter.getGasType().getGasType();
        FluidStack outputFluidStack = ResidueGenerationRecipe.getRequiredFluid(level, gasType);
        boolean fluidSuccess;
        if (outputFluidStack.isEmpty()) {
            fluidSuccess = true;
        }
        else {
            int fillAmount = Mth.ceil(getResidueGenerationRate() * getFluidQuantityMultiplier());
            int filledAmount = fluidHandler.forceFill(outputFluidStack.copyWithAmount(fillAmount), FluidAction.EXECUTE);
            fluidSuccess = filledAmount >= fillAmount;
        }

        ItemStack outputItemStack = ResidueGenerationRecipe.getRequiredItem(level, gasType);
        boolean itemSuccess;
        if (outputItemStack.isEmpty()) {
            itemSuccess = true;
        }
        else {
            float itemCount = getResidueGenerationRate() / MAX_LEVEL / MAX_LEVEL * getItemQuantityMultiplier();
            itemSuccess = outlet.getInventory().addPartialItemCount(itemCount, outputItemStack);
        }
        return fluidSuccess && itemSuccess;
    }

    private float getResidueGenerationRate() {
        int currentLevel = driverCore.getLevelCalculator().getCurrentLevel();
        int count = outletsPositions.size();
        return currentLevel == 0 || count == 0 ? 0 : (float) currentLevel / count;
    }
}
